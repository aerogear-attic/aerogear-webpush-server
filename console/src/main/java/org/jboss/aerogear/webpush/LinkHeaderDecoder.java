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

import io.netty.handler.codec.AsciiString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class LinkHeaderDecoder {
    private static final Pattern CLEAN_HREF = Pattern.compile("^['\"\\s<]+|['\"\\s>]+$");
    private static final Pattern CLEAN_PARAM = Pattern.compile("^['\"\\s]+|['\"\\s]+$");

    private final AsciiString header;
    private List<Map<String, String>> links;

    public LinkHeaderDecoder(final AsciiString header) {
        this.header = header;
    }

    private static String cleanURL(final AsciiString url) {
        return CLEAN_HREF.matcher(url).replaceAll("");
    }

    private static String cleanParam(final AsciiString param) {
        return CLEAN_PARAM.matcher(param).replaceAll("");
    }

    public List<Map<String, String>> getLinks() {
        if (this.links != null) {
            return this.links;
        }
        if (this.header == null || this.header.length() == 0) {
            this.links = Collections.emptyList();
            return this.links;
        }
        this.links = new ArrayList<Map<String, String>>();
        final AsciiString[] headers = this.header.split(',');
        for (AsciiString header : headers) {
            final AsciiString[] params = header.split(';');
            if (params.length < 1) {
                continue;
            }
            final Map<String, String> link = new LinkedHashMap<String, String>();
            link.put("href", cleanURL(params[0]));
            for (int i = 1; i < params.length; i++) {
                final AsciiString param = params[i];
                final int valueIndex = param.indexOf(0x3d); // '='
                String key, value;
                if (valueIndex < 0 || valueIndex == param.length()) {
                    key = value = cleanParam(param);
                } else {
                    key = cleanParam(param.subSequence(0, valueIndex));
                    value = cleanParam(param.subSequence(valueIndex + 1));
                }
                link.put(key, value);
            }
            this.links.add(link);
        }
        return this.links;
    }

    public Map<String, String> getParamsByField(final String name, final String value) {
        final List<Map<String, String>> links = this.getLinks();
        for (Map<String, String> link : links) {
            String param = link.get(name);
            if (param != null && param.equals(value)) {
                return link;
            }
        }
        return null;
    }

    public String getURLByRel(final String rel) {
        Map<String, String> params = this.getParamsByField("rel", rel);
        if (params == null) {
            return null;
        }
        return params.get("href");
    }

    public String toString() {
      final List<Map<String, String>> links = this.getLinks();
      return links.toString();
    }
}
