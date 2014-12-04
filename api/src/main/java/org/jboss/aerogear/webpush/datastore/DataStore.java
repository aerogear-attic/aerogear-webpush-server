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

import java.util.Optional;
import java.util.Set;

import org.jboss.aerogear.webpush.Channel;
import org.jboss.aerogear.webpush.Registration;

/**
 * Handles the storing of channels for a WebPush Server implementation.
 */
public interface DataStore {

    /**
     * Saves the server's private key salt.
     *
     * @param salt the server's private key salt.
     */
    void savePrivateKeySalt(byte[] salt);

    /**
     * Returns the server's private key salt.
     *
     * @return {@code byte[]} the server's private key salt if one has previously been saved, or an empty byte array.
     */
    byte[] getPrivateKeySalt();

    /**
     * Saves a {@link Registration} to the underlying storage system.
     *
     * @param registration the channelURI to be stored.
     * @return {@code true} if storage was successful.
     */
    boolean saveRegistration(Registration registration);

    /**
     * Returns the {@link Registration} for the passed-in id.
     *
     * @param registrationId the registration identifier to retreive.
     */
    Optional<Registration> getRegistration(String registrationId);

    /**
     * Saves a {@link Channel} to the underlying storage system.
     *
     * @param channel the channel to store.
     */
    void saveChannel(Channel channel);

    /**
     * Remove a {@link Channel} from the underlying storage system.
     *
     * @param channel the channel to remove
     */
    void removeChannel(Channel channel);

    /**
     * Returns registrations for a certain registration.
     *
     * @param registrationId the registration identifier.
     * @return {@code Set<Channel>} the registered channelURI uri's.
     */
    Set<Channel> getChannels(String registrationId);

}