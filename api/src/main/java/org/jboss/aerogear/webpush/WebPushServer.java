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

import java.util.List;
import java.util.Optional;

/**
 * A Java implementation of <a href="http://tools.ietf.org/html/draft-thomson-webpush-http2-01">WebPush</a> Server.
 */
public interface WebPushServer {

    /**
     * TODO add comments
     */
    Subscription subscription();

    Optional<Subscription> subscriptionById(String id);

    Optional<Subscription> subscriptionByToken(String token);

    Optional<Subscription> subscriptionByPushToken(String pushToken);

    Optional<Subscription> subscriptionByReceiptToken(String receiptToken);

    List<PushMessage> removeSubscription(String id);

    void saveMessage(PushMessage msg);

    List<PushMessage> waitingDeliveryMessages(String subId);

    void saveSentMessage(PushMessage msg);

    Optional<PushMessage> sentMessage(String pushMsgResource);

    /**
     * Returns the configuration for this WebPush server.
     *
     * @return {@link WebPushServerConfig} this servers configuration.
     */
    WebPushServerConfig config();

    String generateEndpointToken(String value);

    String generateEndpointToken(String firstId, String secondId);
}
