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


import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.jboss.aerogear.webpush.Subscription;
import org.jboss.aerogear.webpush.Registration;

/**
 * A {@link DataStore} implementation that stores all information in memory.
 */
public class InMemoryDataStore implements DataStore {

    private final ConcurrentMap<String, Registration> registrations = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Set<Subscription>> subscriptions = new ConcurrentHashMap<>();

    private byte[] salt;

    @Override
    public void savePrivateKeySalt(final byte[] salt) {
        if (this.salt != null) {
            this.salt = salt;
        }
    }

    @Override
    public byte[] getPrivateKeySalt() {
        if (salt == null) {
            return new byte[]{};
        }
        return salt;
    }

    @Override
    public boolean saveRegistration(final Registration registration) {
        Objects.requireNonNull(registration, "registration must not be null");
        return registrations.putIfAbsent(registration.id(), registration) == null;
    }

    @Override
    public Optional<Registration> getRegistration(final String id) {
        return Optional.ofNullable(registrations.get(id));
    }

    @Override
    public void saveChannel(final Subscription subscription) {
        final String id = subscription.registrationId();
        final Set<Subscription> newSubscriptions = Collections.newSetFromMap(new ConcurrentHashMap<>());
        newSubscriptions.add(subscription);
        while (true) {
            final Set<Subscription> currentSubscriptions = subscriptions.get(id);
            if (currentSubscriptions == null) {
                final Set<Subscription> previous = subscriptions.putIfAbsent(id, newSubscriptions);
                if (previous != null) {
                    newSubscriptions.addAll(previous);
                    if (subscriptions.replace(id, previous, newSubscriptions)) {
                        break;
                    }
                } else {
                    break;
                }
            } else {
                newSubscriptions.addAll(currentSubscriptions);
                if (subscriptions.replace(id, currentSubscriptions, newSubscriptions)) {
                    break;
                }
            }
        }
    }

    @Override
    public void removeChannel(final Subscription subscription) {
        Objects.requireNonNull(subscription, "subscription must not be null");
        while (true) {
            final Set<Subscription> currentSubscriptions = subscriptions.get(subscription.registrationId());
            if (currentSubscriptions == null || currentSubscriptions.isEmpty()) {
                break;
            }
            final Set<Subscription> newSubscriptions = Collections.newSetFromMap(new ConcurrentHashMap<>());
            boolean added = newSubscriptions.addAll(currentSubscriptions);
            if (!added){
                break;
            }

            boolean removed = newSubscriptions.remove(subscription);
            if (removed) {
                if (subscriptions.replace(subscription.registrationId(), currentSubscriptions, newSubscriptions)) {
                    break;
                }
            }
        }
    }

    @Override
    public Set<Subscription> getSubscriptions(final String registrationId) {
        final Set<Subscription> subscriptions = this.subscriptions.get(registrationId);
        if (subscriptions == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(subscriptions);
    }

}
