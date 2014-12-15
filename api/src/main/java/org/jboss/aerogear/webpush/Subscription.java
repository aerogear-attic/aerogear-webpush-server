package org.jboss.aerogear.webpush;

import java.util.Optional;

public interface Subscription {

    /**
     * Returns the id for this subscription.
     *
     * @return {@code String} this subscriptions identifier
     */
    String id();

    /**
     * The registration that this subscription belongs to.
     *
     * @return {@link String} the registration id that this subscription belongs to
     */
    String registrationId();

    /**
     * Returns the endpoint token for this subscription.
     *
     * This is the endpoint token that an encrypted hash of the registration id, a ':', and the subscription id.
     *
     * @return {@code String} the endpoint token
     */
    String endpoint();

    /**
     * The notification/message for this subscription.
     *
     * @return {@link Optional} the notification/message for this subscription
     */
    Optional<String> message();

}
