package org.jboss.aerogear.webpush;

import java.net.URI;

public class DefaultRegistration implements Registration {

    private final String id;
    private final URI monitorURI;
    private final URI channelURI;

    public DefaultRegistration(final String id, final URI monitorURI, final URI channelURI) {
        this.id = id;
        this.monitorURI = monitorURI;
        this.channelURI = channelURI;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public URI monitorURI() {
        return monitorURI;
    }

    @Override
    public URI channelURI() {
        return channelURI;
    }

    @Override
    public String toString() {
        return "DefautRegistration[id=" + id + ", monitorURI=" + monitorURI + ", channelURI=" + channelURI + "]";
    }
}
