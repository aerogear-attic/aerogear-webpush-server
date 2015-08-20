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

import java.util.Objects;

public class DefaultSubscription implements Subscription {

    private final String id;
    private final String pushResourceId;

    public DefaultSubscription(final String id, final String pushResourceId) {
        this.id = Objects.requireNonNull(id, "id");
        this.pushResourceId = Objects.requireNonNull(pushResourceId, "pushResourceId");
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String pushResourceId() {
        return pushResourceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DefaultSubscription)) {
            return false;
        }

        DefaultSubscription that = (DefaultSubscription) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "DefaultSubscription{" +
                "id='" + id + '\'' +
                ", pushResourceId='" + pushResourceId + '\'' +
                '}';
    }
}
