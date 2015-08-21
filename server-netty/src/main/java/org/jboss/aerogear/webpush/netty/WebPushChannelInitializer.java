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
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerUpgradeHandler;
import io.netty.handler.codec.http.HttpServerUpgradeHandler.UpgradeCodecFactory;
import io.netty.handler.codec.http2.Http2CodecUtil;
import io.netty.handler.codec.http2.Http2ServerUpgradeCodec;
import io.netty.handler.ssl.SslContext;
import org.jboss.aerogear.webpush.DefaultWebPushServer;
import org.jboss.aerogear.webpush.WebPushServer;
import org.jboss.aerogear.webpush.WebPushServerConfig;
import org.jboss.aerogear.webpush.datastore.DataStore;

public class WebPushChannelInitializer extends ChannelInitializer<SocketChannel> {

    private static final int MAX_HTTP_CONTENT_LENGTH = 16 * 1024;

    private final SslContext sslCtx;
    private final DataStore dataStore;
    private final WebPushServerConfig config;
    private final byte[] privateKey;

    public WebPushChannelInitializer(final SslContext sslCtx,
                                     final DataStore dataStore,
                                     final WebPushServerConfig config) {
        this.sslCtx = sslCtx;
        this.dataStore = dataStore;
        this.config = config;
        privateKey = DefaultWebPushServer.generateAndStorePrivateKey(dataStore, config);
    }

    @Override
    public void initChannel(final SocketChannel ch) {
        final WebPushServer webPushServer = new DefaultWebPushServer(dataStore, config, privateKey);
        if (sslCtx != null) {
            configureSsl(ch, webPushServer);
        } else {
            final UpgradeCodecFactory upgradeCodecFactory = protocol -> {
                if (Http2CodecUtil.HTTP_UPGRADE_PROTOCOL_NAME.equals(protocol)) {
                    return new Http2ServerUpgradeCodec(new WebPushHttp2Handler(webPushServer));
                } else {
                    return null;
                }
            };
            configureClearText(ch, upgradeCodecFactory);
        }
    }

    private void configureSsl(final SocketChannel ch, final WebPushServer webPushServer) {
        ch.pipeline().addLast(sslCtx.newHandler(ch.alloc()), new Http2OrHttpHandler(webPushServer));
    }

    private static void configureClearText(final SocketChannel ch, final UpgradeCodecFactory upgradeCodecFactory) {
        final ChannelPipeline p = ch.pipeline();
        final HttpServerCodec sourceCodec = new HttpServerCodec();

        p.addLast(sourceCodec);
        p.addLast(new HttpServerUpgradeHandler(sourceCodec, upgradeCodecFactory));
        p.addLast(new SimpleChannelInboundHandler<HttpMessage>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, HttpMessage msg) throws Exception {
                // If this handler is hit then no upgrade has been attempted and the client is just talking HTTP.
                System.err.println("Directly talking: " + msg.protocolVersion() + " (no upgrade was attempted)");
                ChannelPipeline pipeline = ctx.pipeline();
                ChannelHandlerContext thisCtx = pipeline.context(this);
                pipeline.addAfter(thisCtx.name(), null, new WebPushHttp11Handler());
                pipeline.replace(this, null, new HttpObjectAggregator(MAX_HTTP_CONTENT_LENGTH));
                ctx.fireChannelRead(msg);
            }
        });
    }
}
