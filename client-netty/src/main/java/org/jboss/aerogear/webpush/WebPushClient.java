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

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http2.Http2OrHttpChooser.SelectedProtocol;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolConfig.Protocol;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectedListenerFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectorFailureBehavior;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.CharsetUtil;

import javax.net.ssl.SSLException;
import java.util.ArrayList;
import java.util.List;

import static io.netty.handler.codec.http.HttpMethod.POST;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpMethod.PUT;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class WebPushClient {

    private final String host;
    private final int port;
    private final boolean ssl;
    private final List<String> protocols;
    private NioEventLoopGroup workerGroup;
    private Channel channel;

    private WebPushClient(final Builder builder) {
        this.host = builder.host;
        this.port = builder.port;
        this.ssl = builder.ssl;
        this.protocols = builder.protocols;
    }

    public String host() {
        return host;
    }

    public int port() {
        return port;
    }

    public void connect() throws Exception {
        workerGroup = new NioEventLoopGroup();
        try {
            final Bootstrap b = new Bootstrap();
            b.group(workerGroup);
            b.channel(NioSocketChannel.class);
            b.option(ChannelOption.SO_KEEPALIVE, true);
            b.remoteAddress(host, port);
            b.handler(new WebPushClientInitializer(configureSsl(), Integer.MAX_VALUE));
            channel = b.connect().syncUninterruptibly().channel();
            System.out.println("Connected to [" + host + ':' + port + ']');
        } catch (final Exception e) {
            e.printStackTrace();
            workerGroup.shutdownGracefully();
        }
    }

    public void register() throws Exception {
        writeRequest(new DefaultFullHttpRequest(HTTP_1_1, POST, "webpush/register"));
    }

    public void monitor(final String monitorUrl) throws Exception {
        writeRequest(new DefaultFullHttpRequest(HTTP_1_1, GET, monitorUrl));
    }

    public void createChannel(final String channelUrl) throws Exception {
        writeRequest(new DefaultFullHttpRequest(HTTP_1_1, POST, channelUrl));
    }

    public void notify(final String channelUrl, final String payload) throws Exception {
        final DefaultFullHttpRequest request = new DefaultFullHttpRequest(HTTP_1_1,
                PUT,
                channelUrl,
                Unpooled.copiedBuffer(payload, CharsetUtil.UTF_8));
        writeRequest(request);
    }

    private void writeRequest(final FullHttpRequest request) throws Exception {
        request.headers().add(HttpHeaderNames.HOST, host + ':' + port);
        ChannelFuture requestFuture = channel.writeAndFlush(request).sync();
        requestFuture.sync();
    }

    public void disconnect() {
        channel.close();
    }

    public void shutdown() {
        workerGroup.shutdownGracefully();
    }

    private SslContext configureSsl() throws SSLException {
        if (ssl) {
            return SslContext.newClientContext(SslProvider.JDK,
                    null,
                    InsecureTrustManagerFactory.INSTANCE,
                    Http2SecurityUtil.CIPHERS,
                    /* NOTE: the following filter may not include all ciphers required by the HTTP/2 specification
                     * Please refer to the HTTP/2 specification for cipher requirements. */
                    SupportedCipherSuiteFilter.INSTANCE,
                    new ApplicationProtocolConfig(
                            Protocol.ALPN,
                            SelectorFailureBehavior.FATAL_ALERT,
                            SelectedListenerFailureBehavior.FATAL_ALERT,
                            SelectedProtocol.HTTP_2.protocolName(),
                            SelectedProtocol.HTTP_1_1.protocolName()),
                    0, 0);

        }
        return null;
    }

    public static Builder forHost(final String host) {
        return new Builder(host);
    }

    public static class Builder {

        private final String host;
        private int port = 8080;
        private boolean ssl;
        private List<String> protocols = new ArrayList<String>();

        public Builder(final String host) {
            this.host = host;
        }

        public Builder port(final int port) {
            this.port = port;
            return this;
        }

        public Builder port(final String port) {
            this.port = Integer.parseInt(port);
            return this;
        }

        public Builder ssl(final boolean ssl) {
            this.ssl = ssl;
            return this;
        }

        public WebPushClient build() {
            if (protocols.isEmpty()) {
                protocols.add(SelectedProtocol.HTTP_2.protocolName());
                protocols.add(SelectedProtocol.HTTP_1_1.protocolName());
            }
            return new WebPushClient(this);
        }

    }

}
