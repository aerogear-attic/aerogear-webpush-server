package org.jboss.aerogear.webpush;

import java.net.URI;
import java.util.UUID;

/**
 * Represents a client registration in the WebPush protocol.
 */
public interface Registration {

    /**
     * A globally unique identifier for this registration.
     *
     * @return {@link UUID} the identifier for this registration.
     */
    String id();

    /**
     * The {@link URI} describing how a device is expected to monitor for incoming push messages.
     * <p>
     * The monitoring URI is used by a device to setup a push stream.
     *
     * @return {@link URI} which will be returned to the calling client, most often as HTTP Location Header value.
     */
    URI monitorURI();

    /**
     * The {@link URI} used by devices to create new channels
     *
     * @return {@link URI} to be used to create new channels.
     */
    URI channelURI();

}
