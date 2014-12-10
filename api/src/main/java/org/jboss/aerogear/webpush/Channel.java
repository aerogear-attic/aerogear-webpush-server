package org.jboss.aerogear.webpush;

import java.util.Optional;

public interface Channel {

    /**
     * The registration that this Channel belongs to.
     *
     * @return {@link String} the registration id that this channel belongs to.
     */
    String registrationId();

    /**
     * Returns the channelId for this channel.
     *
     * @return {@code String} this channels identifier.
     */
    String channelId();

    /**
     * Returns the endpoint token for this channel.
     *
     * This is the endpoint token that an encrypted hash of the registration id, a ':', and the channel id.
     *
     * @return {@code String} the endpoint token.
     */
    String endpointToken();

    /**
     * The notification/message for this channelURI.
     *
     * @return {@link Optional} the notification/message for this channelURI.
     */
    Optional<String> message();

}
