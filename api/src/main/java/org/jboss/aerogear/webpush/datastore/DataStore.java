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
package org.jboss.aerogear.webpush.datastore;

import org.jboss.aerogear.webpush.Subscription;
import org.jboss.aerogear.webpush.PushMessage;

import java.util.List;
import java.util.Optional;

/**
 * Handles the storing of subscriptions for a WebPush Server implementation.
 */
public interface DataStore {

    void saveSubscription(Subscription subscription);

    Optional<Subscription> subscription(String id);

    List<PushMessage> removeSubscription(String id);

    void saveMessage(PushMessage msg);

    List<PushMessage> waitingDeliveryMessages(String subId);

    void saveSentMessage(PushMessage msg);

    Optional<PushMessage> sentMessage(String subId, String msgId);

    /**
     * Saves the server's private key salt.
     *
     * @param salt the server's private key salt
     */
    void savePrivateKeySalt(byte[] salt);

    /**
     * Returns the server's private key salt.
     *
     * @return {@code byte[]} the server's private key salt if one has previously been saved, or an empty byte array
     */
    byte[] getPrivateKeySalt();

}
