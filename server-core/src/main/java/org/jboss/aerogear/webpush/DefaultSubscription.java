package org.jboss.aerogear.webpush;

import java.util.Objects;
import java.util.Optional;

public class DefaultSubscription implements Subscription {

    private final String regstrationId;
    private final Optional<String> message;
    private final String subscriptionId;
    private final String endpoint;

    public DefaultSubscription(final String regstrationId, final String subscriptionId, final String endpoint) {
        this(regstrationId, subscriptionId, endpoint, Optional.empty());
    }

    public DefaultSubscription(final String regstrationId,
                               final String subscriptionId,
                               String endpoint,
                               final Optional<String> message) {
        Objects.requireNonNull(regstrationId, "registrationId must not be null");
        Objects.requireNonNull(subscriptionId, "subscriptionId must not be null");
        Objects.requireNonNull(endpoint, "endpoint must not be null");
        Objects.requireNonNull(message, "message must not be null");
        this.subscriptionId = subscriptionId;
        this.endpoint = endpoint;
        this.regstrationId = regstrationId;
        this.message = message;
    }

    @Override
    public String registrationId() {
        return regstrationId;
    }

    @Override
    public String id() {
        return subscriptionId;
    }

    @Override
    public String endpoint() {
        return endpoint;
    }

    @Override
    public Optional<String> message() {
        return message;
    }

    @Override
    public String toString() {
        return "DefaultChannel[registrationId=" + regstrationId +
                ", id=" + subscriptionId +
                ", endpoint=" + endpoint +
                ", message=" + message + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DefaultSubscription that = (DefaultSubscription) o;

        if (!subscriptionId.equals(that.subscriptionId)) return false;
        if (!endpoint.equals(that.endpoint)) return false;
        if (!regstrationId.equals(that.regstrationId)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = regstrationId.hashCode();
        result = 31 * result + subscriptionId.hashCode();
        result = 31 * result + endpoint.hashCode();
        return result;
    }
}
