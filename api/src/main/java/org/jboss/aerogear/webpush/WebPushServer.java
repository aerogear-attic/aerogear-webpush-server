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

import java.util.Optional;

/**
 * A Java implementation of <a href="http://tools.ietf.org/html/draft-thomson-webpush-http2-01">WebPush</a> Server.
 */
public interface WebPushServer {

    /**
     * Registers a device with this server as per
     * <a href="https://tools.ietf.org/html/draft-thomson-webpush-http2-01#section-4.">Section 4</a> of
     * the specification.
     * <p>
     * This establishes a shared session between the device and the server.
     * A new registration does not have any channels associated with it.
     *
     * @return {@link Registration} the response for this registration.
     */
    Registration register();

    /**
     * Handles the creation of new channels for a registration.
     *
     * @param registrationId the registration id for which this new channel belongs to.
     */
    Optional<Channel> newChannel(String registrationId);

    /**
     * Retrieves a channels.
     *
     * @param path
     */
    Optional<Channel> getChannel(String path);

    /**
     * Removes the specified channel
     *
     * @param channel the channel to be removed.
     */
    void removeChannel(Channel channel);

    /**
     * Handles the retrieval of a channnel's message.
     * This enables clients to query the server for the latest notification/message.
     *
     * @param endpointToken the endpoint token for the channel.
     * @return {code String} the latest notification/message for the specified channel.
     */
    Optional<String> getMessage(String endpointToken);

    /**
     * Set the notifcation/message for a channel.
     *
     * @param endpointToken that identifies the channel.
     * @param content the new content.
     */
    void setMessage(String endpointToken, String content);

    /**
     * Handles device monitorURI requests which are a signal to the server to begin delivering
     * push notification/messages to the client.
     *
     * @param registrationId the id of the registration.
     * @param channelUri the channelURI uri to monitorURI.
     */
    void monitor(String registrationId, String channelUri);

    /**
     * Returns the configuration for this WebPush server.
     *
     * @return {@link WebPushServerConfig} this servers configuration.
     */
    WebPushServerConfig config();

}
