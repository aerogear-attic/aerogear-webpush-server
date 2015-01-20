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
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.AsciiString;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Headers;
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
import org.jboss.aerogear.webpush.AggregateSubscription.Entry;
import org.jboss.aerogear.webpush.DefaultAggregateSubscription.DefaultEntry;

import javax.net.ssl.SSLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static io.netty.buffer.Unpooled.copiedBuffer;
import static io.netty.handler.codec.http.HttpMethod.POST;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpMethod.PUT;
import static io.netty.handler.codec.http.HttpMethod.DELETE;
import static io.netty.util.CharsetUtil.UTF_8;

public class WebPushClient {

    private final String host;
    private final int port;
    private final boolean ssl;
    private final List<String> protocols;
    private NioEventLoopGroup workerGroup;
    private Channel channel;
    private final EventHandler handler;

    private WebPushClient(final Builder builder) {
        host = builder.host;
        port = builder.port;
        ssl = builder.ssl;
        protocols = builder.protocols;
        handler = builder.handler;
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
            b.handler(new WebPushClientInitializer(configureSsl(), Integer.MAX_VALUE, handler));
            channel = b.connect().syncUninterruptibly().channel();
            System.out.println("Connected to [" + host + ':' + port + ']');
        } catch (final Exception e) {
            e.printStackTrace();
            workerGroup.shutdownGracefully();
        }
    }

    public void register() throws Exception {
        writeRequest(POST, "/webpush/register", Unpooled.buffer());
    }

    public void monitor(final String monitorUrl, final boolean now) throws Exception {
        final Http2Headers headers = http2Headers(GET, monitorUrl);
        if (now) {
            headers.add(new AsciiString("prefer"), new AsciiString("wait=0"));
        }
        writeRequest(headers);
    }

    public void createSubscription(final String subscribeUrl) throws Exception {
        writeRequest(POST, subscribeUrl, Unpooled.buffer());
    }

    public void status(final String endpointUrl) throws Exception {
        writeRequest(GET, endpointUrl);
    }

    public void deleteSubscription(final String endpointUrl) throws Exception {
        writeRequest(DELETE, endpointUrl);
    }

    public void createAggregateSubscription(final String aggregateUrl, final String json) throws Exception {
        writeRequest(POST, aggregateUrl, copiedBuffer(json, UTF_8));
    }

    public static Set<Entry> asEntries(final String[] endpointUrls) {
        final Set<Entry> entries = new HashSet<>(endpointUrls.length);
        for (String url: endpointUrls) {
            entries.add(new DefaultEntry(url, Optional.of(0L)));
        }
        return entries;
    }

    public void notify(final String endpointUrl, final String payload) throws Exception {
        writeRequest(PUT, endpointUrl, copiedBuffer(payload, UTF_8));
    }

    private void writeRequest(final HttpMethod method, final String url) throws Exception {
        final Http2Headers headers = http2Headers(method, url);
        writeRequest(headers);
    }

    private void writeRequest(final Http2Headers headers) throws Exception {
        handler.outbound(headers);
        ChannelFuture requestFuture = channel.writeAndFlush(new WebPushMessage(headers)).sync();
        requestFuture.sync();
    }

    private void writeRequest(final HttpMethod method, final String url, final ByteBuf payload) throws Exception {
        final Http2Headers headers = http2Headers(method, url);
        handler.outbound(headers);
        ChannelFuture requestFuture = channel.writeAndFlush(new WebPushMessage(headers, Optional.of(payload))).sync();
        requestFuture.sync();
    }

    private Http2Headers http2Headers(final HttpMethod method, final String url) {
        final URI hostUri = URI.create("https://" + host + ":" + port + "/" + url);
        final Http2Headers headers = new DefaultHttp2Headers(false).method(method.name());
        headers.path(asciiString(url));
        headers.authority(asciiString(hostUri.getAuthority()));
        headers.scheme(asciiString(hostUri.getScheme()));
        return headers;
    }

    private static AsciiString asciiString(final String str) {
        return new AsciiString(str);
    }

    public void disconnect() {
        if (channel != null) {
            channel.close();
            shutdown();
        }
    }

    public boolean isConnected() {
        return channel != null && channel.isOpen();
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
        private final List<String> protocols = new ArrayList<>();
        private EventHandler handler;

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

        public Builder notificationHandler(final EventHandler handler) {
            this.handler = handler;
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
