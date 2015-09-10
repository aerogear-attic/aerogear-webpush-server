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
import org.junit.Test;

import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InMemoryDataStoreTest {

    @Test
    public void saveSubscription() {
        final InMemoryDataStore store = new InMemoryDataStore();
        final Subscription subscription = mockSubscription(UUID.randomUUID().toString(), "p123");
        store.saveSubscription(subscription);
        final Optional<Subscription> optionalSub = store.subscription(subscription.id());
        assertThat(optionalSub.isPresent(), is(true));
        assertThat(optionalSub.get().id(), equalTo(subscription.id()));
        assertThat(optionalSub.get().pushResourceId(), equalTo(subscription.pushResourceId()));
    }

    private static Subscription mockSubscription(final String id, final String pushResourceId) {
        final Subscription r = mock(Subscription.class);
        when(r.id()).thenReturn(id);
        when(r.pushResourceId()).thenReturn(pushResourceId);
        return r;
    }

}
