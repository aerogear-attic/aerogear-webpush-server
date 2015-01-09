/**
 * JBoss, Home of Professional Open Source Copyright Red Hat, Inc., and individual contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package org.jboss.aerogear.webpush;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

public class DefaultWebPushServerConfigTest {

    @Test
    public void endpointHost() {
        final WebPushServerConfig config = DefaultWebPushConfig.create()
                .endpointHost("localhost")
                .password("dummy").build();
        assertThat(config.endpointHost(), equalTo("localhost"));
    }

    @Test
    public void endpointPort() {
        final WebPushServerConfig config = DefaultWebPushConfig.create()
                .endpointPort(8888)
                .password("dummy").build();
        assertThat(config.endpointPort(), is(8888));
    }

    @Test
    public void endpointPortNegative() {
        final WebPushServerConfig config = DefaultWebPushConfig.create()
                .endpointPort(-8888)
                .password("dummy").build();
        assertThat(config.endpointPort(), is(7777));
    }

    @Test (expected = IllegalStateException.class)
    public void shouldThrowIfMaxSizeLessThanMaxLowerBound() {
        DefaultWebPushConfig.create().messageMaxSize(20L).password("dummy").build();
    }

    @Test
    public void messageMaxSize() {
        final WebPushServerConfig config = DefaultWebPushConfig.create()
                .messageMaxSize(4097L)
                .password("dummy").build();
        assertThat(config.messageMaxSize(), is(4097L));
    }

}
