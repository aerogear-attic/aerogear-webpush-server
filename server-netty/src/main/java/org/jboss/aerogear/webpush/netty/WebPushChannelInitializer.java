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

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
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

import static io.netty.handler.codec.http.HttpServerUpgradeHandler.UpgradeCodec;

class WebPushChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final SslContext sslCtx;
    private final DataStore dataStore;
    private final WebPushServerConfig config;
    private final byte[] privateKey;

    WebPushChannelInitializer(final SslContext sslCtx,
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
            configureClearText(ch, webPushServer);
        }
    }

    private void configureSsl(final SocketChannel ch, final WebPushServer webPushServer) {
        ch.pipeline().addLast(sslCtx.newHandler(ch.alloc()), new Http2OrHttpHandler(webPushServer));
    }

    private static void configureClearText(final SocketChannel ch, final WebPushServer webPushServer) {
        final HttpServerCodec sourceCodec = new HttpServerCodec();
        final HttpServerUpgradeHandler upgradeHandler =
                new HttpServerUpgradeHandler(sourceCodec, new WebPushCodecFactory(webPushServer), 65536);
        ch.pipeline().addLast(sourceCodec);
        ch.pipeline().addLast(upgradeHandler);
    }

    private static class WebPushCodecFactory implements UpgradeCodecFactory {

        private final WebPushServer webPushServer;

        WebPushCodecFactory(final WebPushServer webPushServer) {
            this.webPushServer = webPushServer;
        }

        @Override
        public UpgradeCodec newUpgradeCodec(final CharSequence protocol) {
            if (Http2CodecUtil.HTTP_UPGRADE_PROTOCOL_NAME.equals(protocol)) {
                return new Http2ServerUpgradeCodec(new WebPushHttp2HandlerBuilder()
                        .webPushServer(webPushServer)
                        .build());
            } else {
                return null;
            }
        }
    }

}
