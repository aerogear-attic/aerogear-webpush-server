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
import io.netty.handler.codec.http2.DefaultHttp2Connection;
import io.netty.handler.codec.http2.DefaultHttp2FrameReader;
import io.netty.handler.codec.http2.DefaultHttp2FrameWriter;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2ConnectionHandler;
import io.netty.handler.codec.http2.Http2FrameReader;
import io.netty.handler.codec.http2.Http2FrameWriter;
import org.jboss.aerogear.webpush.WebPushServer;

public class WebPushHttp2Handler extends Http2ConnectionHandler {

    private final WebPushFrameListener listener;

    public WebPushHttp2Handler(final WebPushServer webpushServer) {
        this(new DefaultHttp2Connection(true),
                new DefaultHttp2FrameReader(),
                new DefaultHttp2FrameWriter(),
                new WebPushFrameListener(webpushServer));
    }

    private WebPushHttp2Handler(final Http2Connection connection,
                                final Http2FrameReader frameReader,
                                final Http2FrameWriter frameWriter,
                                final WebPushFrameListener listener) {
        super(connection, frameReader, frameWriter, listener);
        this.listener = listener;
        listener.encoder(encoder());
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    public void channelUnregistered(final ChannelHandlerContext ctx) throws Exception {
        listener.disconnect(ctx);
        super.channelUnregistered(ctx);
    }
}
