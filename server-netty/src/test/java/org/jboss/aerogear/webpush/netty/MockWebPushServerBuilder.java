package org.jboss.aerogear.webpush.netty;

import org.jboss.aerogear.webpush.Subscription;
import org.jboss.aerogear.webpush.Registration;
import org.jboss.aerogear.webpush.Registration.Resource;
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
        when(registration.uri()).thenReturn(asURI(context, Resource.REGISTRATION.resourceName(), id));
        when(registration.subscribeUri()).thenReturn(asURI(context, Resource.SUBSCRIBE.resourceName(), id));
        when(registration.aggregateUri()).thenReturn(asURI(context, Resource.AGGREGATE.resourceName(), id));
    }

    public MockWebPushServerBuilder registrationMaxAge(final long maxAge) {
        when(config.registrationMaxAge()).thenReturn(maxAge);
        return this;
    }

    public MockWebPushServerBuilder subscriptionMaxAge(final long maxAge) {
        when(config.subscriptionMaxAge()).thenReturn(maxAge);
        return this;
    }

    public MockWebPushServerBuilder addSubscription(final Subscription subscription) {
        when(webPushServer.subscription(subscription.endpoint())).thenReturn(Optional.of(subscription));
        when(webPushServer.newSubscription(registrationId)).thenReturn(Optional.of(subscription));
        return this;
    }

    public MockWebPushServerBuilder subscriptionOrder(final Consumer<OngoingStubbing<Optional<Subscription>>> consumer) {
        consumer.accept(when(webPushServer.newSubscription(registrationId)));
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

    private static URI asURI(final String context, final String resource, final String id) {
        try {
            return new URI(context + "/" + resource + "/" + id);
        } catch (final URISyntaxException e) {
            throw new RuntimeException("String [" + resource + " is not a valid URI", e);
        }
    }
}
