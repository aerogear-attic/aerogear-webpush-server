package org.jboss.aerogear.webpush;

import java.net.URI;

public class DefaultRegistration implements Registration {

    private final String id;
    private final URI monitorUri;
    private final URI channelUri;
    private final URI aggregateUri;

    public DefaultRegistration(final String id, final URI monitorUri, final URI channelUri, final URI aggregateUri) {
        this.id = id;
        this.monitorUri = monitorUri;
        this.channelUri = channelUri;
        this.aggregateUri = aggregateUri;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public URI monitorUri() {
        return monitorUri;
    }

    @Override
    public URI channelUri() {
        return channelUri;
    }

    @Override
    public URI aggregateUri() {
        return aggregateUri;
    }

    @Override
    public String toString() {
        return "DefautRegistration[id=" + id +
                ", monitorUri=" + monitorUri +
                ", channelUri=" + channelUri +
                ", aggregateUri=" + aggregateUri + "]";
    }
}
