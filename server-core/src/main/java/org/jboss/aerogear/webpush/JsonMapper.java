package org.jboss.aerogear.webpush;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.jboss.aerogear.webpush.AggregateChannel.Entry;
import org.jboss.aerogear.webpush.DefaultAggregateChannel.DefaultEntry;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class JsonMapper {

    private static ObjectMapper om = createObjectMapper();

    private static ObjectMapper createObjectMapper() {
        om = new ObjectMapper();
        final SimpleModule module = new SimpleModule("Module", new Version(1, 0, 0, null, "aerogear", "webpush"));
        module.addDeserializer(AggregateChannel.class, new AggregateChannelDeserializer());
        module.addSerializer(AggregateChannel.class, new AggregateChannelSerializer());
        om.registerModule(module);
        return om;
    }

    private JsonMapper() {
    }

    /**
     * Transforms from Java object notation to JSON.
     *
     * @param obj the Java object to transform into JSON.
     * @return {@code String} the json representation for the object.
     */
    public static String toJson(final Object obj) {
        try {
            return om.writeValueAsString(obj);
        } catch (final Exception e) {
            throw new RuntimeException("error trying to parse json [" + obj + ']', e);
        }
    }

    /**
     * Transforms from JSON to the type specified.
     *
     * @param json the json to be transformed.
     * @param type the Java type that the JSON should be transformed to.
     * @return T an instance of the type populated with data from the json message.
     */
    public static <T> T fromJson(final String json, final Class<T> type) {
        try {
            return om.readValue(json, type);
        } catch (final Exception e) {
            throw new RuntimeException("error trying to parse json [" + json + ']', e);
        }
    }

    public static String pretty(final String json) {
        try {
            final Object o = om.readValue(json, Object.class);
            return om.writerWithDefaultPrettyPrinter().writeValueAsString(o);
        } catch (final Exception e) {
            throw new RuntimeException("error trying to parse json [" + json + ']', e);
        }
    }

    private static class AggregateChannelDeserializer extends JsonDeserializer<AggregateChannel> {

        @Override
        public AggregateChannel deserialize(final JsonParser jp, final DeserializationContext ctxt) throws IOException {
            final ObjectCodec oc = jp.getCodec();
            final JsonNode tree = oc.readTree(jp);
            final Set<Entry> channels = new LinkedHashSet<>();
            if (tree.isArray()) {
                tree.forEach(node -> {
                    final Map.Entry<String, JsonNode> entry = node.fields().next();
                    final JsonNode objectNode = entry.getValue();
                    final Optional<JsonNode> expires = Optional.ofNullable(objectNode.get("expires"));
                    final Optional<JsonNode> pubkey = Optional.ofNullable(objectNode.get("pubkey"));
                    channels.add(new DefaultEntry(entry.getKey(),
                            expires.map(n -> n.asLong(0)),
                            pubkey.map(JsonMapper::parseBinaryValue)));
                });
            }
            return new DefaultAggregateChannel(channels);
        }
    }

    private static byte[] parseBinaryValue(final JsonNode node) {
        try {
            return node.binaryValue();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static class AggregateChannelSerializer extends JsonSerializer<AggregateChannel> {

        @Override
        public void serialize(final AggregateChannel aggregateChannel,
                              final JsonGenerator jgen,
                              final SerializerProvider provider) throws IOException {

            jgen.writeStartArray();
            for (Entry entry: aggregateChannel.channels()) {
                jgen.writeStartObject();
                jgen.writeObjectFieldStart(entry.endpoint());
                entry.expires().ifPresent(l -> writeLongField("expires", l, jgen));
                entry.pubkey().ifPresent(b -> writeBinaryField("pubkey", b, jgen));
                jgen.writeEndObject();
                jgen.writeEndObject();
            }
            jgen.writeEndArray();
        }
    }

    private static void writeLongField(final String name, final long value, final JsonGenerator gen) {
        try {
            gen.writeNumberField(name, value);
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void writeBinaryField(final String name, final byte[] value, final JsonGenerator gen) {
        try {
            gen.writeBinaryField(name, value);
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

}
