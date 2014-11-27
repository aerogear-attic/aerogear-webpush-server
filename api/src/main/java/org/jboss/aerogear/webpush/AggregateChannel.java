package org.jboss.aerogear.webpush;

import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;

public interface AggregateChannel {

    Set<Entry> channels();

    interface Entry {
        /**
         * The endpoint for the channel of this aggregate.
         *
         * @return {@code String} the endpoint of the channel of this aggregate.
         */
        String endpoint();

        /**
         * Determines when the provided channel becomes invalid.
         * <p>
         * When this time expires this channel must be removed from the
         * aggregate.
         *
         * @return {@code long} the expires data, or 0 if channel never expires.
         */
        Optional<Long> expires();

        /**
         * The public key used for encrypting on this channel.
         *
         * @return {@code byte[]} the public key.
         */
        Optional<byte[]> pubkey();
    }
}
