package org.jboss.aerogear.webpush;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

import static org.jboss.aerogear.webpush.util.ArgumentUtil.checkNotNull;

public class DefaultAggregateChannel implements AggregateChannel {

    private final Set<Entry> channels;

    public DefaultAggregateChannel(final Set<Entry> channels) {
        this.channels = channels;
    }

    @Override
    public Set<Entry> channels() {
        return channels;
    }

    @Override
    public String toString() {
        return "DefaulEntry[channels=" + channels + "]";
    }

    public static final class DefaultEntry implements Entry {

        private final String endpoint;
        private final Optional<Long> expires;
        private final Optional<byte[]> pubkey;

        public DefaultEntry(final String endpoint) {
            this(endpoint, Optional.empty(), Optional.empty());
        }

        public DefaultEntry(final String endpoint, final Optional<Long> expires) {
            this(endpoint, expires, Optional.empty());
        }

        public DefaultEntry(final String endpoint, final Optional<Long> expires, final Optional<byte[]> pubkey) {
            checkNotNull(endpoint, "endpoint");
            checkNotNull(expires, "expires");
            checkNotNull(pubkey, "pubkey");
            this.endpoint = endpoint;
            this.expires = expires;
            this.pubkey = pubkey;
        }

        @Override
        public String endpoint() {
            return endpoint;
        }

        @Override
        public Optional<Long> expires() {
            return expires;
        }

        @Override
        public Optional<byte[]> pubkey() {
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

            if (!endpoint.equals(that.endpoint)) return false;
            if (expires.isPresent() && that.expires.isPresent()) {
                if (!(expires.get().equals(that.expires.get()))) return false;
            }
            if (pubkey.isPresent() && that.pubkey.isPresent()) {
                if (!Arrays.equals(pubkey.get(), that.pubkey.get())) return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = endpoint.hashCode();
            if (expires.isPresent()) {
                result = 31 * result + expires.hashCode();
            }
            if (pubkey.isPresent()) {
                result = 31 * result + pubkey.hashCode();
            }
            return result;
        }
    }
}
