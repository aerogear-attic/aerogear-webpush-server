package org.jboss.aerogear.webpush;

import java.util.Optional;
import java.util.Set;

public interface AggregateSubscription {

    Set<Entry> subscriptions();

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
        Optional<Long> expires();

        /**
         * The public key used for encrypting on this subscription.
         *
         * @return {@code byte[]} the public key.
         */
        Optional<byte[]> pubkey();
    }
}
