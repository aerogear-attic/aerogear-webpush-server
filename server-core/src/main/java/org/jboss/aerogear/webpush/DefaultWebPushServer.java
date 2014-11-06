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
import org.jboss.aerogear.webpush.datastore.RegistrationNotFoundException;
import org.jboss.aerogear.webpush.util.CryptoUtil;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Set;
import java.util.UUID;

public class DefaultWebPushServer implements WebPushServer {

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
        final DefaultRegistration reg = new DefaultRegistration(id, monitorURI(stringId), channelURI(stringId));
        store.saveRegistration(reg);
        return reg;
    }

    private static URI monitorURI(final String id) {
        return webpushURI(id, ":push:monitor");
    }

    private static URI channelURI(final String id) {
        return webpushURI(id, ":push:channel");
    }

    private static URI webpushURI(final String id, final String postfix) {
        return URI.create("webpush:" + id + postfix);
    }

    private static String urlEncodeId(final String id) {
        try {
            return URLEncoder.encode(id, "ASCII");
        } catch (final UnsupportedEncodingException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Channel newChannel(final String registrationId) throws RegistrationNotFoundException {
        final Registration registration = store.getRegistration(registrationId);
        final String channelId = UUID.randomUUID().toString();
        final String endpointToken = generateEndpointToken(registration.id(), channelId);
        final DefaultChannel newChannel = new DefaultChannel(registration.id(), channelId, endpointToken);
        store.saveChannel(newChannel);
        return newChannel;
    }

    @Override
    public String getMessage(final String endpointToken) {
        return getChannel(endpointToken).message();
    }

    @Override
    public void setMessage(String endpointToken, String content) {
        final Channel channel = getChannel(endpointToken);
        store.saveChannel(new DefaultChannel(channel.registrationId(), channel.channelId(), endpointToken, content));
    }

    private Channel getChannel(final String endpointToken) {
        try {
            final String decrypt = CryptoUtil.decrypt(privateKey, endpointToken);
            final String[] tokens = decrypt.split("\\.");
            final Set<Channel> channels = store.getChannels(tokens[0]);
            for (Channel channel : channels) {
                if (channel.channelId().equals(tokens[1])) {
                    return channel;
                }
            }
            throw new RuntimeException("No channel for endpoint found");
        } catch (final Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
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
