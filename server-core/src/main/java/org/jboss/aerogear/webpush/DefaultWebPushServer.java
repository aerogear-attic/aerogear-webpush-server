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

import org.jboss.aerogear.crypto.Random;
import org.jboss.aerogear.webpush.datastore.DataStore;
import org.jboss.aerogear.webpush.util.CryptoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class DefaultWebPushServer implements WebPushServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultWebPushServer.class);
    private final DataStore store;
    private final WebPushServerConfig config;
    private final byte[] privateKey;

    /**
     * Sole constructor.
     *
     * @param store the {@link DataStore} that this server should use.
     * @param config the {@link WebPushServerConfig} for this server.
     */
    public DefaultWebPushServer(final DataStore store, final WebPushServerConfig config, final byte[] privateKey) {
        this.store = store;
        this.config = config;
        this.privateKey = privateKey;
    }

    /**
     * TODO add comments
     */
    @Override
    public Subscription subscribe() {
        String id = UUID.randomUUID().toString();
        String pushResourceId = UUID.randomUUID().toString();
        Subscription subscription = new DefaultSubscription(id, pushResourceId);
        store.saveSubscription(subscription);
        return subscription;
    }

    @Override
    public Optional<Subscription> subscriptionById(String subscriptionId) {
        return store.subscription(subscriptionId);
    }

    @Override
    public Optional<Subscription> subscriptionByToken(String subscriptionToken) {
        try {
            String subscriptionId = CryptoUtil.decrypt(privateKey, subscriptionToken);
            return subscriptionById(subscriptionId);
        } catch (Exception e) {
            LOGGER.debug(e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Subscription> subscriptionByPushToken(String pushToken) {
        try {
            String decrypt = CryptoUtil.decrypt(privateKey, pushToken);
            String[] tokens = decrypt.split(CryptoUtil.DELIMITER);
            Optional<Subscription> subscription = store.subscription(tokens[1]);
            if (subscription.isPresent()) {
                Subscription sub = subscription.get();
                if (sub.pushResourceId().equals(tokens[0])) {
                    return subscription;
                }
            }
        } catch (final Exception e) {
            LOGGER.debug(e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Subscription> subscriptionByReceiptToken(String receiptToken) {
        try {
            String decrypt = CryptoUtil.decrypt(privateKey, receiptToken);
            String[] tokens = decrypt.split(CryptoUtil.DELIMITER);
            return store.subscription(tokens[1]);
        } catch (final Exception e) {
            LOGGER.debug(e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<PushMessage> removeSubscription(String id) {
        return store.removeSubscription(id);
    }

    @Override
    public void saveMessage(PushMessage msg) {
        store.saveMessage(msg);
    }

    @Override
    public List<PushMessage> waitingDeliveryMessages(String subId) {
        return store.waitingDeliveryMessages(subId);
    }

    @Override
    public void saveSentMessage(PushMessage msg) {
        store.saveSentMessage(msg);
    }

    @Override
    public Optional<PushMessage> sentMessage(String pushMsgResource) {
        try {
            String decrypt = CryptoUtil.decrypt(privateKey, pushMsgResource);
            String[] tokens = decrypt.split(CryptoUtil.DELIMITER);
            return store.sentMessage(tokens[1], tokens[0]);
        } catch (final Exception e) {
            LOGGER.debug(e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public WebPushServerConfig config() {
        return config;
    }

    public static byte[] generateAndStorePrivateKey(final DataStore store, final WebPushServerConfig config) {
        byte[] keySalt = store.getPrivateKeySalt();
        if (keySalt.length == 0) {
            keySalt = new Random().randomBytes();
            store.savePrivateKeySalt(keySalt);
        }
        return CryptoUtil.secretKey(config.password(), keySalt);
    }

    @Override
    public String generateEndpointToken(final String value) {
        return CryptoUtil.endpointToken(privateKey, value);
    }

    @Override
    public String generateEndpointToken(final String firstId, final String secondId) {
        final String value = firstId + CryptoUtil.DELIMITER + secondId;
        return CryptoUtil.endpointToken(privateKey, value);
    }
}
