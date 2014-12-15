package org.jboss.aerogear.webpush;

import java.net.URI;

public class DefaultRegistration implements Registration {

    private final String id;
    private final URI uri;
    private final URI subscribeUri;
    private final URI aggregateUri;

    public DefaultRegistration(final String id, final URI uri, final URI subscribeUri, final URI aggregateUri) {
        this.id = id;
        this.uri = uri;
        this.subscribeUri = subscribeUri;
        this.aggregateUri = aggregateUri;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public URI uri() {
        return uri;
    }

    @Override
    public URI subscribeUri() {
        return subscribeUri;
    }

    @Override
    public URI aggregateUri() {
        return aggregateUri;
    }

    @Override
    public String toString() {
        return "DefautRegistration[id=" + id +
                ", uri=" + uri +
                ", subscribeUri=" + subscribeUri +
                ", aggregateUri=" + aggregateUri + "]";
    }
}
