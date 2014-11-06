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

public final class DefaultWebPushConfig implements WebPushServerConfig {

    private final String host;
    private final int port;
    private final boolean endpointTls;
    private final String password;
    private final String endpointPrefix;
    private final String endpointUrl;
    private final String endpointHost;
    private final int endpointPort;
    private final long registrationMaxAge;
    private final long channelMaxAge;

    private DefaultWebPushConfig(final Builder builder) {
        host = builder.host;
        port = builder.port;
        endpointHost = builder.endpointHost == null ? host : builder.endpointHost;
        endpointPort = builder.endpointPort <= 0 ? port : builder.endpointPort;
        endpointTls = builder.endpointTls;
        endpointPrefix = builder.endpointPrefix.startsWith("/") ? builder.endpointPrefix : "/" + builder.endpointPrefix;
        endpointUrl = makeEndpointUrl(endpointHost, endpointPort, endpointPrefix, endpointTls);
        password = builder.password;
        registrationMaxAge = builder.registrationMaxAge;
        channelMaxAge = builder.channelMaxAge;
    }

    private static String makeEndpointUrl(final String endpointHost, final int endpointPort, final String prefix, final boolean tls) {
        return new StringBuilder(tls ? "https://" : "http://")
            .append(endpointHost).append(":").append(endpointPort)
            .append(prefix)
            .toString();
    }

    @Override
    public String host() {
        return host;
    }

    @Override
    public int port() {
        return port;
    }

    @Override
    public String password() {
        return password;
    }

    @Override
    public boolean useEndpointTls() {
        return endpointTls;
    }

    @Override
    public String endpointUrl() {
        return endpointUrl;
    }

    @Override
    public String endpointPrefix() {
        return endpointPrefix;
    }

    @Override
    public String endpointHost() {
        return endpointHost;
    }

    @Override
    public int endpointPort() {
        return endpointPort;
    }

    @Override
    public long registrationMaxAge() {
        return registrationMaxAge;
    }

    @Override
    public long channelMaxAge() {
        return channelMaxAge;
    }

    public String toString() {
        return new StringBuilder("WebPushConfig[host=").append(host)
                .append(", port=").append(port)
                .append(", endpointHost=").append(endpointHost)
                .append(", endpointPort=").append(endpointPort)
                .append(", endpointTls=").append(endpointTls)
                .append(", endpointUrlPrefix=").append(endpointPrefix)
                .append(", endpointUrl=").append(endpointUrl)
                .append("]").toString();
    }

    public static Builder create() {
        return new DefaultWebPushConfig.Builder().host("127.0.0.1").port(7777);
    }

    public static Builder create(final String host, final int port) {
        return new DefaultWebPushConfig.Builder().host(host).port(port);
    }

    public static class Builder {
        private String host;
        private int port;
        private String password;
        private boolean endpointTls;
        private String endpointPrefix = DEFAULT_ENDPOINT_URL_PREFIX;
        private String endpointHost;
        private int endpointPort;
        private long registrationMaxAge = 604800000L;
        private long channelMaxAge = 604800000L;

        public Builder host(final String host) {
            if (host != null) {
                this.host = host;
            }
            return this;
        }

        public Builder port(final int port) {
            this.port = port;
            return this;
        }

        public Builder password(final String password) {
            this.password = password;
            return this;
        }

        public Builder endpointTls(final boolean tls) {
            endpointTls = tls;
            return this;
        }

        public Builder endpointHost(final String endpointHost) {
            this.endpointHost = endpointHost;
            return this;
        }

        public Builder endpointPrefix(final String endpointPrefix) {
            if (endpointPrefix != null) {
                this.endpointPrefix = endpointPrefix;
            }
            return this;
        }

        public Builder endpointPort(final int endpointPort) {
            this.endpointPort = endpointPort;
            return this;
        }

        public Builder registrationMaxAge(final Long maxAge) {
            if (maxAge != null) {
                this.registrationMaxAge = maxAge;
            }
            return this;
        }

        public Builder channelMaxAge(final Long maxAge) {
            if (maxAge != null) {
                this.channelMaxAge = maxAge;
            }
            return this;
        }

        public WebPushServerConfig build() {
            if (password == null) {
                throw new IllegalStateException("No 'password' was configured!");
            }
            return new DefaultWebPushConfig(this);
        }
    }

}
