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
package org.jboss.aerogear.webpush.standalone;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.aerogear.webpush.DefaultWebPushConfig;
import org.jboss.aerogear.webpush.DefaultWebPushConfig.Builder;
import org.jboss.aerogear.webpush.WebPushServerConfig;

/**
 * Utility to read a JSON config files.
 */
public class ConfigReader {

    private static final ObjectMapper OM = new ObjectMapper();

    private ConfigReader() {
    }

    /**
     * Will parse the passed in file, which can either be a file on the file system
     * or a file on the classpath into a {@link WebPushServerConfig} instance.
     *
     *
     * @param fileName the name of a file on the file system or on the classpath.
     * @return {@link WebPushServerConfig} populated with the values in the JSON configuration file.
     * @throws Exception
     */
    public static WebPushServerConfig parse(final String fileName) throws Exception {
        final File configFile = new File(fileName);
        InputStream in = null;
        try {
            in = configFile.exists() ? new FileInputStream(configFile) : ConfigReader.class.getResourceAsStream(fileName);
            return parse(in);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    /**
     * Will parse the passed InputStream into a {@link WebPushServerConfig} instance.
     *
     *
     * @param in the input stream to parse. Should be from a JSON source representing a SimplePush configuration.
     * @return {@link WebPushServerConfig} populated with the values in the JSON input stream.
     */
    public static WebPushServerConfig parse(final InputStream in) {
        try {
            final JsonNode json = OM.readTree(in);
            return parseSimplePushProperties(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static WebPushServerConfig parseSimplePushProperties(final JsonNode json) {
        final JsonNode host = json.get("host");
        final JsonNode port = json.get("port");
        final Builder builder = DefaultWebPushConfig.create(host.asText(), port.asInt());
        final JsonNode password = json.get("password");
        if (password != null) {
            builder.password(password.asText());
        }
        final JsonNode endpointHost = json.get("endpoint-host");
        if (endpointHost != null) {
            builder.endpointHost(endpointHost.asText());
        }
        final JsonNode endpointPort = json.get("endpoint-port");
        if (endpointPort != null) {
            builder.endpointPort(endpointPort.asInt());
        }
        final JsonNode endpointTls = json.get("endpoint-tls");
        if (endpointTls != null) {
            builder.endpointTls(endpointTls.asBoolean());
        }
        final JsonNode endpointPrefix = json.get("endpoint-prefix");
        if (endpointPrefix != null) {
            builder.endpointPrefix(endpointPrefix.asText());
        }
        final JsonNode registrationMaxAge = json.get("registration-max-age");
        if (registrationMaxAge != null) {
            builder.registrationMaxAge(registrationMaxAge.asLong());
        }
        final JsonNode channelMaxAge = json.get("channel-max-age");
        if (channelMaxAge != null) {
            builder.channelMaxAge(channelMaxAge.asLong());
        }
        return builder.build();
    }

}
