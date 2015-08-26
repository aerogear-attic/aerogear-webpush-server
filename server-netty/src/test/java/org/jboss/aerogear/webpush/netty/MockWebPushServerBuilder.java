package org.jboss.aerogear.webpush.netty;

import org.jboss.aerogear.webpush.PushMessage;
import org.jboss.aerogear.webpush.Subscription;
import org.jboss.aerogear.webpush.WebPushServer;
import org.jboss.aerogear.webpush.WebPushServerConfig;
import org.mockito.stubbing.OngoingStubbing;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MockWebPushServerBuilder {

    private final WebPushServer webPushServer = mock(WebPushServer.class);
    private final WebPushServerConfig config = mock(WebPushServerConfig.class);
    private final Subscription subscription;
    private OngoingStubbing<String> tokens;

    private MockWebPushServerBuilder(final Subscription subscription) {
        this.subscription = subscription;
        when(webPushServer.subscribe()).thenReturn(subscription);
        when(webPushServer.subscriptionById(subscription.id())).thenReturn(Optional.of(subscription));
        when(config.messageMaxSize()).thenReturn(4096L);
        when(webPushServer.generateEndpointToken(eq(subscription.pushResourceId()), eq(subscription.id())))
                .thenReturn(subscription.pushResourceId());
    }

    public MockWebPushServerBuilder subscriptionMaxAge(final long maxAge) {
        when(config.subscriptionMaxAge()).thenReturn(maxAge);
        return this;
    }

    public MockWebPushServerBuilder waitingPushMessage(final PushMessage message) {
        when(webPushServer.waitingDeliveryMessages(subscription.id())).thenReturn(Collections.singletonList(message))
                .thenReturn(Collections.emptyList());
        return this;
    }

    public MockWebPushServerBuilder messageMaxSize(final long maxSize) {
        when(config.messageMaxSize()).thenReturn(maxSize);
        return this;
    }

    public MockWebPushServerBuilder receiptsToken(final String token) {
        when(webPushServer.generateEndpointToken(subscription.id())).thenReturn(token);
        when(webPushServer.subscriptionByToken(eq(token))).thenReturn(Optional.of(subscription));
        when(webPushServer.subscriptionByReceiptToken(token)).thenReturn(Optional.of(subscription));
        return this;
    }

    public MockWebPushServerBuilder receiptToken(final String token) {
        when(webPushServer.subscriptionByReceiptToken(eq(token))).thenReturn(Optional.of(subscription));
        when(webPushServer.generateEndpointToken(anyString(), eq(subscription.id()))).thenReturn(token);
        return this;
    }

    public MockWebPushServerBuilder receiptToken(final String token, final PushMessage pushMessage) {
        receiptToken(token);
        when(webPushServer.sentMessage(token)).thenReturn(Optional.of(pushMessage));
        return this;
    }

    public MockWebPushServerBuilder pushResourceToken(final String token) {
        when(webPushServer.generateEndpointToken(eq(token), eq(subscription.id()))).thenReturn(token);
        when(webPushServer.subscriptionByPushToken(token)).thenReturn(Optional.of(subscription));
        return this;
    }

    public MockWebPushServerBuilder nonexistentPushResourceToken(final String token) {
        when(webPushServer.subscriptionByPushToken(token)).thenReturn(Optional.empty());
        return this;
    }

    public MockWebPushServerBuilder pushMessageToken(final String token) {
        when(webPushServer.generateEndpointToken(anyString(), eq(subscription.id()))).thenReturn(token);
        return this;
    }

    public WebPushServer build() throws Exception {
        when(webPushServer.config()).thenReturn(config);
        return webPushServer;
    }

    public static MockWebPushServerBuilder withSubscription(final Subscription subscription) {
        return new MockWebPushServerBuilder(subscription);
    }

}
