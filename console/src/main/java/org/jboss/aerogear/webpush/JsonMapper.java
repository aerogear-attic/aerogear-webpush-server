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
import org.jboss.aerogear.webpush.AggregateSubscription.DefaultEntry;
import org.jboss.aerogear.webpush.AggregateSubscription.Entry;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class JsonMapper {

    private static ObjectMapper om = createObjectMapper();

    private static ObjectMapper createObjectMapper() {
        om = new ObjectMapper();
        final SimpleModule module = new SimpleModule("Module", new Version(1, 0, 0, null, "aerogear", "webpush"));
        module.addDeserializer(AggregateSubscription.class, new AggregateChannelDeserializer());
        module.addSerializer(AggregateSubscription.class, new AggregateChannelSerializer());
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

    public static String pretty(final String json) {
        try {
            final Object o = om.readValue(json, Object.class);
            return om.writerWithDefaultPrettyPrinter().writeValueAsString(o);
        } catch (final Exception e) {
            throw new RuntimeException("error trying to parse json [" + json + ']', e);
        }
    }

    private static class AggregateChannelDeserializer extends JsonDeserializer<AggregateSubscription> {

        @Override
        public AggregateSubscription deserialize(final JsonParser jp, final DeserializationContext ctxt) throws IOException {
            final ObjectCodec oc = jp.getCodec();
            final JsonNode tree = oc.readTree(jp);
            final Set<Entry> subscriptions = new LinkedHashSet<>();
            if (tree.isArray()) {
                for (JsonNode node : tree) {
                    final Map.Entry<String, JsonNode> entry = node.fields().next();
                    final JsonNode objectNode = entry.getValue();
                    final JsonNode expires = objectNode.get("expires");
                    final JsonNode pubkey = objectNode.get("pubkey");
                    subscriptions.add(new DefaultEntry(entry.getKey(), expires.asLong(0), parseBinaryValue(pubkey)));
                }
            }
            return new AggregateSubscription(subscriptions);
        }
    }

    private static byte[] parseBinaryValue(final JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        try {
            return node.binaryValue();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static class AggregateChannelSerializer extends JsonSerializer<AggregateSubscription> {

        @Override
        public void serialize(final AggregateSubscription aggregateSubscription,
                              final JsonGenerator jgen,
                              final SerializerProvider provider) throws IOException {

            jgen.writeStartArray();
            for (Entry entry: aggregateSubscription.subscriptions()) {
                jgen.writeStartObject();
                jgen.writeObjectFieldStart(entry.endpoint());
                writeExpiresField(entry.expires() ,jgen);
                writePubkeyField(entry.pubkey(), jgen);
                jgen.writeEndObject();
                jgen.writeEndObject();
            }
            jgen.writeEndArray();
        }
    }

    private static void writeExpiresField(final long value, final JsonGenerator gen) {
        try {
            gen.writeNumberField("expires", value);
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void writePubkeyField(final byte[] value, final JsonGenerator gen) {
        if (value == null) {
            return;
        }
        try {
            gen.writeBinaryField("pubkey", value);
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

}
