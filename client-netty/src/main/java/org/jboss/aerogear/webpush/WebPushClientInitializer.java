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
package org.jboss.aerogear.webpush;

import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.ChannelPromiseAggregator;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpClientUpgradeHandler;
import io.netty.handler.codec.http2.DefaultHttp2Connection;
import io.netty.handler.codec.http2.DefaultHttp2FrameReader;
import io.netty.handler.codec.http2.DefaultHttp2FrameWriter;
import io.netty.handler.codec.http2.DefaultHttp2InboundFlowController;
import io.netty.handler.codec.http2.DefaultHttp2OutboundFlowController;
import io.netty.handler.codec.http2.DelegatingDecompressorFrameListener;
import io.netty.handler.codec.http2.Http2ClientUpgradeCodec;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2ConnectionHandler;
import io.netty.handler.codec.http2.Http2FrameListener;
import io.netty.handler.codec.http2.Http2FrameReader;
import io.netty.handler.codec.http2.Http2FrameWriter;
import io.netty.handler.codec.http2.Http2InboundFlowController;
import io.netty.handler.codec.http2.Http2OutboundFlowController;
import io.netty.handler.codec.http2.InboundHttp2ToHttpAdapter;
import io.netty.handler.ssl.SslContext;

import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class WebPushClientInitializer extends ChannelInitializer<SocketChannel> {

    private final SslContext sslCtx;
    private final int maxContentLength;
    private final ResponseHandler callback;
    private WebPushToHttp2ConnectionHandler connectionHandler;
    private HttpResponseHandler responseHandler;
    private Http2SettingsHandler settingsHandler;


    public WebPushClientInitializer(SslContext sslCtx, int maxContentLength, final ResponseHandler callback) {
        this.sslCtx = sslCtx;
        this.maxContentLength = maxContentLength;
        this.callback = callback;
    }

    @Override
    public void initChannel(SocketChannel ch) throws Exception {
        final Http2Connection connection = new DefaultHttp2Connection(false);
        final Http2FrameWriter frameWriter = new DefaultHttp2FrameWriter();
        final Http2FrameReader frameReader = new WebPushFrameReader(callback, new DefaultHttp2FrameReader());
        connectionHandler = new WebPushToHttp2ConnectionHandler(connection,
                frameReader,
                frameWriter,
                new DefaultHttp2InboundFlowController(connection, frameWriter),
                new DefaultHttp2OutboundFlowController(connection, frameWriter),
                new DelegatingDecompressorFrameListener(connection,
                        new InboundHttp2ToHttpAdapter.Builder(connection).maxContentLength(maxContentLength).build()));
        responseHandler = new HttpResponseHandler();
        settingsHandler = new Http2SettingsHandler(ch.newPromise());

        if (sslCtx != null) {
            configureSsl(ch);
        } else {
            configureClearText(ch);
        }
    }

    public HttpResponseHandler responseHandler() {
        return responseHandler;
    }

    public Http2SettingsHandler settingsHandler() {
        return settingsHandler;
    }

    protected void configureEndOfPipeline(ChannelPipeline pipeline) {
        pipeline.addLast("Http2SettingsHandler", settingsHandler);
        pipeline.addLast("HttpResponseHandler", responseHandler);
    }

    /**
     * Configure the pipeline for TLS NPN negotiation to HTTP/2.
     */
    private void configureSsl(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast("SslHandler", sslCtx.newHandler(ch.alloc()));
        pipeline.addLast("Http2Handler", connectionHandler);
        configureEndOfPipeline(pipeline);
    }

    /**
     * Configure the pipeline for a cleartext upgrade from HTTP to HTTP/2.
     */
    private void configureClearText(SocketChannel ch) {
        HttpClientCodec sourceCodec = new HttpClientCodec();
        Http2ClientUpgradeCodec upgradeCodec = new Http2ClientUpgradeCodec(connectionHandler);
        HttpClientUpgradeHandler upgradeHandler = new HttpClientUpgradeHandler(sourceCodec, upgradeCodec, 65536);
        ch.pipeline().addLast("Http2SourceCodec", sourceCodec);
        ch.pipeline().addLast("Http2UpgradeHandler", upgradeHandler);
        ch.pipeline().addLast("Http2UpgradeRequestHandler", new UpgradeRequestHandler());
        ch.pipeline().addLast("Logger", new UserEventLogger());
    }

    /**
     * A handler that triggers the cleartext upgrade to HTTP/2 by sending an initial HTTP request.
     */
    private final class UpgradeRequestHandler extends ChannelHandlerAdapter {
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            DefaultFullHttpRequest upgradeRequest = new DefaultFullHttpRequest(HTTP_1_1, GET, "/");
            ctx.writeAndFlush(upgradeRequest);
            super.channelActive(ctx);
            ctx.pipeline().remove(this);
            WebPushClientInitializer.this.configureEndOfPipeline(ctx.pipeline());
        }
    }

    /**
     * Class that logs any User Events triggered on this channel.
     */
    private static class UserEventLogger extends ChannelHandlerAdapter {
        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            System.out.println("User Event Triggered: " + evt);
            super.userEventTriggered(ctx, evt);
        }
    }

    public static class WebPushToHttp2ConnectionHandler extends Http2ConnectionHandler {

        public WebPushToHttp2ConnectionHandler(final Http2Connection connection,
                                               final Http2FrameReader frameReader,
                                               final Http2FrameWriter frameWriter,
                                               final Http2InboundFlowController inboundFlow,
                                               final Http2OutboundFlowController outboundFlow,
                                               final Http2FrameListener listener) {
            super(connection, frameReader, frameWriter, inboundFlow, outboundFlow, listener);
        }

        @Override
        public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) {
            if (msg instanceof WebPushMessage) {
                final WebPushMessage message = (WebPushMessage) msg;
                final Http2ConnectionEncoder encoder = encoder();
                final int streamId = connection().local().nextStreamId();
                if (message.hasData()) {
                    final ChannelPromiseAggregator promiseAggregator = new ChannelPromiseAggregator(promise);
                    final ChannelPromise headerPromise = ctx.newPromise();
                    final ChannelPromise dataPromise = ctx.newPromise();
                    promiseAggregator.add(headerPromise, dataPromise);
                    encoder.writeHeaders(ctx, streamId, message.headers(), 0, false, headerPromise);
                    encoder.writeData(ctx, streamId, message.payload().get(), 0, true, dataPromise);
                } else {
                    encoder.writeHeaders(ctx, streamId, message.headers(), 0, true, promise);
                }
            } else {
                ctx.write(msg, promise);
            }
        }

        @Override
        public void onException(ChannelHandlerContext ctx, Throwable cause) {
            //cause.printStackTrace();
            super.onException(ctx, cause);
        }
    }


}
