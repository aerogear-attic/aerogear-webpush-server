/**
 * JBoss, Home of Professional Open Source
 * Copyright Red Hat, Inc., and individual contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.aerogear.webpush;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.ChannelPromiseAggregator;
import io.netty.handler.codec.http2.Http2ConnectionDecoder;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2ConnectionHandler;
import io.netty.handler.codec.http2.Http2Settings;

public class WebPushToHttp2ConnectionHandler extends Http2ConnectionHandler {

    protected WebPushToHttp2ConnectionHandler(final Http2ConnectionDecoder decoder,
                                              final Http2ConnectionEncoder encoder,
                                              final Http2Settings initialSettings) {
        super(decoder, encoder, initialSettings);
    }

    @Override
    public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) {
        if (msg instanceof WebPushMessage) {
            final WebPushMessage message = (WebPushMessage) msg;
            final Http2ConnectionEncoder encoder = encoder();
            final int streamId = connection().local().incrementAndGetNextStreamId();
            if (message.hasData()) {
                final ChannelPromiseAggregator promiseAggregator = new ChannelPromiseAggregator(promise);
                final ChannelPromise headerPromise = ctx.newPromise();
                final ChannelPromise dataPromise = ctx.newPromise();
                promiseAggregator.add(headerPromise, dataPromise);
                encoder.writeHeaders(ctx, streamId, message.headers(), 0, false, headerPromise);
                encoder.writeData(ctx, streamId, message.payload(), 0, true, dataPromise);
            } else {
                encoder.writeHeaders(ctx, streamId, message.headers(), 0, true, promise);
            }
        } else {
            ctx.write(msg, promise);
        }
    }
}
