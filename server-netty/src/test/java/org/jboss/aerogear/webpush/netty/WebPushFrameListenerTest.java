package org.jboss.aerogear.webpush.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.AsciiString;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2Connection.Endpoint;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2Stream;
import org.jboss.aerogear.webpush.AggregateSubscription;
import org.jboss.aerogear.webpush.AggregateSubscription.Entry;
import org.jboss.aerogear.webpush.DefaultAggregateSubscription;
import org.jboss.aerogear.webpush.Subscription;
import org.jboss.aerogear.webpush.DefaultAggregateSubscription.DefaultEntry;
import org.jboss.aerogear.webpush.Registration.Resource;
import org.jboss.aerogear.webpush.Registration.WebLink;
import org.jboss.aerogear.webpush.WebPushServer;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.OngoingStubbing;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static io.netty.buffer.Unpooled.copiedBuffer;
import static io.netty.handler.codec.http.HttpHeaderNames.CACHE_CONTROL;
import static io.netty.handler.codec.http.HttpHeaderNames.LOCATION;
import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.util.CharsetUtil.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.*;
import static org.jboss.aerogear.webpush.JsonMapper.toJson;
import static org.jboss.aerogear.webpush.Registration.WebLink.AGGREGATE;
import static org.jboss.aerogear.webpush.Registration.WebLink.SUBSCRIBE;
import static org.jboss.aerogear.webpush.Registration.WebLink.REGISTRATION;
import static org.jboss.aerogear.webpush.netty.WebPushFrameListener.LINK;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WebPushFrameListenerTest {

    @Test (expected = NullPointerException.class)
    public void withNullWebPushServer() {
        new WebPushFrameListener(null);
    }

    @Test
    public void register() throws Exception {
        final String regId = "9999";
        final ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        final WebPushFrameListener frameListener = new WebPushFrameListener(MockWebPushServerBuilder
                .withRegistrationid(regId)
                .registrationMaxAge(10000L)
                .build());
        final Http2ConnectionEncoder encoder = mockEncoder(w -> w.thenReturn(registerPath(regId)));
        frameListener.encoder(encoder);

        final Http2Headers responseHeaders = register(frameListener, ctx, encoder);
        assertThat(responseHeaders.status(), equalTo(OK.codeAsText()));
        assertThat(responseHeaders.get(LOCATION), equalTo(asciiString(registrationPath(regId))));
        assertThat(responseHeaders.getAll(LINK), hasItems(
                asciiString(REGISTRATION.weblink(registrationPath(regId))),
                asciiString(SUBSCRIBE.weblink(subscribePath(regId))),
                asciiString(AGGREGATE.weblink(aggregatePath(regId)))));
        assertThat(responseHeaders.get(CACHE_CONTROL), equalTo(asciiString("private, max-age=10000")));
        frameListener.shutdown();
    }

    @Test
    public void subscribe() throws Exception {
        final String regId = "9999";
        final String endpoint = "endpoint1";
        final String subscriptionId = "sub1";
        final ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        final WebPushFrameListener frameListener = new WebPushFrameListener(MockWebPushServerBuilder
                .withRegistrationid(regId)
                .registrationMaxAge(10000L)
                .subscriptionMaxAge(50000L)
                .addSubscription(mockSubscription(regId, subscriptionId, endpoint))
                .build());
        final Http2ConnectionEncoder encoder = mockEncoder(w -> w.thenReturn(registerPath(regId))
                .thenReturn(subscribePath(regId)));
        frameListener.encoder(encoder);

        final Http2Headers regHeaders = register(frameListener, ctx, encoder);
        final Http2Headers subHeaders = subscribe(frameListener, ctx, regHeaders, encoder);
        assertThat(subHeaders.status(), equalTo(CREATED.codeAsText()));
        assertThat(subHeaders.get(LOCATION), equalTo(asciiString(webpushPath(endpoint))));
        assertThat(subHeaders.get(CACHE_CONTROL), equalTo(asciiString("private, max-age=50000")));
        frameListener.shutdown();
    }

    private static Subscription mockSubscription(final String regId, final String id, final String endpoint) {
        final Subscription subscription = mock(Subscription.class);
        when(subscription.endpoint()).thenReturn(endpoint);
        when(subscription.id()).thenReturn(id);
        when(subscription.registrationId()).thenReturn(regId);
        when(subscription.message()).thenReturn(Optional.empty());
        return subscription;
    }

    private static Subscription mockSubscription(final String regId,
                                                 final String id,
                                                 final String endpoint,
                                                 final String message) {
        final Subscription subscription = mockSubscription(regId, id, endpoint);
        when(subscription.message()).thenReturn(Optional.of(message));
        return subscription;
    }

    @Test
    public void statusNoMessage() throws Exception {
        final String regId = "9999";
        final String endpoint = "endpoint1";
        final String subId = "sub1";
        final String message = "message1";
        final ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        final Subscription subscription = mockSubscription(regId, subId, endpoint, message);
        when(subscription.message()).thenReturn(Optional.of(message)).thenReturn(Optional.<String>empty());
        final WebPushFrameListener frameListener = new WebPushFrameListener(MockWebPushServerBuilder
                .withRegistrationid(regId)
                .registrationMaxAge(10000L)
                .subscriptionMaxAge(50000L)
                .addSubscription(subscription)
                .build());
        final Http2ConnectionEncoder encoder = mockEncoder(w -> w.thenReturn(registerPath(regId))
                .thenReturn(subscribePath(regId)));
        frameListener.encoder(encoder);

        final Http2Headers regHeaders = register(frameListener, ctx, encoder);
        final Http2Headers subHeaders = subscribe(frameListener, ctx, regHeaders, encoder);
        assertThat(subHeaders.status(), equalTo(CREATED.codeAsText()));
        final Http2Headers statusHeaders = status(frameListener, ctx, subHeaders, encoder,
                false);
        assertThat(statusHeaders.status(), equalTo(OK.codeAsText()));
        final ByteBuf byteBuf = verifyAndCaptureData(ctx, encoder);
        assertThat(byteBuf.toString(UTF_8), equalTo(message));
        final Http2Headers statusHeaders2 = status(frameListener, ctx, subHeaders, encoder,
                true);
        assertThat(statusHeaders2.status(), equalTo(NO_CONTENT.codeAsText()));
        frameListener.shutdown();
    }

    @Test
    public void statusMessage() throws Exception {
        final String regId = "9999";
        final String endpoint = "endpoint1";
        final String subId = "sub1";
        final String message = "message1";
        final ByteBuf data = copiedBuffer("message1", UTF_8);
        final ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        final Subscription subscription = mockSubscription(regId, subId, endpoint, message);
        when(subscription.message()).thenReturn(Optional.of(message)).thenReturn(Optional.<String>empty());
        final WebPushFrameListener frameListener = new WebPushFrameListener(MockWebPushServerBuilder
                .withRegistrationid(regId)
                .registrationMaxAge(10000L)
                .subscriptionMaxAge(50000L)
                .addSubscription(subscription)
                .build());
        final Http2ConnectionEncoder encoder = mockEncoder(w -> w.thenReturn(registerPath(regId))
                .thenReturn(subscribePath(regId)));
        frameListener.encoder(encoder);

        final Http2Headers regHeaders = register(frameListener, ctx, encoder);
        final Http2Headers subHeaders = subscribe(frameListener, ctx, regHeaders, encoder);
        assertThat(subHeaders.status(), equalTo(CREATED.codeAsText()));
        notify(frameListener, ctx, encoder, subHeaders, data);

        final Http2Headers statusHeaders = status(frameListener, ctx, subHeaders, encoder,
                false);
        assertThat(statusHeaders.status(), equalTo(OK.codeAsText()));
        final ByteBuf byteBuf = verifyAndCaptureData(ctx, encoder);
        assertThat(byteBuf.toString(UTF_8), equalTo(message));
        final Http2Headers statusHeaders2 = status(frameListener, ctx, subHeaders, encoder,
                true);
        assertThat(statusHeaders2.status(), equalTo(NO_CONTENT.codeAsText()));
        frameListener.shutdown();
    }

    @Test
    public void deleteSubscription() throws Exception {
        final String regId = "9999";
        final String endpoint = "endpoint1";
        final String subId = "sub1";
        final ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        final WebPushServer webPushServer = MockWebPushServerBuilder
                .withRegistrationid(regId)
                .registrationMaxAge(10000L)
                .subscriptionMaxAge(50000L)
                .addSubscription(mockSubscription(regId, subId, endpoint))
                .build();
        final WebPushFrameListener frameListener = new WebPushFrameListener(webPushServer);
        final Http2ConnectionEncoder encoder = mockEncoder(w -> w.thenReturn(registerPath(regId))
                .thenReturn(subscribePath(regId))
                .thenReturn(registrationPath(regId)));
        frameListener.encoder(encoder);

        final Http2Headers regHeaders = register(frameListener, ctx, encoder);
        final Http2Headers subHeaders = subscribe(frameListener, ctx, regHeaders, encoder);
        assertThat(subHeaders.status(), equalTo(CREATED.codeAsText()));

        final Http2Headers delHeaders = deleteSubscription(frameListener, ctx, subHeaders, encoder);
        assertThat(delHeaders.status(), equalTo(OK.codeAsText()));

        when(webPushServer.subscription(endpoint)).thenReturn(Optional.empty());
        final Http2Headers statusHeaders = status(frameListener, ctx, subHeaders, encoder,
                true);
        assertThat(statusHeaders.status(), equalTo(HttpResponseStatus.NOT_FOUND.codeAsText()));
        frameListener.shutdown();
    }

    @Test
    public void monitor() throws Exception {
        final String regId = "9999";
        final String subId = "sub1";
        final String endpoint = "endpoint1";
        final ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        final WebPushFrameListener frameListener = new WebPushFrameListener(MockWebPushServerBuilder
                .withRegistrationid(regId)
                .registrationMaxAge(10000L)
                .subscriptionMaxAge(50000L)
                .addSubscription(mockSubscription(regId, subId, endpoint))
                .build());
        final Http2ConnectionEncoder encoder = mockEncoder(w -> w.thenReturn(registerPath(regId))
                .thenReturn(registrationPath(regId)));
        frameListener.encoder(encoder);

        final Http2Headers regHeaders = register(frameListener, ctx, encoder);
        monitor(frameListener, ctx, regHeaders);
        final ArgumentCaptor<Http2Headers> captor = ArgumentCaptor.forClass(Http2Headers.class);
        verify(encoder).writePushPromise(eq(ctx), eq(3), eq(4), captor.capture(), eq(0),
                any(ChannelPromise.class));
        final Http2Headers monitorHeaders = captor.getValue();
        assertThat(monitorHeaders.status(), equalTo(OK.codeAsText()));
        assertThat(monitorHeaders.getAll(LINK), hasItems(
                asciiString(SUBSCRIBE.weblink(subscribePath(regId))),
                asciiString(AGGREGATE.weblink(aggregatePath(regId)))));
        assertThat(monitorHeaders.get(CACHE_CONTROL), equalTo(asciiString("private, max-age=10000")));
        frameListener.shutdown();
    }

    @Test
    public void monitorNow() throws Exception {
        final String message = "some payload";
        final ByteBuf data = copiedBuffer(message, UTF_8);
        final String regId = "9999";
        final String endpoint = "endpoint1";
        final String subId = "sub1";
        final int pushStreamId = 4;
        try {
            final Subscription subscription = mockSubscription(regId, subId, endpoint);
            when(subscription.message()).thenReturn(Optional.of(message)).thenReturn(Optional.of(message));

            final ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
            final WebPushServer webPushServer = MockWebPushServerBuilder.withRegistrationid(regId)
                    .registrationMaxAge(10000L)
                    .subscriptionMaxAge(50000L)
                    .addSubscription(subscription)
                    .build();
            when(webPushServer.subscription(endpoint)).thenReturn(Optional.of(subscription));
            final WebPushFrameListener frameListener = new WebPushFrameListener(webPushServer);

            final Http2ConnectionEncoder encoder = mockEncoder(w -> w.thenReturn(registerPath(regId))
                    .thenReturn(subscribePath(regId))
                    .thenReturn(webpushPath(endpoint, regId))
                    .thenReturn(registrationPath(regId)));
            frameListener.encoder(encoder);

            final Http2Headers regHeaders = register(frameListener, ctx, encoder);
            final Http2Headers subHeaders = subscribe(frameListener, ctx, regHeaders, encoder);
            assertThat(subHeaders.status(), equalTo(CREATED.codeAsText()));
            notify(frameListener, ctx, encoder, subHeaders, data);

            monitorWithWait(frameListener, ctx, regHeaders, 0);
            verify(encoder).writeData(eq(ctx), eq(pushStreamId), eq(data), eq(0), eq(false), any(ChannelPromise.class));
            verify(encoder).writePushPromise(eq(ctx), eq(3), eq(pushStreamId), any(Http2Headers.class), eq(0),
                    any(ChannelPromise.class));
            frameListener.shutdown();
        } finally {
            data.release();
        }
    }

    @Test
    public void aggregateChannelCreation() throws Exception {
        final String regId = "9999";
        final ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        final Subscription subscription1 = mockSubscription(regId, "sub1", "endpoint1", "message1");
        final Subscription subscription2 = mockSubscription(regId, "sub3", "endpoint2", "message2");
        final Subscription subscription3 = mockSubscription(regId, "sub3", "aggSub");
        final WebPushFrameListener frameListener = new WebPushFrameListener(MockWebPushServerBuilder
                .withRegistrationid(regId)
                .registrationMaxAge(10000L)
                .subscriptionMaxAge(50000L)
                .addSubscription(subscription1)
                .addSubscription(subscription2)
                .addSubscription(subscription3)
                .subscriptionOrder(w -> w.thenReturn(Optional.of(subscription1))
                        .thenReturn(Optional.of(subscription2))
                        .thenReturn(Optional.of(subscription3)))
                .build());
        final Http2ConnectionEncoder encoder = mockEncoder(w -> w.thenReturn(registerPath(regId))
                .thenReturn(subscribePath(regId))
                .thenReturn(subscribePath(regId))
                .thenReturn(aggregatePath(regId)));
        frameListener.encoder(encoder);
        final Http2Headers regHeaders = register(frameListener, ctx, encoder);
        final String endpoint1 = subscribe(frameListener, ctx, regHeaders, encoder).get(LOCATION).toString();
        final String endpoint2 = subscribe(frameListener, ctx, regHeaders, encoder).get(LOCATION).toString();
        final AggregateSubscription aggregateSubscription = asAggregateChannel(
                new DefaultEntry(endpoint1, Optional.of(5000L)),
                new DefaultEntry(endpoint2));
        aggregateSubscribe(frameListener, ctx, regHeaders, toJson(aggregateSubscription), encoder);
        final Http2Headers aggregateResponseHeaders = verifyAndCapture(ctx, encoder, true);
        assertThat(aggregateResponseHeaders.status(), equalTo(CREATED.codeAsText()));
        assertThat(aggregateResponseHeaders.get(LOCATION), equalTo(asciiString(webpushPath("aggSub"))));
        assertThat(aggregateResponseHeaders.get(CACHE_CONTROL), equalTo(asciiString("private, max-age=50000")));
        frameListener.shutdown();
    }

    @Test
    public void notification() throws Exception {
        final String regId = "9999";
        final String endpoint = "endpoint1";
        final String subId = "sub1";
        final String message = "message1";
        final ByteBuf data = copiedBuffer("some payload", UTF_8);
        final int pushStreamId = 4;
        try {
            final ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
            final WebPushFrameListener frameListener = new WebPushFrameListener(MockWebPushServerBuilder
                    .withRegistrationid(regId)
                    .registrationMaxAge(10000L)
                    .subscriptionMaxAge(50000L)
                    .addSubscription(mockSubscription(regId, subId, endpoint, message))
                    .build());
            final Http2ConnectionEncoder encoder = mockEncoder(w -> w.thenReturn(registerPath(regId))
                    .thenReturn(subscribePath(regId))
                    .thenReturn(endpointPath(endpoint)));
            frameListener.encoder(encoder);

            final Http2Headers regHeaders = register(frameListener, ctx, encoder);
            final Http2Headers subHeaders = subscribe(frameListener, ctx, regHeaders, encoder);
            assertThat(subHeaders.status(), equalTo(CREATED.codeAsText()));
            monitor(frameListener, ctx, regHeaders);
            verify(encoder).writePushPromise(eq(ctx), eq(3), eq(pushStreamId), any(Http2Headers.class), eq(0),
                    any(ChannelPromise.class));
            notify(frameListener, ctx, encoder, subHeaders, data);
            verify(encoder).writeData(eq(ctx), eq(pushStreamId), eq(data), eq(0), eq(false), any(ChannelPromise.class));
            frameListener.shutdown();
        } finally {
            data.release();
        }
    }

    @Test
    public void notificationStoreMessage() throws Exception {
        final String regId = "9999";
        final String endpoint = "endpoint1";
        final String subId = "sub1";
        final String message = "message1";
        final ByteBuf data = copiedBuffer("some payload", UTF_8);
        try {
            final ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
            final WebPushFrameListener frameListener = new WebPushFrameListener(MockWebPushServerBuilder
                    .withRegistrationid(regId)
                    .registrationMaxAge(10000L)
                    .subscriptionMaxAge(50000L)
                    .addSubscription(mockSubscription(regId, subId, endpoint, message))
                    .build());
            final Http2ConnectionEncoder encoder = mockEncoder(w -> w.thenReturn(registerPath(regId))
                    .thenReturn(subscribePath(regId))
                    .thenReturn(endpointPath(endpoint)));
            frameListener.encoder(encoder);

            final Http2Headers regHeaders = register(frameListener, ctx, encoder);
            final Http2Headers subHeaders = subscribe(frameListener, ctx, regHeaders, encoder);
            assertThat(subHeaders.status(), equalTo(CREATED.codeAsText()));
            final Http2Headers notifyHeaders = notify(frameListener, ctx, encoder, subHeaders, data);
            assertThat(notifyHeaders.status(), equalTo(HttpResponseStatus.ACCEPTED.codeAsText()));
            frameListener.shutdown();
        } finally {
            data.release();
        }
    }

    @Test
    public void aggregateChannelNotification() throws Exception {
        final String regId = "9999";
        final String subId = "aggSub";
        final ByteBuf data = copiedBuffer("aggregate payload", UTF_8);
        final Subscription subscription1 = mockSubscription(regId, "sub1", "endpoint1");
        final Subscription subscription2 = mockSubscription(regId, "sub2", "endpoint2");
        final Subscription subscription3 = mockSubscription(regId, "sub3", "aggSub");
        try {
            final int pushStreamId = 4;
            final ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
            final WebPushFrameListener frameListener = new WebPushFrameListener(MockWebPushServerBuilder
                    .withRegistrationid(regId)
                    .registrationMaxAge(10000L)
                    .subscriptionMaxAge(50000L)
                    .addSubscription(subscription1)
                    .addSubscription(subscription2)
                    .addSubscription(subscription3)
                    .subscriptionOrder(w -> w.thenReturn(Optional.of(subscription1))
                            .thenReturn(Optional.of(subscription2))
                            .thenReturn(Optional.of(subscription3)))
                    .build());
            final Http2ConnectionEncoder encoder = mockEncoder(w -> w.thenReturn(registerPath(regId))
                    .thenReturn(subscribePath(regId))
                    .thenReturn(subscribePath(regId))
                    .thenReturn(aggregatePath(regId))
                    .thenReturn(webpushPath(subId)));
            frameListener.encoder(encoder);

            final Http2Headers regHeaders = register(frameListener, ctx, encoder);
            final String endpoint1 = subscribe(frameListener, ctx, regHeaders, encoder).get(LOCATION).toString();
            final String endpoint2 = subscribe(frameListener, ctx, regHeaders, encoder).get(LOCATION).toString();
            final AggregateSubscription aggregateSubscription = asAggregateChannel(
                    new DefaultEntry(endpoint1, Optional.of(5000L)),
                    new DefaultEntry(endpoint2));
            final Http2Headers aggregateChannelHeaders = aggregateSubscribe(frameListener,
                    ctx,
                    regHeaders,
                    toJson(aggregateSubscription),
                    encoder);
            assertThat(aggregateChannelHeaders.get(LOCATION), equalTo(asciiString(webpushPath(subId))));
            monitor(frameListener, ctx, regHeaders);

            verify(encoder).writePushPromise(eq(ctx), eq(3), eq(pushStreamId), any(Http2Headers.class), eq(0),
                    any(ChannelPromise.class));

            notify(frameListener, ctx, encoder, aggregateChannelHeaders, data);
            verify(encoder, times(2)).writeData(eq(ctx), eq(pushStreamId), eq(data), eq(0), eq(false), any(ChannelPromise.class));
            frameListener.shutdown();
        } finally {
            data.release();
        }
    }

    private static Http2Headers register(final WebPushFrameListener frameListener,
                                         final ChannelHandlerContext ctx,
                                         final Http2ConnectionEncoder encoder) throws Http2Exception {
        frameListener.onHeadersRead(ctx, 3, registerHeaders(), 0, (short) 22, false, 0, true);
        final ByteBuf empty = Unpooled.buffer();
        frameListener.onDataRead(ctx, 3, empty, 0, true);
        return verifyAndCapture(ctx, encoder, true);
    }

    private static Http2Headers subscribe(final WebPushFrameListener frameListener,
                                          final ChannelHandlerContext ctx,
                                          final Http2Headers registrationHeaders,
                                          final Http2ConnectionEncoder encoder) throws Http2Exception {
        final AsciiString subUri = getLinkUri(asciiString(SUBSCRIBE), registrationHeaders.getAll(LINK));
        frameListener.onHeadersRead(ctx, 3, subHeaders(subUri), 0, (short) 22, false, 0, false);
        frameListener.onDataRead(ctx, 3, Unpooled.buffer(), 0, true);
        return verifyAndCapture(ctx, encoder, true);
    }

    private static Http2Headers aggregateSubscribe(final WebPushFrameListener frameListener,
                                                   final ChannelHandlerContext ctx,
                                                   final Http2Headers registrationHeaders,
                                                   final String json,
                                                   final Http2ConnectionEncoder encoder) throws Http2Exception {
        final AsciiString aggregateUri = getLinkUri(asciiString(AGGREGATE), registrationHeaders.getAll(LINK));
        frameListener.onHeadersRead(ctx, 3, subHeaders(aggregateUri), 0, (short) 22, false, 0, false);
        frameListener.onDataRead(ctx, 3, copiedBuffer(json, UTF_8), 0, true);
        return verifyAndCapture(ctx, encoder, true);
    }

    private static AsciiString getLinkUri(final AsciiString linkType, final List<AsciiString> links) {
        for (AsciiString link : links) {
            if (link.contains(linkType)) {
                return link.subSequence(1, link.indexOf(";")-1);
            }
        }
        throw new IllegalStateException("No link header of type " + linkType + " was found in " + links);
    }

    private static void monitor(final WebPushFrameListener frameListener,
                                        final ChannelHandlerContext ctx,
                                        final Http2Headers registrationHeaders) throws Http2Exception {
        final AsciiString location = registrationHeaders.get(LOCATION);
        frameListener.onHeadersRead(ctx, 3, monitorHeaders(location), 0, (short) 22, false, 0, true);
    }

    private static void monitorWithWait(final WebPushFrameListener frameListener,
                                final ChannelHandlerContext ctx,
                                final Http2Headers registrationHeaders,
                                final int wait) throws Http2Exception {
        final AsciiString location = registrationHeaders.get(LOCATION);
        final Http2Headers http2Headers = monitorHeaders(location);
        http2Headers.add(new AsciiString("prefer"), new AsciiString("wait=" + wait));
        frameListener.onHeadersRead(ctx, 3, http2Headers, 0, (short) 22, false, 0, true);
    }

    private static Http2Headers notify(final WebPushFrameListener frameListener,
                                final ChannelHandlerContext ctx,
                                final Http2ConnectionEncoder encoder,
                                final Http2Headers subHeaders,
                                final ByteBuf data) throws Http2Exception {
        final AsciiString location = subHeaders.get(LOCATION);
        frameListener.onHeadersRead(ctx, 3, notifyHeaders(location), 0, (short) 22, false, 0, false);
        frameListener.onDataRead(ctx, 3, data, 0, true);
        return verifyAndCapture(ctx, encoder, true);
    }

    private static Http2Headers status(final WebPushFrameListener frameListener,
                                       final ChannelHandlerContext ctx,
                                       final Http2Headers subHeaders,
                                       final Http2ConnectionEncoder encoder,
                                       final boolean endstream) throws Http2Exception {
        final AsciiString location = subHeaders.get(LOCATION);
        frameListener.onHeadersRead(ctx, 3, statusHeaders(location), 0, (short) 22, false, 0, false);
        return verifyAndCapture(ctx, encoder, endstream);
    }

    private static Http2Headers deleteSubscription(final WebPushFrameListener frameListener,
                                                   final ChannelHandlerContext ctx,
                                                   final Http2Headers subHeaders,
                                                   final Http2ConnectionEncoder encoder) throws Http2Exception {
        final AsciiString location = subHeaders.get(LOCATION);
        frameListener.onHeadersRead(ctx, 3, subDeleteHeaders(location), 0, (short) 22, false, 0, false);
        return verifyAndCapture(ctx, encoder, true);
    }

    private static Http2Headers verifyAndCapture(final ChannelHandlerContext ctx,
                                                 final Http2ConnectionEncoder encoder,
                                                 final boolean endstream) {
        final ArgumentCaptor<Http2Headers> captor = ArgumentCaptor.forClass(Http2Headers.class);
        verify(encoder, atLeastOnce()).writeHeaders(eq(ctx), eq(3), captor.capture(), eq(0), eq(endstream),
                any(ChannelPromise.class));
        return captor.getValue();
    }

    private static ByteBuf verifyAndCaptureData(final ChannelHandlerContext ctx,
                                                 final Http2ConnectionEncoder encoder) {
        final ArgumentCaptor<ByteBuf> captor = ArgumentCaptor.forClass(ByteBuf.class);
        verify(encoder, atLeastOnce()).writeData(eq(ctx), eq(3), captor.capture(), eq(0), eq(false),
                any(ChannelPromise.class));
        return captor.getValue();
    }

    private static AsciiString asciiString(final String str) {
        return new AsciiString(str);
    }

    private static AsciiString asciiString(final WebLink type) {
        return new AsciiString(type.toString());
    }

    private static Http2Headers registerHeaders() {
        final Http2Headers requestHeaders = new DefaultHttp2Headers(false);
        requestHeaders.method(HttpMethod.POST.name());
        requestHeaders.path(asciiString("/webpush/" + Resource.REGISTER.resourceName()));
        return requestHeaders;
    }

    private static Http2Headers subHeaders(final AsciiString resourceUrl) {
        final Http2Headers requestHeaders = new DefaultHttp2Headers(false);
        requestHeaders.method(HttpMethod.POST.name());
        requestHeaders.path(resourceUrl);
        return requestHeaders;
    }

    private static Http2Headers monitorHeaders(final AsciiString resourceUrl) {
        final Http2Headers requestHeaders = new DefaultHttp2Headers(false);
        requestHeaders.method(HttpMethod.GET.name());
        requestHeaders.path(resourceUrl);
        return requestHeaders;
    }

    private static Http2Headers notifyHeaders(final AsciiString resourceUrl) {
        final Http2Headers requestHeaders = new DefaultHttp2Headers(false);
        requestHeaders.method(HttpMethod.PUT.name());
        requestHeaders.path(resourceUrl);
        return requestHeaders;
    }

    private static Http2Headers statusHeaders(final AsciiString resourceUrl) {
        final Http2Headers requestHeaders = new DefaultHttp2Headers(false);
        requestHeaders.method(HttpMethod.GET.name());
        requestHeaders.path(resourceUrl);
        return requestHeaders;
    }

    private static Http2Headers subDeleteHeaders(final AsciiString resourceUrl) {
        final Http2Headers requestHeaders = new DefaultHttp2Headers(false);
        requestHeaders.method(HttpMethod.DELETE.name());
        requestHeaders.path(resourceUrl);
        return requestHeaders;
    }

    private static Http2ConnectionEncoder mockEncoder(final Consumer<OngoingStubbing<String>> consumer) {
        final Http2ConnectionEncoder encoder = mock(Http2ConnectionEncoder.class);
        final Http2Connection connection = mock(Http2Connection.class);
        final Endpoint local = mock(Endpoint.class);
        final Http2Stream stream = mock(Http2Stream.class);
        when(local.nextStreamId()).thenReturn(4);
        consumer.accept(when(stream.getProperty("webpush.path")));
        when(connection.local()).thenReturn(local);
        when(connection.stream(anyInt())).thenReturn(stream);
        when(encoder.connection()).thenReturn(connection);
        final ChannelFuture channelFuture = mock(ChannelFuture.class);
        when(channelFuture.isSuccess()).thenReturn(true);
        when(encoder.writeHeaders(any(ChannelHandlerContext.class), anyInt(), any(Http2Headers.class), anyInt(),
                anyBoolean(), any(ChannelPromise.class))).thenReturn(channelFuture);
        when(encoder.writeData(any(ChannelHandlerContext.class), anyInt(), any(ByteBuf.class), anyInt(),
                anyBoolean(), any(ChannelPromise.class))).thenReturn(channelFuture);
        return encoder;
    }

    private static AggregateSubscription asAggregateChannel(final Entry... entries) {
        return new DefaultAggregateSubscription(new LinkedHashSet<>(Arrays.asList(entries)));
    }

    private static String registrationPath(final String registrationId) {
        return webpushPath(Resource.REGISTRATION.resourceName(), registrationId);
    }

    private static String subscribePath(final String registrationId) {
        return webpushPath(Resource.SUBSCRIBE.resourceName(), registrationId);
    }

    private static String aggregatePath(final String registrationId) {
        return webpushPath(Resource.AGGREGATE.resourceName(), registrationId);
    }

    private static String registerPath(final String registrationId) {
        return webpushPath(Resource.REGISTER.resourceName(), registrationId);
    }

    private static String webpushPath(final String path, final String registrationId) {
        return webpushPath(path + "/" + registrationId);
    }

    private static String endpointPath(final String path) {
        return "/" + path;
    }

    private static String webpushPath(final String path) {
        return "/webpush/" + path;
    }

}