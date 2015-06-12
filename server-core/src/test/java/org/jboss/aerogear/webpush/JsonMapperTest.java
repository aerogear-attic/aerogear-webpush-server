package org.jboss.aerogear.webpush;

import org.jboss.aerogear.webpush.AggregateSubscription.Entry;
import org.jboss.aerogear.webpush.DefaultAggregateSubscription.DefaultEntry;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class JsonMapperTest {

    @Test
    public void aggregateChannelToJson() {
        final Entry entry1 = new DefaultEntry("http://host/webpush/entry1", Optional.of(10L), Optional.of(new byte[1]));
        final Entry entry2 = new DefaultEntry("http://host/webpush/entry2", Optional.of(11L));
        final Entry entry3 = new DefaultEntry("http://host/webpush/entry3");
        final String json = JsonMapper.toJson(new DefaultAggregateSubscription(asSet(entry1, entry2, entry3)));
        assertAggregateChannel(JsonMapper.fromJson(json, AggregateSubscription.class));
    }

    @Test
    public void aggregateChannelFromJson() {
        final String json = "[{\"http://host/webpush/entry1\":{\"expires\":10,\"pubkey\":\"AA==\"}},{\"http://host/webpush/entry2\":{\"expires\":11}},{\"http://host/webpush/entry3\":{\"expires\":0}}]";
        assertAggregateChannel(JsonMapper.fromJson(json, AggregateSubscription.class));
    }

    private static void assertAggregateChannel(final AggregateSubscription aggregate) {
        assertThat(aggregate.subscriptions().size(), is(3));
        assertThat(aggregate.subscriptions(), hasItems(
                new DefaultEntry("http://host/webpush/entry1", Optional.of(10L), Optional.of(new byte[1])),
                new DefaultEntry("http://host/webpush/entry2", Optional.of(11L)),
                new DefaultEntry("http://host/webpush/entry3")));
    }

    private static Set<Entry> asSet(final AggregateSubscription.Entry... entries) {
        return new LinkedHashSet<>(Arrays.asList(entries));
    }
}