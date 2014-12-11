package org.jboss.aerogear.webpush.netty;

import org.jboss.aerogear.webpush.Channel;
import org.jboss.aerogear.webpush.Registration;
import org.jboss.aerogear.webpush.WebPushServer;
import org.jboss.aerogear.webpush.WebPushServerConfig;
import org.mockito.stubbing.OngoingStubbing;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.function.Consumer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MockWebPushServerBuilder {

    private final WebPushServer webPushServer = mock(WebPushServer.class);
    private final WebPushServerConfig config = mock(WebPushServerConfig.class);
    private final Registration registration = mock(Registration.class);
    private static final String context = "/webpush";
    private final String registrationId;

    private MockWebPushServerBuilder(final String registrationId) {
        this.registrationId = registrationId;
        when(registration.id()).thenReturn(registrationId);
        setRegistrationUrls(registrationId);
    }

    private void setRegistrationUrls(final String id) {
        when(registration.monitorUri()).thenReturn(asURI(context, id, "monitor"));
        when(registration.channelUri()).thenReturn(asURI(context, id, "channel"));
        when(registration.aggregateUri()).thenReturn(asURI(context, id, "aggregate"));
    }

    public MockWebPushServerBuilder registrationMaxAge(final long maxAge) {
        when(config.registrationMaxAge()).thenReturn(maxAge);
        return this;
    }

    public MockWebPushServerBuilder channelMaxAge(final long maxAge) {
        when(config.channelMaxAge()).thenReturn(maxAge);
        return this;
    }

    public MockWebPushServerBuilder addChannel(final Channel channel) {
        when(webPushServer.getChannel(channel.endpointToken())).thenReturn(Optional.of(channel));
        when(webPushServer.newChannel(registrationId)).thenReturn(Optional.of(channel));
        return this;
    }

    public MockWebPushServerBuilder channelOrder(final Consumer<OngoingStubbing<Optional<Channel>>> consumer) {
        consumer.accept(when(webPushServer.newChannel(registrationId)));
        return this;
    }

    public WebPushServer build() throws Exception {
        when(webPushServer.config()).thenReturn(config);
        when(webPushServer.register()).thenReturn(registration);
        when(webPushServer.registration(registrationId)).thenReturn(Optional.of(registration));
        return webPushServer;
    }

    public static MockWebPushServerBuilder withRegistrationid(final String id) {
        return new MockWebPushServerBuilder(id);
    }

    private static URI asURI(final String context, final String id, final String path) {
        try {
            return new URI(context + "/" + id + "/" + path);
        } catch (final URISyntaxException e) {
            throw new RuntimeException("String [" + path + " is not a valid URI", e);
        }
    }
}
