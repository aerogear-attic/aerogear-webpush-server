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

public enum Resource {

    SUBSCRIBE("subscribe"),
    SUBSCRIPTION("s"),
    PUSH("p"),
    PUSH_MESSAGE("d"),
    RECEIPTS("receipts"),
    RECEIPT("r");

    private final String resourceName;

    Resource(final String resourceName) {
        this.resourceName = resourceName;
    }

    public String resourceName() {
        return resourceName;
    }

    public static Resource byResourceName(String name) {
        for (Resource resource : Resource.values()) {
            if (resource.resourceName().equalsIgnoreCase(name)) {
                return resource;
            }
        }
        return null;
    }
}
