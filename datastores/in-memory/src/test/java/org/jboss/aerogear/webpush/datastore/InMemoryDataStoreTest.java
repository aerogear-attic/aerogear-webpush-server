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

import org.jboss.aerogear.webpush.Registration;
import org.junit.Test;

import java.net.URI;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InMemoryDataStoreTest {

    @Test
    public void saveSubscription() {
        final InMemoryDataStore store = new InMemoryDataStore();
        final Registration registration = mockRegistration(UUID.randomUUID().toString(), asURI("regURI"), asURI("subscribeURI"));
        final boolean saved = store.saveRegistration(registration);
        assertThat(saved, is(true));
    }

    private static URI asURI(final String uri) {
        return URI.create(uri);
    }

    private static Registration mockRegistration(final String id, final URI monitorURI, final URI subscribeUri) {
        final Registration r = mock(Registration.class);
        when(r.id()).thenReturn(id);
        when(r.uri()).thenReturn(monitorURI);
        when(r.subscribeUri()).thenReturn(subscribeUri);
        return r;
    }

}
