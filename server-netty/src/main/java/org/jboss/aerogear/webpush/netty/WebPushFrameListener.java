/**
 * JBoss, Home of Professional Open Source
 * Copyright Red Hat, Inc., and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.aerogear.webpush.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.AsciiString;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.EmptyHttp2Headers;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.Http2FrameAdapter;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2Stream;
import io.netty.util.CharsetUtil;
import org.jboss.aerogear.webpush.Channel;
import org.jboss.aerogear.webpush.Registration;
import org.jboss.aerogear.webpush.Registration.WebLink;
import org.jboss.aerogear.webpush.WebPushServer;
import org.jboss.aerogear.webpush.datastore.RegistrationNotFoundException;
import org.jboss.aerogear.webpush.util.ArgumentUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static io.netty.handler.codec.http.HttpHeaderNames.CACHE_CONTROL;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderNames.LOCATION;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;

public class WebPushFrameListener extends Http2FrameAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebPushNettyServer.class);
    private static final ConcurrentHashMap<String, Optional<Client>> monitoredStreams = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> notificationStreams = new ConcurrentHashMap<>();
    public static final AsciiString LINK = new AsciiString("link");
    public static final AsciiString ACCESS_CONTROL_ALLOW_ORIGIN = new AsciiString("access-control-allow-origin");
    public static final AsciiString ANY_ORIGIN = new AsciiString("*");
    public static final AsciiString ACCESS_CONTROL_EXPOSE_HEADERS = new AsciiString("access-control-expose-headers");

    private static final String PATH_KEY = "webpush.path";
    private static final String METHOD_KEY = "webpush.method";
    private final WebPushServer webpushServer;
    private Http2ConnectionEncoder encoder;

    public WebPushFrameListener(final WebPushServer webpushServer) {
        ArgumentUtil.checkNotNull(webpushServer, "webpushServer");
        this.webpushServer = webpushServer;
    }

    public void encoder(Http2ConnectionEncoder encoder) {
        this.encoder = encoder;
    }

    @Override
    public void onHeadersRead(final ChannelHandlerContext ctx,
                              final int streamId,
                              final Http2Headers headers,
                              final int streamDependency,
                              final short weight,
                              final boolean exclusive,
                              final int padding,
                              final boolean endStream) throws Http2Exception {
        final String path = headers.path().toString();
        encoder.connection().stream(streamId).setProperty(PATH_KEY, path);
        encoder.connection().stream(streamId).setProperty(METHOD_KEY, headers.method());
        LOGGER.info("onHeadersRead. streamId={}, method={}, path={}, endstream={}", streamId, headers.method(), path, endStream);

        switch (headers.method().toString()) {
            case "GET":
                if (path.contains("monitor")) {
                    handleMonitor(path, ctx, streamId);
                }
                break;
            case "DELETE":
                handleChannelRemoval(path, ctx, streamId);
                break;
            case "PUT":
                break;
        }
    }

    @Override
    public int onDataRead(final ChannelHandlerContext ctx,
                          final int streamId,
                          final ByteBuf data,
                          final int padding,
                          final boolean endOfStream) throws Http2Exception {
        final Http2Stream stream = encoder.connection().stream(streamId);
        final String path = stream.getProperty(PATH_KEY);
        LOGGER.info("onDataRead. streamId={}, path={}, endstream={}", streamId, path, endOfStream);
        if (path.contains("/register")) {
            handleDeviceRegistration(ctx, streamId);
        } else if (path.contains("channel")) {
            handleChannelCreation(path, ctx, streamId);
        } else {
            handleNotification(ctx, data, padding, path);
        }
        return super.onDataRead(ctx, streamId, data, padding, endOfStream);
    }

    private void handleNotification(final ChannelHandlerContext ctx,
                                    final ByteBuf data,
                                    final int padding,
                                    final String path) {
        final String endpointToken = extractEndpointToken(path);
        final String registrationId = notificationStreams.get(endpointToken);
        final Optional<Client> optionalClient = monitoredStreams.get(registrationId);
        if (optionalClient.isPresent()) {
            LOGGER.info("Handle notification {} payload {}", registrationId, data.toString(CharsetUtil.UTF_8));
            final Client client = optionalClient.get();
            LOGGER.info("Handle notification {}", client.ctx);
            encoder.writeHeaders(client.ctx, client.streamId, EmptyHttp2Headers.INSTANCE, 0, false, ctx.newPromise());
            encoder.writeData(client.ctx, client.streamId, data.retain(), padding, false, ctx.newPromise());
        } else {
            LOGGER.info("Could not find a monitor for registrationId: {}", registrationId);
        }

    }

    private class Client {

        private final ChannelHandlerContext ctx;
        private final int streamId;

        Client(final ChannelHandlerContext ctx, final int streamId) {
            this.ctx = ctx;
            this.streamId = streamId;
        }

    }

    private void handleDeviceRegistration(final ChannelHandlerContext ctx, final int streamId) {
        final Registration registration = webpushServer.register();
        LOGGER.info("Registered {} " + registration);
        final Http2Headers responseHeaders = new DefaultHttp2Headers(false)
                .status(OK.codeAsText())
                .set(LOCATION, new AsciiString(registration.monitorURI().toString()))
                .set(ACCESS_CONTROL_ALLOW_ORIGIN, ANY_ORIGIN)
                .set(ACCESS_CONTROL_EXPOSE_HEADERS, new AsciiString("Link, Cache-Control, Location"))
                .set(LINK, asLink(registration.monitorURI(), WebLink.MONITOR.toString()),
                        asLink(registration.channelURI(), WebLink.CHANNEL.toString()))
                .set(CACHE_CONTROL, new AsciiString("private, max-age=" + webpushServer.config().registrationMaxAge()));

        LOGGER.debug(responseHeaders.getAll(LINK).toString());
        encoder.writeHeaders(ctx, streamId, responseHeaders, 0, true, ctx.newPromise());
    }

    private static AsciiString asLink(final URI contextUri, final String relationType) {
        return new AsciiString("<" + contextUri + ">;rel=\"" +  relationType + "\"");
    }

    private void handleChannelCreation(final String path, final ChannelHandlerContext ctx, final int streamId) {
        try {
            final String registrationId = extractRegistrationId(path, "channel");
            final Channel channel = webpushServer.newChannel(registrationId);
            LOGGER.info("Created channel {} " + channel);
            notificationStreams.put(channel.endpointToken(), registrationId);
            final Http2Headers responseHeaders = new DefaultHttp2Headers(false)
                    .status(CREATED.codeAsText())
                    .set(LOCATION, new AsciiString("webpush/" + channel.endpointToken()))
                    .set(ACCESS_CONTROL_ALLOW_ORIGIN, ANY_ORIGIN)
                    .set(ACCESS_CONTROL_EXPOSE_HEADERS, new AsciiString("Location"))
                    .set(CACHE_CONTROL, new AsciiString("private, max-age=" + webpushServer.config().channelMaxAge()));
            encoder.writeHeaders(ctx, streamId, responseHeaders, 0, true, ctx.newPromise());
        } catch (RegistrationNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void handleChannelRemoval(final String path, final ChannelHandlerContext ctx, final int streamId) {
        //TODO: implement removal.
    }

    /*
      A monitor request is responded to with a push promise. A push promise is associated with a
      previous client-initiated request (the monitor request)
     */
    private void handleMonitor(final String path, final ChannelHandlerContext ctx, final int streamId) {
        try {
            final Http2Headers responseHeaders = new DefaultHttp2Headers(false)
                    .status(OK.codeAsText())
                    .set(CONTENT_TYPE, new AsciiString("text/event-stream"))
                    .set(ACCESS_CONTROL_ALLOW_ORIGIN, ANY_ORIGIN)
                    .set(ACCESS_CONTROL_EXPOSE_HEADERS, CONTENT_TYPE);
            final String registrationId = extractRegistrationId(path, "monitor");
            final int pushStreamId = encoder.connection().local().nextStreamId();
            monitoredStreams.put(registrationId, Optional.of(new Client(ctx, pushStreamId)));
            LOGGER.info("Monitor ctx={}, registrationId={}, pushPromiseStreamId={}", ctx, registrationId, pushStreamId);
            LOGGER.info("Monitor {}", ctx);
            encoder.writePushPromise(ctx, streamId, pushStreamId, responseHeaders, 0, ctx.newPromise());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String extractRegistrationId(final String path, final String segment) {
        final String subpath = path.substring(0, path.indexOf(segment) -1);
        return subpath.subSequence(subpath.lastIndexOf('/') + 1, subpath.length()).toString();
    }

    private static String extractEndpointToken(final String path) {
        return path.substring(path.lastIndexOf("/") + 1);
    }

}
