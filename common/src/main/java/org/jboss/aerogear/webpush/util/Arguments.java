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
package org.jboss.aerogear.webpush.util;

/**
 * Utility methods for checking method arguments.
 */
public final class Arguments {

    private Arguments() {
    }

    /**
     * Checks that the given argument is not null. If it is, throws {@link NullPointerException}.
     * Otherwise, returns the argument.
     *
     * @param arg the argument to check.
     * @param text the text to display if the {@code arg} was null
     */
    public static <T> T checkNotNull(final T arg, final String text) {
        if (arg == null) {
            throw new NullPointerException(text);
        }
        return arg;
    }
}
