/**
 * JBoss, Home of Professional Open Source
 * Copyright Red Hat, Inc., and individual contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.aerogear.webpush.util;

import io.netty.util.AsciiString;

public final class HttpHeaders {

    // custom headers:
    public static final AsciiString LINK_HEADER = new AsciiString("link");
    public static final AsciiString PUSH_RECEIPT_HEADER = new AsciiString("push-receipt");
    public static final AsciiString PREFER_HEADER = new AsciiString("prefer");
    public static final AsciiString TTL_HEADER = new AsciiString("ttl");

    // header values:
    public static final AsciiString ALLOW_ORIGIN_ANY = new AsciiString("*");
    public static final AsciiString CACHE_CONTROL_PRIVATE = new AsciiString("private");
    public static final AsciiString CONTENT_TYPE_VALUE = new AsciiString("text/plain;charset=utf8");
    public static final AsciiString EXPOSE_HEADERS_CACHE_CONTROL_CONTENT_TYPE_CONTENT_LENGTH
            = new AsciiString("cache-control, content-type");
    public static final AsciiString EXPOSE_HEADERS_LINK_CACHE_CONTROL_LOCATION
            = new AsciiString("link, cache-control, location");
    public static final AsciiString EXPOSE_HEADERS_LOCATION = new AsciiString("location");
    public static final AsciiString WAIT_0 = new AsciiString("wait=0");

    private HttpHeaders() {}
}
