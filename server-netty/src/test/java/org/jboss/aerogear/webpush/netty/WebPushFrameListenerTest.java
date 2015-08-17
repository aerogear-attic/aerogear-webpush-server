package org.jboss.aerogear.webpush.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2Connection.Endpoint;
import io.netty.handler.codec.http2.Http2Connection.PropertyKey;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2Stream;
import io.netty.util.AsciiString;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.ByteString;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.jboss.aerogear.webpush.DefaultSubscription;
import org.jboss.aerogear.webpush.Resource;
import org.jboss.aerogear.webpush.WebLink;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.OngoingStubbing;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static io.netty.buffer.Unpooled.copiedBuffer;
import static io.netty.handler.codec.http.HttpHeaderNames.CACHE_CONTROL;
import static io.netty.handler.codec.http.HttpHeaderNames.LOCATION;
import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.util.CharsetUtil.UTF_8;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.*;
import static org.jboss.aerogear.webpush.WebLink.PUSH;
import static org.jboss.aerogear.webpush.netty.WebPushFrameListener.LINK_HEADER;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WebPushFrameListenerTest {

    private static final String WEBPUSH_URI = "/webpush/";

    @Test (expected = NullPointerException.class)
    public void withNullWebPushServer() {
        new WebPushFrameListener(null);
    }

    @Test
    public void subscribe() throws Exception {
        final String subscriptionId = "9999";
        final String pushResourceId = "8888";
        final String pushToken = "p123";
        final String receiptToken = "r123";
        final ChannelHandlerContext ctx = mockChannelHandlerContext(subscriptionId);
        final WebPushFrameListener frameListener = new WebPushFrameListener(MockWebPushServerBuilder
                .withSubscription(new DefaultSubscription(subscriptionId, pushResourceId))
                .pushToken(pushToken)
                .receiptToken(receiptToken)
                .build());
        final Http2ConnectionEncoder encoder = mockEncoder(w -> w.thenReturn(subscribePath(subscriptionId)),
                Resource.SUBSCRIBE);
        frameListener.encoder(encoder);
        try {
            final Http2Headers responseHeaders = subscribe(frameListener, ctx, encoder);
            assertThat(responseHeaders.status(), equalTo(CREATED.codeAsText()));
            assertThat(responseHeaders.get(LOCATION), equalTo(subscriptionLocation(subscriptionId)));
            assertThat(responseHeaders.getAll(LINK_HEADER), hasItems(pushWebLink(pushToken), receiptWebLink(receiptToken)));
            assertThat(responseHeaders.get(CACHE_CONTROL), equalTo(asciiString("private, max-age=0")));
        } finally {
            frameListener.shutdown();
        }
    }

    @Test
    public void receivePushMessages() throws Exception {
        final String subscriptionId = "9999";
        final String pushResourceId = "8888";
        final ChannelHandlerContext ctx = mockChannelHandlerContext(subscriptionId);
        final WebPushFrameListener frameListener = new WebPushFrameListener(MockWebPushServerBuilder
                .withSubscription(new DefaultSubscription(subscriptionId, pushResourceId))
                .subscriptionMaxAge(10000L)
                .pushToken(pushResourceId)
                .build());
        final Http2ConnectionEncoder encoder = mockEncoder(w -> w.thenReturn(pushPath(pushResourceId))
                .thenReturn(pushPath(pushResourceId)), Resource.PUSH);
        frameListener.encoder(encoder);
        try {
            final Http2Headers subscribeHeaders = subscribe(frameListener, ctx, encoder);
            receivePushMessages(frameListener, ctx, subscribeHeaders);
            final ByteBuf payload = copiedBuffer("payload", UTF_8);
            notify(frameListener, ctx, encoder, subscribeHeaders, payload);
            verify(encoder).writePushPromise(any(ChannelHandlerContext.class), eq(3), eq(4), any(Http2Headers.class),
                    eq(0), any(ChannelPromise.class));
            final ArgumentCaptor<Http2Headers> captor = ArgumentCaptor.forClass(Http2Headers.class);
            verify(encoder).writeHeaders(any(ChannelHandlerContext.class), eq(4), captor.capture(), eq(0), eq(false),
                    any(ChannelPromise.class));
            final Http2Headers headers = captor.getValue();
            assertThat(headers.status(), equalTo(OK.codeAsText()));
            assertThat(headers.get(CACHE_CONTROL), equalTo(asciiString("private, max-age=10000")));
        } finally {
            frameListener.shutdown();
        }
    }

    @Test
    public void deleteSubscription() throws Exception {
        final String subscriptionId = "9999";
        final String pushResourceId = "8888";
        final String pushToken = "p123";
        final String receiptToken = "r123";
        final ChannelHandlerContext ctx = mockChannelHandlerContext(subscriptionId);
        final WebPushFrameListener frameListener = new WebPushFrameListener(MockWebPushServerBuilder
                .withSubscription(new DefaultSubscription(subscriptionId, pushResourceId))
                .pushToken(pushToken)
                .receiptToken(receiptToken)
                .build());
        final Http2ConnectionEncoder encoder = mockEncoder(w -> w.thenReturn(subscribePath(subscriptionId)),
                Resource.SUBSCRIBE);
        frameListener.encoder(encoder);
        try {
            final Http2Headers responseHeaders = subscribe(frameListener, ctx, encoder);
            assertThat(responseHeaders.status(), equalTo(CREATED.codeAsText()));
            assertThat(responseHeaders.get(LOCATION), equalTo(subscriptionLocation(subscriptionId)));
            assertThat(responseHeaders.getAll(LINK_HEADER), hasItems(pushWebLink(pushToken), receiptWebLink(receiptToken)));
            assertThat(responseHeaders.get(CACHE_CONTROL), equalTo(asciiString("private, max-age=0")));
        } finally {
            frameListener.shutdown();
        }
    }
    /*
    @Test
    public void deleteSubscription() throws Exception {
        final String subscriptionId = "9999";
        final String pushResourceId = "endpoint1";
        final String subId = "sub1";
        final ChannelHandlerContext ctx = mockChannelHandlerContext(subscriptionId);
        final WebPushServer webPushServer = MockWebPushServerBuilder
                .withSubscription(new DefaultSubscription(subscriptionId, pushResourceId))
                .registrationMaxAge(10000L)
                .subscriptionMaxAge(50000L)
                .build();
        final WebPushFrameListener frameListener = new WebPushFrameListener(webPushServer);
        final Http2ConnectionEncoder encoder = mockEncoder(w -> w.thenReturn(subscriptionPath(subscriptionId))
                .thenReturn(subscribePath(subscriptionId))
                .thenReturn(registrationPath(pushResourceId)));
        frameListener.encoder(encoder);

        final Http2Headers regHeaders = subscribe(frameListener, ctx, encoder);
        final Http2Headers subHeaders = subscribeOld(frameListener, ctx, regHeaders, encoder);
        assertThat(subHeaders.status(), equalTo(CREATED.codeAsText()));

        final Http2Headers delHeaders = deleteSubscription(frameListener, ctx, subHeaders, encoder);
        assertThat(delHeaders.status(), equalTo(OK.codeAsText()));

        when(webPushServer.subscription(subscriptionId)).thenReturn(Optional.empty());
        final Http2Headers statusHeaders = status(frameListener, ctx, subHeaders, encoder,
                true);
        assertThat(statusHeaders.status(), equalTo(HttpResponseStatus.NOT_FOUND.codeAsText()));
        frameListener.shutdown();
    }

    @Test
    public void monitor() throws Exception {
        final String subscriptionId = "9999";
        final String pushResourceId = "sub1";
        final String endpoint = "endpoint1";
        final ChannelHandlerContext ctx = mockChannelHandlerContext(subscriptionId);
        final WebPushFrameListener frameListener = new WebPushFrameListener(MockWebPushServerBuilder
                .withSubscription(new DefaultSubscription(subscriptionId, pushResourceId))
                .registrationMaxAge(10000L)
                .subscriptionMaxAge(50000L)
                .build());
        final Http2ConnectionEncoder encoder = mockEncoder(w -> w.thenReturn(subscriptionPath(subscriptionId))
                .thenReturn(registrationPath(pushResourceId)));
        frameListener.encoder(encoder);

        final Http2Headers regHeaders = subscribe(frameListener, ctx, encoder);
        receivePushMessages(frameListener, ctx, regHeaders);
        final ArgumentCaptor<Http2Headers> captor = ArgumentCaptor.forClass(Http2Headers.class);
        verify(encoder).writePushPromise(eq(ctx), eq(3), eq(4), captor.capture(), eq(0),
                any(ChannelPromise.class));
        final Http2Headers monitorHeaders = captor.getValue();
        assertThat(monitorHeaders.status(), equalTo(OK.codeAsText()));
        assertThat(monitorHeaders.getAll(LINK), hasItems(
                asciiString(SUBSCRIBE.weblink(subscribePath(subscriptionId))),
                asciiString(AGGREGATE.weblink(aggregatePath(subscriptionId)))));
        assertThat(monitorHeaders.get(CACHE_CONTROL), equalTo(asciiString("private, max-age=10000")));
        frameListener.shutdown();
    }

    @Test
    public void monitorNow() throws Exception {
        final String message = "some payload";
        final ByteBuf data = copiedBuffer(message, UTF_8);
        final String subscriptionId = "9999";
        final String pushResourceId = "endpoint1";
        final String subId = "sub1";
        final int pushStreamId = 4;
        try {
            final Subscription subscription = mockSubscription(subId, , endpoint);
            //when(subscription.messages()).thenReturn(Optional.of(message)).thenReturn(Optional.of(message));

            final ChannelHandlerContext ctx = mockChannelHandlerContext(regId);
            final WebPushServer webPushServer = MockWebPushServerBuilder
                    .withSubscription(regId)
                    .registrationMaxAge(10000L)
                    .subscriptionMaxAge(50000L)
                    .addSubscription(subscription)
                    .build();
            when(webPushServer.subscription(endpoint)).thenReturn(Optional.of(subscription));
            final WebPushFrameListener frameListener = new WebPushFrameListener(webPushServer);

            final Http2ConnectionEncoder encoder = mockEncoder(w -> w.thenReturn(subscriptionPath(regId))
                    .thenReturn(subscribePath(regId))
                    .thenReturn(webpushPath(endpoint, regId))
                    .thenReturn(registrationPath(regId)));
            frameListener.encoder(encoder);

            final Http2Headers regHeaders = subscribe(frameListener, ctx, encoder);
            final Http2Headers subHeaders = subscribeOld(frameListener, ctx, regHeaders, encoder);
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
    public void aggregateSubscription() throws Exception {
        final String regId = "9999";
        final ChannelHandlerContext ctx = mockChannelHandlerContext(regId);
        final Subscription subscription1 = mockSubscription(regId, "sub1", "endpoint1");
        final Subscription subscription2 = mockSubscription(regId, "sub3", "endpoint2");
        final Subscription subscription3 = mockSubscription(regId, "sub3", "aggSub");
        final WebPushFrameListener frameListener = new WebPushFrameListener(MockWebPushServerBuilder
                .withSubscriptionId(regId)
                .registrationMaxAge(10000L)
                .subscriptionMaxAge(50000L)
                .addSubscription(subscription1)
                .addSubscription(subscription2)
                .addSubscription(subscription3)
                .subscriptionOrder(w -> w.thenReturn(Optional.of(subscription1))
                        .thenReturn(Optional.of(subscription2))
                        .thenReturn(Optional.of(subscription3)))
                .build());
        final Http2ConnectionEncoder encoder = mockEncoder(w -> w.thenReturn(subscriptionPath(regId))
                .thenReturn(subscribePath(regId))
                .thenReturn(subscribePath(regId))
                .thenReturn(aggregatePath(regId)));
        frameListener.encoder(encoder);
        final Http2Headers regHeaders = subscribe(frameListener, ctx, encoder);
        final String endpoint1 = subscribeOld(frameListener, ctx, regHeaders, encoder).get(LOCATION).toString();
        final String endpoint2 = subscribeOld(frameListener, ctx, regHeaders, encoder).get(LOCATION).toString();
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
            final ChannelHandlerContext ctx = mockChannelHandlerContext(regId);
            final WebPushFrameListener frameListener = new WebPushFrameListener(MockWebPushServerBuilder
                    .withSubscriptionId(regId)
                    .registrationMaxAge(10000L)
                    .subscriptionMaxAge(50000L)
                    .addSubscription(mockSubscription(regId, subId, endpoint))
                    .build());
            final Http2ConnectionEncoder encoder = mockEncoder(w -> w.thenReturn(subscriptionPath(regId))
                    .thenReturn(subscribePath(regId))
                    .thenReturn(endpointPath(endpoint)));
            frameListener.encoder(encoder);

            final Http2Headers regHeaders = subscribe(frameListener, ctx, encoder);
            final Http2Headers subHeaders = subscribeOld(frameListener, ctx, regHeaders, encoder);
            assertThat(subHeaders.status(), equalTo(CREATED.codeAsText()));
            receivePushMessages(frameListener, ctx, regHeaders);
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
            final ChannelHandlerContext ctx = mockChannelHandlerContext(regId);
            final WebPushFrameListener frameListener = new WebPushFrameListener(MockWebPushServerBuilder
                    .withSubscriptionId(regId)
                    .registrationMaxAge(10000L)
                    .subscriptionMaxAge(50000L)
                    .addSubscription(mockSubscription(regId, subId, endpoint))
                    .build());
            final Http2ConnectionEncoder encoder = mockEncoder(w -> w.thenReturn(subscriptionPath(regId))
                    .thenReturn(subscribePath(regId))
                    .thenReturn(endpointPath(endpoint)));
            frameListener.encoder(encoder);

            final Http2Headers regHeaders = subscribe(frameListener, ctx, encoder);
            final Http2Headers subHeaders = subscribeOld(frameListener, ctx, regHeaders, encoder);
            assertThat(subHeaders.status(), equalTo(CREATED.codeAsText()));
            final Http2Headers notifyHeaders = notify(frameListener, ctx, encoder, subHeaders, data);
            assertThat(notifyHeaders.status(), equalTo(OK.codeAsText()));
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
            final ChannelHandlerContext ctx = mockChannelHandlerContext(regId);
            final WebPushFrameListener frameListener = new WebPushFrameListener(MockWebPushServerBuilder
                    .withSubscriptionId(regId)
                    .registrationMaxAge(10000L)
                    .subscriptionMaxAge(50000L)
                    .addSubscription(subscription1)
                    .addSubscription(subscription2)
                    .addSubscription(subscription3)
                    .subscriptionOrder(w -> w.thenReturn(Optional.of(subscription1))
                            .thenReturn(Optional.of(subscription2))
                            .thenReturn(Optional.of(subscription3)))
                    .build());
            final Http2ConnectionEncoder encoder = mockEncoder(w -> w.thenReturn(subscriptionPath(regId))
                    .thenReturn(subscribePath(regId))
                    .thenReturn(subscribePath(regId))
                    .thenReturn(aggregatePath(regId))
                    .thenReturn(webpushPath(subId)));
            frameListener.encoder(encoder);

            final Http2Headers regHeaders = subscribe(frameListener, ctx, encoder);
            final String endpoint1 = subscribeOld(frameListener, ctx, regHeaders, encoder).get(LOCATION).toString();
            final String endpoint2 = subscribeOld(frameListener, ctx, regHeaders, encoder).get(LOCATION).toString();
            final AggregateSubscription aggregateSubscription = asAggregateChannel(
                    new DefaultEntry(endpoint1, Optional.of(5000L)),
                    new DefaultEntry(endpoint2));
            final Http2Headers aggregateChannelHeaders = aggregateSubscribe(frameListener,
                    ctx,
                    regHeaders,
                    toJson(aggregateSubscription),
                    encoder);
            assertThat(aggregateChannelHeaders.get(LOCATION), equalTo(asciiString(webpushPath(subId))));
            receivePushMessages(frameListener, ctx, regHeaders);

            verify(encoder).writePushPromise(eq(ctx), eq(3), eq(pushStreamId), any(Http2Headers.class), eq(0),
                    any(ChannelPromise.class));

            notify(frameListener, ctx, encoder, aggregateChannelHeaders, data);
            verify(encoder, times(2)).writeData(eq(ctx), eq(pushStreamId), eq(data), eq(0), eq(false), any(ChannelPromise.class));
            frameListener.shutdown();
        } finally {
            data.release();
        }
    }

    @Test
    public void notificationMessageTooBig() throws Exception {
        final String regId = "9999";
        final String endpoint = "endpoint1";
        final String subId = "sub1";
        final String message = "message1";
        final ByteBuf data = copiedBuffer(new String(new byte[4099]), UTF_8);
        try {
            final ChannelHandlerContext ctx = mockChannelHandlerContext(regId);
            final WebPushFrameListener frameListener = new WebPushFrameListener(MockWebPushServerBuilder
                    .withSubscriptionId(regId)
                    .registrationMaxAge(10000L)
                    .subscriptionMaxAge(50000L)
                    .messageMaxSize(4098)
                    .addSubscription(mockSubscription(regId, subId, endpoint))
                    .build());
            final Http2ConnectionEncoder encoder = mockEncoder(w -> w.thenReturn(subscriptionPath(regId))
                    .thenReturn(subscribePath(regId))
                    .thenReturn(endpointPath(endpoint)));
            frameListener.encoder(encoder);

            final Http2Headers regHeaders = subscribe(frameListener, ctx, encoder);
            final Http2Headers subHeaders = subscribeOld(frameListener, ctx, regHeaders, encoder);
            assertThat(subHeaders.status(), equalTo(CREATED.codeAsText()));
            final Http2Headers notifyHeaders = notify(frameListener, ctx, encoder, subHeaders, data);
            assertThat(notifyHeaders.status(), equalTo(HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE.codeAsText()));
            frameListener.shutdown();
        } finally {
            data.release();
        }
    }
    */

    private static Http2Headers subscribe(final WebPushFrameListener frameListener,
                                          final ChannelHandlerContext ctx,
                                          final Http2ConnectionEncoder encoder) throws Http2Exception {
        frameListener.onHeadersRead(ctx, 3, registerHeaders(), 0, (short) 22, false, 0, true);
        //final ByteBuf empty = Unpooled.buffer();
        //frameListener.onDataRead(ctx, 3, empty, 0, true);
        return verifyAndCapture(ctx, encoder, true);
    }

    private static ByteString getLinkUri(final AsciiString linkType, final List<ByteString> links) {
        for (ByteString link : links) {
            AsciiString asciiLink = new AsciiString(link, false);
            if (asciiLink.toString().contains(linkType.toString())) {
                return link.subSequence(1, asciiLink.indexOf(";") - 1);
            }
        }
        throw new IllegalStateException("No link header of type " + linkType + " was found in " + links);
    }

    private static void receivePushMessages(final WebPushFrameListener frameListener,
                                            final ChannelHandlerContext ctx,
                                            final Http2Headers subscribeHeaders) throws Http2Exception {
        final ByteString location = subscribeHeaders.get(LOCATION);
        frameListener.onHeadersRead(ctx, 3, receivePushMessageHeaders(location), 0, (short) 22, false, 0, true);
    }

    private static void monitorWithWait(final WebPushFrameListener frameListener,
                                final ChannelHandlerContext ctx,
                                final Http2Headers subscribeHeaders,
                                final int wait) throws Http2Exception {
        final ByteString location = subscribeHeaders.get(LOCATION);
        final Http2Headers http2Headers = receivePushMessageHeaders(location);
        http2Headers.add(new AsciiString("prefer"), new AsciiString("wait=" + wait));
        frameListener.onHeadersRead(ctx, 3, http2Headers, 0, (short) 22, false, 0, true);
    }

    private static Http2Headers notify(final WebPushFrameListener frameListener,
                                final ChannelHandlerContext ctx,
                                final Http2ConnectionEncoder encoder,
                                final Http2Headers subHeaders,
                                final ByteBuf data) throws Http2Exception {
        final ByteString push = getLinkUri(asciiString(PUSH), subHeaders.getAll(LINK_HEADER));
        frameListener.onHeadersRead(ctx, 3, notifyHeaders(push), 0, (short) 22, false, 0, false);
        frameListener.onDataRead(ctx, 3, data, 0, true);
        return verifyAndCapture(ctx, encoder, true);
    }

    private static Http2Headers status(final WebPushFrameListener frameListener,
                                       final ChannelHandlerContext ctx,
                                       final Http2Headers subHeaders,
                                       final Http2ConnectionEncoder encoder,
                                       final boolean endstream) throws Http2Exception {
        final ByteString location = subHeaders.get(LOCATION);
        frameListener.onHeadersRead(ctx, 3, statusHeaders(location), 0, (short) 22, false, 0, false);
        return verifyAndCapture(ctx, encoder, endstream);
    }

    private static Http2Headers deleteSubscription(final WebPushFrameListener frameListener,
                                                   final ChannelHandlerContext ctx,
                                                   final Http2Headers subHeaders,
                                                   final Http2ConnectionEncoder encoder) throws Http2Exception {
        final ByteString location = subHeaders.get(LOCATION);
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
        requestHeaders.method(AsciiString.of(HttpMethod.POST.name()));
        requestHeaders.path(asciiString("/webpush/" + Resource.SUBSCRIBE.resourceName()));
        return requestHeaders;
    }

    private static Http2Headers subHeaders(final ByteString resourceUrl) {
        final Http2Headers requestHeaders = new DefaultHttp2Headers(false);
        requestHeaders.method(AsciiString.of(HttpMethod.POST.name()));
        requestHeaders.path(resourceUrl);
        return requestHeaders;
    }

    private static Http2Headers receivePushMessageHeaders(final ByteString resourceUrl) {
        final Http2Headers requestHeaders = new DefaultHttp2Headers(false);
        requestHeaders.method(AsciiString.of(HttpMethod.GET.name()));
        requestHeaders.path(resourceUrl);
        return requestHeaders;
    }

    private static Http2Headers notifyHeaders(final ByteString resourceUrl) {
        final Http2Headers requestHeaders = new DefaultHttp2Headers(false);
        requestHeaders.method(AsciiString.of(HttpMethod.POST.name()));
        requestHeaders.path(resourceUrl);
        return requestHeaders;
    }

    private static Http2Headers statusHeaders(final ByteString resourceUrl) {
        final Http2Headers requestHeaders = new DefaultHttp2Headers(false);
        requestHeaders.method(AsciiString.of(HttpMethod.GET.name()));
        requestHeaders.path(resourceUrl);
        return requestHeaders;
    }

    private static Http2Headers subDeleteHeaders(final ByteString resourceUrl) {
        final Http2Headers requestHeaders = new DefaultHttp2Headers(false);
        requestHeaders.method(AsciiString.of(HttpMethod.DELETE.name()));
        requestHeaders.path(resourceUrl);
        return requestHeaders;
    }

    private static Http2ConnectionEncoder mockEncoder(final Consumer<OngoingStubbing<String>> consumer,
                                                      final Resource... resources) {
        final Http2ConnectionEncoder encoder = mock(Http2ConnectionEncoder.class);
        final Http2Connection connection = mock(Http2Connection.class);

        final Endpoint local = mock(Endpoint.class);
        final Http2Stream stream = mock(Http2Stream.class);
        when(local.nextStreamId()).thenReturn(4);

        final PropertyKey pathPropertyKey = mock(PropertyKey.class);
        final PropertyKey resourcePropertyKey = mock(PropertyKey.class);
        final PropertyKey pushReceiptPropertyKey = mock(PropertyKey.class);
        when(connection.newKey())
                .thenReturn(pathPropertyKey)
                .thenReturn(resourcePropertyKey)
                .thenReturn(pushReceiptPropertyKey);
        when(stream.getProperty(pushReceiptPropertyKey))
                .thenReturn(Optional.empty());

        consumer.accept(when(stream.getProperty(pathPropertyKey)));
        for (Resource r : resources) {
            // a PUSH need to process both the header and body, hence we need
            // to have the PUSH resource returned twice in a row..
            if (r == Resource.PUSH) {
                when(stream.getProperty(resourcePropertyKey)).thenReturn(r).thenReturn(r);
            } else {
                when(stream.getProperty(resourcePropertyKey)).thenReturn(r);
            }
        }

        when(connection.local()).thenReturn(local);
        when(connection.stream(anyInt())).thenReturn(stream);
        when(encoder.connection()).thenReturn(connection);

        final ChannelFuture channelFuture = mock(ChannelFuture.class);
        when(channelFuture.isSuccess()).thenReturn(true);
        when(encoder.writeHeaders(any(ChannelHandlerContext.class), anyInt(), any(Http2Headers.class), anyInt(),
                anyBoolean(), any(ChannelPromise.class))).thenReturn(channelFuture);
        when(encoder.writeData(any(ChannelHandlerContext.class), anyInt(), any(ByteBuf.class), anyInt(),
                anyBoolean(), any(ChannelPromise.class))).thenReturn(channelFuture);

        when(encoder.writePushPromise(any(ChannelHandlerContext.class),
                anyInt(),
                anyInt(),
                any(Http2Headers.class),
                anyInt(),
                any(ChannelPromise.class)))
                .thenReturn(channelFuture);
        return encoder;
    }

    private static String subscribePath(final String subscriptionId) {
        return webpushPath(Resource.SUBSCRIBE.resourceName(), subscriptionId);
    }

    private static String subscriptionPath(final String subscriptionId) {
        return webpushPath(Resource.SUBSCRIPTION.resourceName(), subscriptionId);
    }

    private static String pushPath(final String token) {
        return webpushPath(Resource.PUSH.resourceName(), token);
    }

    private static String recieptsPath(final String token) {
        return webpushPath(Resource.RECEIPTS.resourceName(), token);
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

    private static ChannelHandlerContext mockChannelHandlerContext(final String subscriptionId) {
        final ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        final Attribute<Object> attribute = mock(Attribute.class);
        final ChannelPromise promise = mock(ChannelPromise.class);
        when(attribute.get()).thenReturn(subscriptionId);
        when(ctx.attr(any(AttributeKey.class))).thenReturn(attribute);
        when(ctx.newPromise()).thenReturn(promise);
        return ctx;
    }

    private static AsciiString subscriptionLocation(final String subscriptionId) {
        return asciiString(subscriptionPath(subscriptionId));
    }

    private static AsciiString webpushUri(Resource resource, String id) {
        return new AsciiString("/webpush/" + resource.resourceName() + "/" + id);
    }

    private static AsciiString pushWebLink(final String pushToken) {
        return asLink(webpushUri(Resource.PUSH, pushToken), WebLink.PUSH);
    }

    private static AsciiString asLink(AsciiString uri, WebLink rel) {
        return new AsciiString("<" + uri + ">;rel=\"" + rel + "\"");
    }

    private static AsciiString receiptWebLink(final String receiptToken) {
        return asLink(webpushUri(Resource.RECEIPTS, receiptToken), WebLink.RECEIPTS);
    }

    private Matcher<ByteString> hasResource(final Resource resource) {
        return new TypeSafeMatcher<ByteString>() {
            @Override
            public boolean matchesSafely(final ByteString byteString) {
                return byteString.toString().contains(WEBPUSH_URI + "/" + resource.resourceName());
            }

            @Override
            public void describeTo(final Description description) {
                description.appendText("Link should contain ").appendValue(WEBPUSH_URI + "/" +  resource.resourceName());
            }

            @Override
            protected void describeMismatchSafely(final ByteString byteString, final Description mismatchDescription) {
                mismatchDescription.appendText(" was ").appendValue(byteString);
            }
        };
    }

}
