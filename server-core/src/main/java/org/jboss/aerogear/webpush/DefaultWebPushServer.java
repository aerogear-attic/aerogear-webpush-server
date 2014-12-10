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

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Optional;
import java.util.Set;
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

    @Override
    public Registration register() {
        final String id = UUID.randomUUID().toString();
        final String stringId = urlEncodeId(id);
        final DefaultRegistration reg = new DefaultRegistration(id,
                monitorUri(stringId),
                channelUri(stringId),
                aggregateUri(stringId));
        store.saveRegistration(reg);
        return reg;
    }

    @Override
    public Optional<Registration> registration(final String id) {
        return store.getRegistration(id);
    }

    private static URI monitorUri(final String id) {
        return webpushURI(id, "/monitor");
    }

    private static URI channelUri(final String id) {
        return webpushURI(id, "/channel");
    }

    private static URI aggregateUri(final String id) {
        return webpushURI(id, "/aggregate");
    }

    private static URI webpushURI(final String id, final String postfix) {
        return URI.create("webpush/" + id + postfix);
    }

    private static String urlEncodeId(final String id) {
        try {
            return URLEncoder.encode(id, "ASCII");
        } catch (final UnsupportedEncodingException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Optional<Channel> newChannel(final String registrationId) {
        final Optional<Registration> registration = store.getRegistration(registrationId);
        return registration.map(r -> {
            final String channelId = UUID.randomUUID().toString();
            final String endpointToken = generateEndpointToken(r.id(), channelId);
            final DefaultChannel newChannel = new DefaultChannel(r.id(), channelId, endpointToken);
            store.saveChannel(newChannel);
            return newChannel;
        });
    }

    @Override
    public void removeChannel(Channel channel) {
        store.removeChannel(channel);
    }

    @Override
    public Optional<String> getMessage(final String endpointToken) {
        return getChannel(endpointToken).flatMap(Channel::message);
    }

    @Override
    public void setMessage(final String endpointToken, final Optional<String> content) {
        getChannel(endpointToken).ifPresent(ch ->
            store.saveChannel(new DefaultChannel(ch.registrationId(),
                    ch.channelId(),
                    endpointToken,
                    content))
        );
    }

    public Optional<Channel> getChannel(final String endpointToken) {
        try {
            final String decrypt = CryptoUtil.decrypt(privateKey, endpointToken);
            final String[] tokens = decrypt.split("\\.");
            final Set<Channel> channels = store.getChannels(tokens[0]);
            return channels.stream().filter(c -> c.channelId().equals(tokens[1])).findAny();
        } catch (final Exception e) {
            LOGGER.debug(e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public void monitor(String registrationId, String channelUri) {
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

    private String generateEndpointToken(final String uaid, final String channelId) {
        return CryptoUtil.endpointToken(uaid, channelId, privateKey);
    }

}
