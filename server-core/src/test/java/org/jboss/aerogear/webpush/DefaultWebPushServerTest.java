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

import java.util.Optional;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class DefaultWebPushServerTest {

    private DefaultWebPushServer server;

    @Before
    public void setup() {
        final DataStore dataStore = new InMemoryDataStore();
        final WebPushServerConfig config = DefaultWebPushConfig.create().password("test").build();
        final byte[] privateKey = DefaultWebPushServer.generateAndStorePrivateKey(dataStore, config);
        server = new DefaultWebPushServer(dataStore, config, privateKey);
    }

    @Test
    public void register() {
        final Registration reg = server.register();
        assertThat(reg.id(), is(notNullValue()));
        assertThat(reg.monitorUri().toString(), equalTo("webpush/" + reg.id() + "/monitor"));
        assertThat(reg.channelUri().toString(), equalTo("webpush/" + reg.id() + "/channel"));
        assertThat(reg.aggregateUri().toString(), equalTo("webpush/" + reg.id() + "/aggregate"));
    }

    @Test
    public void newChannel() throws Exception {
        final Registration reg = server.register();
        final Optional<Channel> ch = server.newChannel(reg.id());
        assertThat(ch.isPresent(), equalTo(true));
        assertThat(ch.get().registrationId(), equalTo(reg.id()));
        assertThat(ch.get().message(), equalTo(Optional.empty()));
    }

    @Test
    public void removeChannel() throws Exception {
        final Registration reg = server.register();
        final Optional<Channel> ch = server.newChannel(reg.id());
        assertThat(ch.isPresent(), equalTo(true));
        assertThat(ch.get().registrationId(), equalTo(reg.id()));
        server.removeChannel(ch.get());
    }

    @Test
    public void setAndGetMessage() throws Exception {
        final Registration reg = server.register();
        final Optional<Channel> ch = server.newChannel(reg.id());
        assertThat(ch.isPresent(), equalTo(true));
        server.setMessage(ch.get().endpointToken(), Optional.of("some message"));
        final Optional<String> message = server.getMessage(ch.get().endpointToken());
        assertThat(message.get(), equalTo("some message"));
    }

}
