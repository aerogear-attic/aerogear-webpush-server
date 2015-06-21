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

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http2.Http2OrHttpChooser.SelectedProtocol;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolConfig.Protocol;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectedListenerFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectorFailureBehavior;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import org.jboss.aerogear.webpush.WebPushServerConfig;
import org.jboss.aerogear.webpush.datastore.DataStore;
import org.jboss.aerogear.webpush.datastore.InMemoryDataStore;
import org.jboss.aerogear.webpush.standalone.ConfigReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A HTTP/2 based WebPush Server.
 */
public final class WebPushNettyServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebPushNettyServer.class);
    private static final String DEFAULT_CONFIG = "/webpush-config.json";

    public static void main(final String[] args) throws Exception {
        final WebPushServerConfig config = readConfig(args);
        final DataStore inMemoryDataStore = new InMemoryDataStore();
        final SslContext sslCtx = createSslContext(config);

        final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        final EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            final ServerBootstrap b = new ServerBootstrap();
            b.option(ChannelOption.SO_BACKLOG, 1024)
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new WebPushChannelInitializer(sslCtx, inMemoryDataStore, config));
            final Channel ch = b.bind(config.host(), config.port()).sync().channel();
            LOGGER.info("WebPush server bound to {}:{}", config.host(), config.port());
            ch.closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    private static WebPushServerConfig readConfig(final String[] args) throws Exception {
        return ConfigReader.parse(args.length == 1 ? args[0] : DEFAULT_CONFIG);
    }

    private static SslContext createSslContext(final WebPushServerConfig config) throws Exception {
        if (config.useEndpointTls()) {
            return SslContextBuilder.forServer(config.cert(), config.privateKey())
                    .sslProvider(SslProvider.JDK)
                    .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
                    .applicationProtocolConfig(new ApplicationProtocolConfig(
                            protocol(config),
                            SelectorFailureBehavior.FATAL_ALERT,
                            SelectedListenerFailureBehavior.FATAL_ALERT,
                            SelectedProtocol.HTTP_2.protocolName(),
                            SelectedProtocol.HTTP_1_1.protocolName()))
                    .build();
        }
        return null;
    }

    private static Protocol protocol(final WebPushServerConfig config) {
        switch (config.protocol()) {
            case ALPN:
                return Protocol.ALPN;
            case NPN:
                return Protocol.NPN;
            default:
                throw new IllegalStateException("Protocol not supported [" + config.protocol() + "]");
        }
    }
}
