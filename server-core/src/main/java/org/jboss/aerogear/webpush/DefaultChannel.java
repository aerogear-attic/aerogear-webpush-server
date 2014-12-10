package org.jboss.aerogear.webpush;

import java.util.Objects;
import java.util.Optional;

public class DefaultChannel implements Channel {

    private final String regstrationId;
    private final Optional<String> message;
    private final String channelId;
    private final String endpointToken;

    public DefaultChannel(final String regstrationId, final String channelId, final String endpointToken) {
        this(regstrationId, channelId, endpointToken, Optional.empty());
    }

    public DefaultChannel(final String regstrationId,
                          final String channelId,
                          String endpointToken,
                          final Optional<String> message) {
        Objects.requireNonNull(regstrationId, "registrationId must not be null");
        Objects.requireNonNull(channelId, "channelId must not be null");
        Objects.requireNonNull(endpointToken, "endpointToken must not be null");
        Objects.requireNonNull(message, "message must not be null");
        this.channelId = channelId;
        this.endpointToken = endpointToken;
        this.regstrationId = regstrationId;
        this.message = message;
    }

    @Override
    public String registrationId() {
        return regstrationId;
    }

    @Override
    public String channelId() {
        return channelId;
    }

    @Override
    public String endpointToken() {
        return endpointToken;
    }

    @Override
    public Optional<String> message() {
        return message;
    }

    @Override
    public String toString() {
        return "DefaultChannel[registrationId=" + regstrationId +
                ", channelId=" + channelId +
                ", endpointToken=" + endpointToken +
                ", message=" + message + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DefaultChannel that = (DefaultChannel) o;

        if (!channelId.equals(that.channelId)) return false;
        if (!endpointToken.equals(that.endpointToken)) return false;
        if (!regstrationId.equals(that.regstrationId)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = regstrationId.hashCode();
        result = 31 * result + channelId.hashCode();
        result = 31 * result + endpointToken.hashCode();
        return result;
    }
}
