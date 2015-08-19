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

import org.jboss.aerogear.webpush.datastore.DataStore;
import org.jboss.aerogear.webpush.datastore.InMemoryDataStore;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class DefaultWebPushServerTest {

    private DefaultWebPushServer server;

    @Before
    public void setup() {
        final DataStore dataStore = new InMemoryDataStore();
        final WebPushServerConfig config = DefaultWebPushConfig.create().password("test")
                .cert("/selfsigned.crt")
                .privateKey("/demo.key")
                .build();
        final byte[] privateKey = DefaultWebPushServer.generateAndStorePrivateKey(dataStore, config);
        server = new DefaultWebPushServer(dataStore, config, privateKey);
    }

    @Test
    public void subscribe() {
        final Subscription reg = server.subscribe();
        assertThat(reg.id(), is(notNullValue()));
    }

    @Test
    public void removeSubscription() throws Exception {
        final Subscription newSubscription = server.subscribe();
        final Optional<Subscription> subscription = server.subscriptionById(newSubscription.id());
        assertThat(subscription.isPresent(), equalTo(true));
        assertThat(subscription.get().id(), equalTo(newSubscription.id()));
        server.removeSubscription(subscription.get().id());
        assertThat(server.subscriptionById(newSubscription.id()).isPresent(), is(false));
    }

    @Test
    public void waitingDeliveryMessages() throws Exception {
        final Subscription subscription = server.subscribe();
        final String messageId = UUID.randomUUID().toString();
        server.saveMessage(new DefaultPushMessage(messageId,
                subscription.id(),
                Optional.empty(),
                "testing",
                0));
        final List<PushMessage> message = server.waitingDeliveryMessages(subscription.id());
        assertThat(message.get(0).payload(), equalTo("testing"));
        assertThat(message.get(0).ttl(), equalTo(0));
    }

}
