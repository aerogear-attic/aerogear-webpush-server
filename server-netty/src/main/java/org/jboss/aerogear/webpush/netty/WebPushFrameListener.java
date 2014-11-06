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

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.AsciiString;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.Http2FrameAdapter;
import io.netty.handler.codec.http2.Http2Headers;
import org.jboss.aerogear.webpush.Channel;
import org.jboss.aerogear.webpush.Registration;
import org.jboss.aerogear.webpush.WebPushServer;
import org.jboss.aerogear.webpush.datastore.RegistrationNotFoundException;
import org.jboss.aerogear.webpush.util.ArgumentUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

import static io.netty.handler.codec.http.HttpHeaderNames.CACHE_CONTROL;
import static io.netty.handler.codec.http.HttpHeaderNames.LOCATION;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;

public class WebPushFrameListener extends Http2FrameAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebPushNettyServer.class);
    private static final int PROMISE_STREAM_ID = 2;
    public static final AsciiString LINK = new AsciiString("link");
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
        if (!endStream) {
            return;
        }
        final String path = headers.path().toString();
        LOGGER.debug("streamId={}, method={}, path={}", streamId, headers.method(), path);

        switch (headers.method().toString()) {
            case "POST":
                if (path.contains("/register")) {
                    handleDeviceRegistration(ctx, streamId);
                } else if (path.contains("channel")) {
                    handleChannelCreation(path, ctx, streamId);
                }
                break;
            case "GET":
                if (path.contains("monitor")) {
                    handleMonitor(ctx, streamId);
                }
                break;
            case "DELETE":
                handleChannelRemoval(path, ctx, streamId);
                break;
        }
    }

    private void handleDeviceRegistration(final ChannelHandlerContext ctx, final int streamId) {
        final Registration registration = webpushServer.register();
        final Http2Headers responseHeaders = new DefaultHttp2Headers(false)
                .status(OK.codeAsText())
                .set(CACHE_CONTROL, new AsciiString("private, max-age=" + webpushServer.config().registrationMaxAge()))
                .set(LOCATION, new AsciiString(registration.monitorURI().toString()))
                .set(LINK, asLink(registration.channelURI(), "push:channel"));
        encoder.writeHeaders(ctx, streamId, responseHeaders, 0, false, ctx.newPromise());
    }

    private static AsciiString asLink(final URI uri, final String type) {
        return new AsciiString("<" + uri + ">;rel=\"" +  type + "\"");
    }

    private void handleChannelCreation(final String path, final ChannelHandlerContext ctx, final int streamId) {
        try {
            final Channel channel = webpushServer.newChannel(extractRegistrationId(path));
            final Http2Headers responseHeaders = new DefaultHttp2Headers(false)
                    .status(CREATED.codeAsText())
                    .set(LOCATION, new AsciiString(channel.endpointToken()))
                    .set(CACHE_CONTROL, new AsciiString("private, max-age=" + webpushServer.config().channelMaxAge()));
            encoder.writeHeaders(ctx, streamId, responseHeaders, 0, false, ctx.newPromise());
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
    private void handleMonitor(final ChannelHandlerContext ctx, final int streamId) {
        final Http2Headers responseHeaders = new DefaultHttp2Headers(false);
        encoder.writePushPromise(ctx, streamId, PROMISE_STREAM_ID, responseHeaders, 0, ctx.newPromise());
    }

    private static String extractRegistrationId(final String path) {
        final String subpath = path.substring(path.indexOf("/") + 1);
        return subpath.subSequence(0, subpath.indexOf('/')).toString();
    }

}
