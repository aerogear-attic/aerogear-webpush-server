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
     * A new registration does not have any subscriptions associated with it.
     *
     * @return {@link Registration} the response for this registration
     */
    Registration register();

    /**
     * Returns the {@link Registration} for the specified id.
     *
     * @param id the registration identifier.
     * @return {@code Optional} {@link Registration} with the registration or {@code Optional.empty}
     */
    Optional<Registration> registration(final String id);

    /**
     * Handles the creation of new subscriptions for a registration.
     *
     * @param registrationId the registration id that this new subscription belongs to
     */
    Optional<Subscription> newSubscription(String registrationId);

    /**
     * Retrieves a subscriptions.
     *
     * @param endpoint the endpoint to retrieve
     * @return {@code Optional} an {@link Optional} {@link Subscription}
     */
    Optional<Subscription> subscription(String endpoint);

    /**
     * Removes the specified subscription.
     *
     * @param subscription the subscription to be removed
     */
    void removeSubscription(Subscription subscription);

    /**
     * Handles the retrieval of a subscriptions's message.
     * This enables clients to query the server for the latest notification/message.
     *
     * @param endpoint the endpoint for the subscriptoin
     * @return {code String} the latest notification/message for the specified subscription
     */
    Optional<String> getMessage(String endpoint);

    /**
     * Set the notifcation/message for a subscription.
     *
     * @param endpoint that identifies the subscription.
     * @param content the new content.
     */
    void setMessage(String endpoint, Optional<String> content);

    /**
     * Handles device monitorURI requests which are a signal to the server to begin delivering
     * push notification/messages to the client.
     *
     * @param registrationId the id of the registration.
     * @param endpoint the endpoint
     */
    void monitor(String registrationId, String endpoint);

    /**
     * Returns the configuration for this WebPush server.
     *
     * @return {@link WebPushServerConfig} this servers configuration.
     */
    WebPushServerConfig config();

}
