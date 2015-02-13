package org.jboss.aerogear.webpush;

import org.jboss.aerogear.webpush.util.Arguments;

import java.util.HashSet;
import java.util.Set;

public class AggregateSubscription {

    interface Entry {
        /**
         * The endpoint for the subscription
         *
         * @return {@code String} the endpoint for the subscription
         */
        String endpoint();

        /**
         * Determines when the provided subscription becomes invalid.
         * <p>
         * When this time expires this subscription must be removed from the
         * aggregate.
         *
         * @return {@code long} the expires data, or 0 if subscription never expires.
         */
        long expires();

        /**
         * The public key used for encrypting on this subscription.
         *
         * @return {@code byte[]} the public key.
         */
        byte[] pubkey();
    }

    private final Set<Entry> subscriptions;

    public AggregateSubscription(final Set<Entry> subscriptions) {
        this.subscriptions = subscriptions;
    }

    public static AggregateSubscription from(final String channelsCsv) {
        return new AggregateSubscription(asEntries(channelsCsv.split(",")));
    }

    public Set<Entry> subscriptions() {
        return subscriptions;
    }

    @Override
    public String toString() {
        return "DefaulEntry[subscriptions=" + subscriptions + "]";
    }

    public static final class DefaultEntry implements Entry {

        private final String endpoint;
        private final Long expires;
        private final byte[] pubkey;

        public DefaultEntry(final String endpoint, final long expires, final byte[] pubkey) {
            Arguments.checkNotNull(endpoint, "endpoint must not be null");
            Arguments.checkNotNull(expires, "expires must not be null");
            this.endpoint = endpoint;
            this.expires = expires;
            this.pubkey = pubkey;
        }

        @Override
        public String endpoint() {
            return endpoint;
        }

        @Override
        public long expires() {
            return expires;
        }

        @Override
        public byte[] pubkey() {
            return pubkey;
        }

        @Override
        public String toString() {
            return "DefaulEntry[endpoint=" + endpoint + ", expires=" + expires + ", pubkey=" + pubkey + "]";
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final DefaultEntry that = (DefaultEntry) o;

            if (!endpoint.equals(that.endpoint)) {
                return false;
            }
            if (expires != that.expires) {
                return false;
            }
            if (pubkey != that.pubkey) {
                 return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int result = endpoint.hashCode();
            result = 31 * result + expires.hashCode();
            if (pubkey != null) {
                result = 31 * result + pubkey.hashCode();
            }
            return result;
        }
    }

    public static Set<Entry> asEntries(final String[] endpointUrls) {
        final Set<Entry> entries = new HashSet<>(endpointUrls.length);
        for (String url: endpointUrls) {
            entries.add(new DefaultEntry(url, 0L, null));
        }
        return entries;
    }
}
