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

/**
 * Configuration settings for WebPush server
 */
public interface WebPushServerConfig {

    enum Protocol {
        ALPN, NPN
    }

    /**
     * The default prefix for the the notification endpoint url. This
     * prefix will be used and returned to the client to enable the
     * client to PUT notifications.
     */
    String DEFAULT_ENDPOINT_URL_PREFIX = "/update";

    /**
     * The host that this server will bind to.
     *
     * @return {@code String} the host.
     */
    String host();

    /**
     * The port that this server will bind to.
     *
     * @return {@code port} the port.
     */
    int port();

    /**
     * Returnes the {@link Protocol} to use.
     *
     * @return {@link Protocol} the protocol used for TLS application protocol negotiation.
     */
    Protocol protocol();

    /**
     * Determins whether transport layer security is in use.
     *
     * @return {@code true} if transport layer security is in use.
     */
    boolean useEndpointTls();

    /**
     * The password for the private key.
     *
     * @return {@code String[]} password used for generating the server's private key.
     */
    String password();

    /**
     * Returns the endpoint url prefix for this WebPush server.
     * This will get the channelId appended to it.
     *
     * @return {@code String} the endpoint url prefix.
     */
    String endpointPrefix();

    /**
     * Returns the notification endpoint url prefix for this WebPush server.
     * This will be the in the format:
     * protocol://endpointHost:endpointPort/endpointPrefix
     *
     * @return {@code String} the notification url.
     */
    String endpointUrl();

    /**
     * The externally available host that this server is reachable by.
     *
     * @return {@code String} the host.
     */
    String endpointHost();

    /**
     * The externally available port that this server is reachable by.
     *
     * @return {@code port} the port.
     */
    int endpointPort();

    /**
     * Returns the maximum age for a registration
     */
    long registrationMaxAge();

    /**
     * Returns the maximum age for a channel.
     */
    long channelMaxAge();

}
