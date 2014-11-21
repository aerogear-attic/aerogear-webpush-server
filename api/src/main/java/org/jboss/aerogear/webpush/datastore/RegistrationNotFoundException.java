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

import java.util.UUID;

/**
 * An exception to signal that a channelURI could not be located in the
 * DataStore in use.
 */
public class RegistrationNotFoundException extends Exception {

    private final String registrationId;

    /**
     * @param message a description of when the exception occurred.
     * @param registrationId the registration id that could not be located.
     */
    public RegistrationNotFoundException(final String message, final String registrationId) {
        super(message);
        this.registrationId = registrationId;
    }

    /**
     * @param message a description of when the exception occurred.
     * @param registrationId the registration id that could not be located.
     */
    public RegistrationNotFoundException(final String message, final String registrationId, final Throwable cause) {
        super(message, cause);
        this.registrationId = registrationId;
    }

    /**
     * Return the registration id that could not be located.
     *
     * @return {@code String} the registration id.
     */
    public String getRegistrationId() {
        return registrationId;
    }

}
