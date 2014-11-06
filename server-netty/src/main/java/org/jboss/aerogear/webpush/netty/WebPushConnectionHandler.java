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

import static io.netty.buffer.Unpooled.copiedBuffer;
import static io.netty.buffer.Unpooled.unreleasableBuffer;
import static io.netty.util.CharsetUtil.UTF_8;
import static io.netty.util.internal.logging.InternalLogLevel.INFO;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.AsciiString;
import io.netty.handler.codec.http2.*;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.jboss.aerogear.webpush.WebPushServer;

public class WebPushConnectionHandler extends Http2ConnectionHandler {

    private static final Http2FrameLogger logger = new Http2FrameLogger(INFO,
            InternalLoggerFactory.getInstance(WebPushConnectionHandler.class));
    static final ByteBuf MONITOR_MSG = unreleasableBuffer(copiedBuffer("monitorURI channelURI", UTF_8));

    private static final Http2FrameLogger FRAME_LOGGER = new Http2FrameLogger(INFO,
            InternalLoggerFactory.getInstance(WebPushConnectionHandler.class));

    static final ByteBuf RESPONSE_BYTES = unreleasableBuffer(copiedBuffer("Hello World", UTF_8));

    private final WebPushServer webPushServer;

    public WebPushConnectionHandler(final WebPushServer webPushServer) {
        this(new DefaultHttp2Connection(true),
                new Http2InboundFrameLogger(
                new DefaultHttp2FrameReader(), logger),
                new Http2OutboundFrameLogger(
                new DefaultHttp2FrameWriter(), logger),
                new SimpleHttp2FrameListener(),
                webPushServer);
    }

    private WebPushConnectionHandler(final Http2Connection connection,
                                     final Http2FrameReader frameReader,
                                     final Http2FrameWriter frameWriter,
                                     final SimpleHttp2FrameListener listener,
                                     final WebPushServer webPushServer) {
        super(connection, frameReader, frameWriter, listener);
        listener.encoder(encoder());
        this.webPushServer = webPushServer;
    }

    /*
    @Override
    public void onHeadersRead(final ChannelHandlerContext ctx,
                              final int streamId,
                              final Http2Headers headers,
                              final int streamDependency,
                              final short weight,
                              final boolean exclusive,
                              final int padding,
                              final boolean endStream,
                              final boolean endSegment) throws Http2Exception {
        connection().stream(streamId).data(headers);
        final String method = headers.method();
        final String path = headers.path();
        logger.info("Request path = " + path);

        if ("POST".equals(method) && "/register".equals(path) && endStream) {
            final Registration registration = webPushServer.register();
            final Http2Headers responseHeaders = newBuilder().status("200")
                    .add("Cache-Control", "private, max-age=" + webPushServer.config().registrationMaxAge())
                    .add("Location", registration.monitorURI())
                    .add("Link", registration.channelURI()).build();
            writeHeaders(ctx(), ctx().newPromise(), streamId, responseHeaders, 0, false, false);
        } else if ("GET".equals(method) && "/monitor".equals(path) && endStream) {
            sendResponse(streamId, 200, MONITOR_MSG.duplicate());
        }
    }

    @Override
    public void onDataRead(final ChannelHandlerContext ctx,
                           final int streamId,
                           final ByteBuf data,
                           final int padding,
                           final boolean endOfStream,
                           final boolean endOfSegment) throws Http2Exception {
        final Http2Headers headers = connection().stream(streamId).data();
        final String path = headers.path();
        if ("/channel".equals(path) && "POST".equals(headers.method()) && endOfStream) {
            sendResponse(streamId, 200, data.retain());
        }
    }

    private void sendResponse(final int streamId, final int status, final ByteBuf payload) {
        final Http2Headers headers = newBuilder().status(String.valueOf(status)).build();
        writeHeaders(ctx(), ctx().newPromise(), streamId, headers, 0, false, false);
        writeData(ctx(), ctx().newPromise(), streamId, payload, 0, true, true);
    }
    */

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    private static class SimpleHttp2FrameListener extends Http2FrameAdapter {
        private Http2ConnectionEncoder encoder;

        public void encoder(Http2ConnectionEncoder encoder) {
            this.encoder = encoder;
        }

        /**
         * If receive a frame with end-of-stream set, send a pre-canned response.
         */
        @Override
        public void onDataRead(ChannelHandlerContext ctx, int streamId, ByteBuf data, int padding,
                               boolean endOfStream) throws Http2Exception {
            if (endOfStream) {
                sendResponse(ctx, streamId, data.retain());
            }
        }

        /**
         * If receive a frame with end-of-stream set, send a pre-canned response.
         */
        @Override
        public void onHeadersRead(ChannelHandlerContext ctx, int streamId,
                                  Http2Headers headers, int streamDependency, short weight,
                                  boolean exclusive, int padding, boolean endStream) throws Http2Exception {
            if (endStream) {
                sendResponse(ctx, streamId, RESPONSE_BYTES.duplicate());
            }
        }

        /**
         * Sends a "Hello World" DATA frame to the client.
         */
        private void sendResponse(ChannelHandlerContext ctx, int streamId, ByteBuf payload) {
            // Send a frame for the response status
            Http2Headers headers = new DefaultHttp2Headers().status(new AsciiString("200"));
            encoder.writeHeaders(ctx, streamId, headers, 0, false, ctx.newPromise());
            encoder.writeData(ctx, streamId, payload, 0, true, ctx.newPromise());
            ctx.flush();
        }
    };
}
