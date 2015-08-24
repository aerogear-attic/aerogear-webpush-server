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
import org.jboss.aerogear.webpush.DefaultPushMessage;
import org.jboss.aerogear.webpush.DefaultSubscription;
import org.jboss.aerogear.webpush.PushMessage;
import org.jboss.aerogear.webpush.Resource;
import org.jboss.aerogear.webpush.Subscription;
import org.jboss.aerogear.webpush.WebLink;
import org.jboss.aerogear.webpush.util.HttpHeaders;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.OngoingStubbing;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static io.netty.buffer.Unpooled.copiedBuffer;
import static io.netty.handler.codec.http.HttpHeaderNames.CACHE_CONTROL;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderNames.LOCATION;
import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE;
import static io.netty.util.CharsetUtil.UTF_8;
import static io.netty.handler.codec.http.HttpResponseStatus.GONE;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.jboss.aerogear.webpush.Resource.SUBSCRIBE;
import static org.jboss.aerogear.webpush.WebLink.PUSH;
import static org.jboss.aerogear.webpush.util.HttpHeaders.LINK_HEADER;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WebPushFrameListenerTest {

    private static final int STREAM_ID = 3;
    private static final int PROMISE_STEAM_ID = 4;

    @Test (expected = NullPointerException.class)
    public void withNullWebPushServer() {
        new WebPushFrameListener(null);
    }

    @Test
    public void subscribe() throws Exception {
        final String subscriptionId = "subscriptionId";
        final String pushResourceId = "pushResourceId";
        final String pushToken = "pushToken";
        final String receiptToken = "receiptToken";
        final ChannelHandlerContext ctx = mockChannelHandlerContext(subscriptionId);
        final WebPushFrameListener frameListener = new WebPushFrameListener(MockWebPushServerBuilder
                .withSubscription(new DefaultSubscription(subscriptionId, pushResourceId))
                .pushResourceToken(pushToken)
                .receiptsToken(receiptToken)
                .build());
        final Http2ConnectionEncoder encoder = mockEncoder(w -> w.thenReturn(subscribePath(subscriptionId)), SUBSCRIBE);
        frameListener.encoder(encoder);
        try {
            final Http2Headers responseHeaders = UserAgent.subscribe(frameListener, ctx, encoder);
            assertThat(responseHeaders.status(), equalTo(CREATED.codeAsText()));
            assertThat(responseHeaders.get(LOCATION), equalTo(subscriptionLocation(subscriptionId)));
            assertThat(responseHeaders.getAll(LINK_HEADER), hasItems(pushWebLink(pushResourceId), receiptWebLink(receiptToken)));
            assertThat(responseHeaders.get(CACHE_CONTROL), equalTo(asciiString("private, max-age=0")));
        } finally {
            frameListener.shutdown();
        }
    }

    @Test @Ignore("need to sort out the return status code.")
    public void deleteSubscription() throws Exception {
        final String subscriptionId = "subscriptionId";
        final String pushResourceId = "pushResourceId";
        final ChannelHandlerContext ctx = mockChannelHandlerContext(subscriptionId);
        final WebPushFrameListener frameListener = new WebPushFrameListener(MockWebPushServerBuilder
                .withSubscription(new DefaultSubscription(subscriptionId, pushResourceId))
                .pushResourceToken(pushResourceId)
                .build());
        final Http2ConnectionEncoder encoder = mockEncoder(w -> w.thenReturn(pushPath(pushResourceId))
                .thenReturn(pushPath(pushResourceId)), Resource.PUSH);
        frameListener.encoder(encoder);
        try {
            final Http2Headers subscribeHeaders = UserAgent.subscribe(frameListener, ctx, encoder);
            UserAgent.deleteSubscription(frameListener, ctx, subscribeHeaders, encoder);
            final ByteBuf payload = copiedBuffer("payload", UTF_8);
            final Http2Headers headers = AppServer.sendPush(frameListener, ctx, encoder, subscribeHeaders, payload);
            /*
            https://tools.ietf.org/html/draft-thomson-webpush-protocol-00#section-7.3:
            "A push service MUST return a 400-series status code, such as 404 (Not
             Found) or 410 (Gone) if an application server attempts to send a push
             message to a removed or expired push message subscription."
            */
            assertThat(headers.status(), equalTo(NOT_FOUND.codeAsText()));
            // or
            assertThat(headers.status(), equalTo(GONE.codeAsText()));
        } finally {
            frameListener.shutdown();
        }
    }

    @Test
    public void receivePushMessages() throws Exception {
        final String subscriptionId = "subscriptionId";
        final String pushResourceId = "pushResourceId";
        final ChannelHandlerContext ctx = mockChannelHandlerContext(subscriptionId);
        final WebPushFrameListener frameListener = new WebPushFrameListener(MockWebPushServerBuilder
                .withSubscription(new DefaultSubscription(subscriptionId, pushResourceId))
                .subscriptionMaxAge(10000L)
                .pushResourceToken(pushResourceId)
                .build());
        final Http2ConnectionEncoder encoder = mockEncoder(w -> w.thenReturn(pushPath(pushResourceId))
                .thenReturn(pushPath(pushResourceId)), Resource.PUSH);
        frameListener.encoder(encoder);
        try {
            final Http2Headers subscribeHeaders = UserAgent.subscribe(frameListener, ctx, encoder);
            UserAgent.receivePushMessages(frameListener, ctx, subscribeHeaders);
            AppServer.sendPush(frameListener, ctx, encoder, subscribeHeaders, copiedBuffer("payload", UTF_8));
            final Http2Headers headers = capturePromiseHeaders(encoder);
            assertThat(headers.status(), equalTo(OK.codeAsText()));
            assertThat(headers.get(CACHE_CONTROL), equalTo(asciiString("private")));
        } finally {
            frameListener.shutdown();
        }
    }

    @Test
    public void receivePushMessagesWaitNoMessages() throws Exception {
        final String subscriptionId = "subscriptionId";
        final String pushResourceId = "pushResourceId";
        final ChannelHandlerContext ctx = mockChannelHandlerContext(subscriptionId);
        final WebPushFrameListener frameListener = new WebPushFrameListener(MockWebPushServerBuilder
                .withSubscription(new DefaultSubscription(subscriptionId, pushResourceId))
                .subscriptionMaxAge(10000L)
                .pushResourceToken(pushResourceId)
                .build());
        final Http2ConnectionEncoder encoder = mockEncoder(w -> w.thenReturn(pushPath(pushResourceId))
                .thenReturn(pushPath(pushResourceId)), Resource.PUSH);
        frameListener.encoder(encoder);
        try {
            final Http2Headers subscribeHeaders = UserAgent.subscribe(frameListener, ctx, encoder);
            AppServer.sendPush(frameListener, ctx, encoder, subscribeHeaders, copiedBuffer("payload", UTF_8));
            UserAgent.receivePushMessagesWithWait(frameListener, ctx, subscribeHeaders, 0);
            final Http2Headers headers = captureStreamHeaders(encoder);
            assertThat(headers.status(), equalTo(NO_CONTENT.codeAsText()));
        } finally {
            frameListener.shutdown();
        }
    }


    @Test
    public void receivePushMessagesWait() throws Exception {
        final String subscriptionId = "subscriptionId";
        final String pushResourceId = "pushResourceId";
        final PushMessage pushMessage = new DefaultPushMessage("pushMessageId", subscriptionId, Optional.empty(), "testing", Optional.of(0));
        final ChannelHandlerContext ctx = mockChannelHandlerContext(subscriptionId);
        final WebPushFrameListener frameListener = new WebPushFrameListener(MockWebPushServerBuilder
                .withSubscription(new DefaultSubscription(subscriptionId, pushResourceId))
                .subscriptionMaxAge(10000L)
                .waitingPushMessage(pushMessage)
                .pushResourceToken(pushResourceId)
                .build());
        final Http2ConnectionEncoder encoder = mockEncoder(w -> w.thenReturn(pushPath(pushResourceId))
                .thenReturn(pushPath(pushResourceId)), Resource.PUSH);
        frameListener.encoder(encoder);
        try {
            final Http2Headers subscribeHeaders = UserAgent.subscribe(frameListener, ctx, encoder);
            AppServer.sendPush(frameListener, ctx, encoder, subscribeHeaders, copiedBuffer(pushMessage.payload(), UTF_8));
            UserAgent.receivePushMessagesWithWait(frameListener, ctx, subscribeHeaders, 0);
            final Http2Headers headers = capturePromiseHeaders(encoder);
            assertThat(headers.status(), equalTo(OK.codeAsText()));
            assertThat(headers.get(CACHE_CONTROL), equalTo(asciiString("private")));
            assertThat(headers.get(CONTENT_TYPE), equalTo(asciiString("text/plain;charset=utf8")));
        } finally {
            frameListener.shutdown();
        }
    }

    @Test
    public void sendPush() throws Exception {
        final String subscriptionId = "subscriptionId";
        final String pushResourceId = "pushResourceId";
        final String receiptsToken = "receiptsToken";
        final String pushMessageToken = "pushMessageToken";
        final ChannelHandlerContext ctx = mockChannelHandlerContext(subscriptionId);
        final WebPushFrameListener frameListener = new WebPushFrameListener(MockWebPushServerBuilder
                .withSubscription(new DefaultSubscription(subscriptionId, pushResourceId))
                .subscriptionMaxAge(10000L)
                .receiptsToken(receiptsToken)
                .pushResourceToken(pushResourceId)
                .pushMessageToken(pushMessageToken)
                .build());
        final Http2ConnectionEncoder encoder = mockEncoder(w -> w.thenReturn(pushPath(pushResourceId))
                .thenReturn(pushPath(pushResourceId)), Resource.PUSH);
        frameListener.encoder(encoder);
        try {
            final Http2Headers subscribeHeaders = UserAgent.subscribe(frameListener, ctx, encoder);
            final Http2Headers headers = AppServer.sendPush(frameListener, ctx, encoder, subscribeHeaders, copiedBuffer("Test", UTF_8));
            assertThat(headers.status(), equalTo(CREATED.codeAsText()));
            assertThat(headers.get(LOCATION), equalTo(asciiString(messagePath(pushMessageToken))));
        } finally {
            frameListener.shutdown();
        }
    }

    @Test
    public void sendPushMessageTooBig() throws Exception {
        final String subscriptionId = "subscriptionId";
        final String pushResourceId = "pushResourceId";
        final String payload = new String(new byte[4099]);
        final ByteBuf data = copiedBuffer(payload, UTF_8);
        final PushMessage pushMessage = new DefaultPushMessage("pushMessageId", subscriptionId, Optional.empty(), payload, Optional.of(0));
        final ChannelHandlerContext ctx = mockChannelHandlerContext(subscriptionId);
        final WebPushFrameListener frameListener = new WebPushFrameListener(MockWebPushServerBuilder
                .withSubscription(new DefaultSubscription(subscriptionId, pushResourceId))
                .subscriptionMaxAge(10000L)
                .waitingPushMessage(pushMessage)
                .pushResourceToken(pushResourceId)
                .build());
        final Http2ConnectionEncoder encoder = mockEncoder(w -> w.thenReturn(pushPath(pushResourceId))
                .thenReturn(pushPath(pushResourceId)), Resource.PUSH);
        frameListener.encoder(encoder);
        try {
            final Http2Headers subscribeHeaders = UserAgent.subscribe(frameListener, ctx, encoder);
            final Http2Headers headers = AppServer.sendPush(frameListener, ctx, encoder, subscribeHeaders, data);
            assertThat(headers.status(), equalTo(REQUEST_ENTITY_TOO_LARGE.codeAsText()));
        } finally {
            frameListener.shutdown();
        }
    }

    @Test
    public void receipt() throws Exception {
        final String subscriptionId = "subscriptionId";
        final String pushResourceId = "pushResourceId";
        final String receiptsToken = "receiptsToken";
        final String receiptToken = "receiptToken";
        final ChannelHandlerContext ctx = mockChannelHandlerContext(subscriptionId);
        final WebPushFrameListener frameListener = new WebPushFrameListener(MockWebPushServerBuilder
                .withSubscription(new DefaultSubscription(subscriptionId, pushResourceId))
                .subscriptionMaxAge(10000L)
                .receiptsToken(receiptsToken)
                .receiptToken(receiptToken)
                .pushResourceToken(pushResourceId)
                .build());
        final Http2ConnectionEncoder encoder = mockEncoder(w -> w.thenReturn(recieptsPath(receiptToken)), Resource.RECEIPT);
        frameListener.encoder(encoder);
        try {
            final Http2Headers subscribeHeaders = UserAgent.subscribe(frameListener, ctx, encoder);
            final ByteString receiptsUri = getLinkUri(asciiString(WebLink.RECEIPTS), subscribeHeaders.getAll(LINK_HEADER));
            final Http2Headers headers = AppServer.receiptsSubcsribe(receiptsUri, frameListener, ctx, encoder);
            assertThat(headers.status(), equalTo(CREATED.codeAsText()));
            assertThat(headers.get(LOCATION), equalTo(asciiString(recieptsPath(receiptToken))));
        } finally {
            frameListener.shutdown();
        }
    }

    @Test
    public void receiveReceipt() throws Exception {
        final String subscriptionId = "subscriptionId";
        final String pushResourceId = "pushResourceId";
        final String receiptsToken = "receiptsToken";
        final String receiptToken = "receiptToken";
        final ByteBuf payload = copiedBuffer("Testing", UTF_8);
        final Subscription subscription = new DefaultSubscription(subscriptionId, pushResourceId);
        final ChannelHandlerContext ctx = mockChannelHandlerContext(subscriptionId);
        final WebPushFrameListener frameListener = new WebPushFrameListener(MockWebPushServerBuilder
                .withSubscription(subscription)
                .subscriptionMaxAge(10000L)
                .receiptsToken(receiptsToken)
                .receiptToken(receiptToken, new DefaultPushMessage("123", subscriptionId, Optional.of(receiptToken), "test", Optional.empty()))
                .pushResourceToken(pushResourceId)
                .build());
        final Http2ConnectionEncoder encoder = mockEncoder(w -> w.thenReturn(pushPath(pushResourceId))
                .thenReturn(pushPath(pushResourceId)),
                receiptToken,
                Resource.RECEIPT,
                Resource.PUSH);
        frameListener.encoder(encoder);
        try {
            final Http2Headers subscribeHeaders = UserAgent.subscribe(frameListener, ctx, encoder);
            final ByteString receiptsUri = getLinkUri(asciiString(WebLink.RECEIPTS), subscribeHeaders.getAll(LINK_HEADER));
            final Http2Headers receiptsHeaders = AppServer.receiptsSubcsribe(receiptsUri, frameListener, ctx, encoder);
            receiveReceipts(frameListener, ctx, receiptsHeaders.get(LOCATION));
            final Http2Headers sendHeaders = AppServer.sendPush(frameListener, ctx, encoder, subscribeHeaders, payload);
            final ByteString pushMessageUri = sendHeaders.get(LOCATION);
            assertThat(sendHeaders.status(), equalTo(CREATED.codeAsText()));
            assertThat(pushMessageUri, equalTo(asciiString(messagePath(receiptToken))));
            final Http2Headers ackHeaders = UserAgent.acknowledgePushMessage(frameListener, ctx, encoder, pushMessageUri);
            assertThat(ackHeaders.status(), equalTo(GONE.codeAsText()));
        } finally {
            frameListener.shutdown();
        }
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

    private static void receiveReceipts(final WebPushFrameListener frameListener,
                                            final ChannelHandlerContext ctx,
                                            final ByteString receiptUri) throws Http2Exception {
        frameListener.onHeadersRead(ctx, STREAM_ID, receiveReceiptsHeaders(receiptUri), 0, (short) 22, false, 0, true);
    }

    private static Http2Headers verifyAndCapture(final ChannelHandlerContext ctx,
                                                 final Http2ConnectionEncoder encoder,
                                                 final boolean endstream) {
        final ArgumentCaptor<Http2Headers> captor = ArgumentCaptor.forClass(Http2Headers.class);
        verify(encoder, atLeastOnce()).writeHeaders(eq(ctx), eq(STREAM_ID), captor.capture(), eq(0), eq(endstream),
                any(ChannelPromise.class));
        return captor.getValue();
    }

    private static AsciiString asciiString(final String str) {
        return new AsciiString(str);
    }

    private static AsciiString asciiString(final WebLink type) {
        return new AsciiString(type.toString());
    }

    private static Http2Headers subscribeHeaders() {
        final Http2Headers requestHeaders = new DefaultHttp2Headers(false);
        requestHeaders.method(AsciiString.of(HttpMethod.POST.name()));
        requestHeaders.path(asciiString("/webpush/" + SUBSCRIBE.resourceName()));
        return requestHeaders;
    }

    private static Http2Headers receivePushMessageHeaders(final ByteString resourceUrl) {
        final Http2Headers requestHeaders = new DefaultHttp2Headers(false);
        requestHeaders.method(AsciiString.of(HttpMethod.GET.name()));
        requestHeaders.path(resourceUrl);
        return requestHeaders;
    }

    private static Http2Headers receiptsSubscribeHeaders(final ByteString resourceUrl) {
        final Http2Headers requestHeaders = new DefaultHttp2Headers(false);
        requestHeaders.method(AsciiString.of(HttpMethod.POST.name()));
        requestHeaders.path(resourceUrl);
        return requestHeaders;
    }

    private static Http2Headers receiveReceiptsHeaders(final ByteString resourceUrl) {
        final Http2Headers requestHeaders = new DefaultHttp2Headers(false);
        requestHeaders.method(AsciiString.of(HttpMethod.GET.name()));
        requestHeaders.path(resourceUrl);
        return requestHeaders;
    }

    private static Http2Headers sendHeaders(final ByteString resourceUrl, final Optional<ByteString> receiptUrl) {
        final Http2Headers requestHeaders = new DefaultHttp2Headers(false);
        receiptUrl.ifPresent( url -> requestHeaders.add(HttpHeaders.PUSH_RECEIPT_HEADER, url));
        requestHeaders.method(AsciiString.of(HttpMethod.POST.name()));
        requestHeaders.path(resourceUrl);
        return requestHeaders;
    }

    private static Http2Headers subDeleteHeaders(final ByteString resourceUrl) {
        final Http2Headers requestHeaders = new DefaultHttp2Headers(false);
        requestHeaders.method(AsciiString.of(HttpMethod.DELETE.name()));
        requestHeaders.path(resourceUrl);
        return requestHeaders;
    }

    private static Http2Headers ackDeleteHeaders(final ByteString resourceUrl) {
        final Http2Headers requestHeaders = new DefaultHttp2Headers(false);
        requestHeaders.method(AsciiString.of(HttpMethod.DELETE.name()));
        requestHeaders.path(resourceUrl);
        return requestHeaders;
    }

    private static Http2ConnectionEncoder mockEncoder(final Consumer<OngoingStubbing<String>> consumer,
                                                      final Resource... resources) {
        return mockEncoder(consumer, null, resources);
    }

    private static Http2ConnectionEncoder mockEncoder(final Consumer<OngoingStubbing<String>> consumer,
                                                      final String receiptToken,
                                                      final Resource... resources) {
        final Http2ConnectionEncoder encoder = mock(Http2ConnectionEncoder.class);
        final Http2Connection connection = mock(Http2Connection.class);

        final Endpoint local = mock(Endpoint.class);
        final Http2Stream stream = mock(Http2Stream.class);
        when(local.nextStreamId()).thenReturn(PROMISE_STEAM_ID);

        final PropertyKey pathPropertyKey = mock(PropertyKey.class);
        final PropertyKey resourcePropertyKey = mock(PropertyKey.class);
        final PropertyKey pushReceiptPropertyKey = mock(PropertyKey.class);
        final PropertyKey ttlPropertyKey = mock(PropertyKey.class);
        when(connection.newKey())
                .thenReturn(pathPropertyKey)
                .thenReturn(resourcePropertyKey)
                .thenReturn(pushReceiptPropertyKey)
                .thenReturn(ttlPropertyKey);
        when(stream.getProperty(pushReceiptPropertyKey))
                .thenReturn(Optional.ofNullable(receiptToken));
        when(stream.getProperty(ttlPropertyKey))
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
        return webpushPath(SUBSCRIBE.resourceName(), subscriptionId);
    }

    private static String subscriptionPath(final String subscriptionId) {
        return webpushPath(Resource.SUBSCRIPTION.resourceName(), subscriptionId);
    }

    private static String pushPath(final String token) {
        return webpushPath(Resource.PUSH.resourceName(), token);
    }

    private static String messagePath(final String token) {
        return webpushPath(Resource.PUSH_MESSAGE.resourceName(), token);
    }

    private static String recieptsPath(final String token) {
        return webpushPath(Resource.RECEIPT.resourceName(), token);
    }

    private static String webpushPath(final String path, final String registrationId) {
        return webpushPath(path + "/" + registrationId);
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

    private static Http2Headers captureStreamHeaders(final Http2ConnectionEncoder encoder) {
        final ArgumentCaptor<Http2Headers> captor = ArgumentCaptor.forClass(Http2Headers.class);
        verify(encoder, atLeastOnce()).writeHeaders(any(ChannelHandlerContext.class), eq(STREAM_ID), captor.capture(), eq(0), eq(true), any(ChannelPromise.class));
        return captor.getValue();
    }

    private static Http2Headers capturePromiseHeaders(final Http2ConnectionEncoder encoder) {
        final ArgumentCaptor<Http2Headers> captor = ArgumentCaptor.forClass(Http2Headers.class);
        verify(encoder).writeHeaders(any(ChannelHandlerContext.class), eq(PROMISE_STEAM_ID), captor.capture(), eq(0), eq(false), any(ChannelPromise.class));
        return captor.getValue();
    }

    private static class UserAgent {

        private static Http2Headers subscribe(final WebPushFrameListener frameListener,
                                              final ChannelHandlerContext ctx,
                                              final Http2ConnectionEncoder encoder) throws Http2Exception {
            frameListener.onHeadersRead(ctx, STREAM_ID, subscribeHeaders(), 0, (short) 22, false, 0, true);
            return verifyAndCapture(ctx, encoder, true);
        }

        private static Http2Headers deleteSubscription(final WebPushFrameListener frameListener,
                                                       final ChannelHandlerContext ctx,
                                                       final Http2Headers subHeaders,
                                                       final Http2ConnectionEncoder encoder) throws Http2Exception {
            final ByteString location = subHeaders.get(LOCATION);
            frameListener.onHeadersRead(ctx, STREAM_ID, subDeleteHeaders(location), 0, (short) 22, false, 0, false);
            return verifyAndCapture(ctx, encoder, true);
        }

        private static void receivePushMessages(final WebPushFrameListener frameListener,
                                                final ChannelHandlerContext ctx,
                                                final Http2Headers subscribeHeaders) throws Http2Exception {
            final ByteString location = subscribeHeaders.get(LOCATION);
            frameListener.onHeadersRead(ctx, STREAM_ID, receivePushMessageHeaders(location), 0, (short) 22, false, 0, true);
        }

        private static void receivePushMessagesWithWait(final WebPushFrameListener frameListener,
                                                        final ChannelHandlerContext ctx,
                                                        final Http2Headers subscribeHeaders,
                                                        final int wait) throws Http2Exception {
            final Http2Headers http2Headers = receivePushMessageHeaders(subscribeHeaders.get(LOCATION));
            http2Headers.add(new AsciiString("prefer"), new AsciiString("wait=" + wait));
            frameListener.onHeadersRead(ctx, STREAM_ID, http2Headers, 0, (short) 22, false, 0, true);
        }

        private static Http2Headers acknowledgePushMessage(final WebPushFrameListener frameListener,
                                                           final ChannelHandlerContext ctx,
                                                           final Http2ConnectionEncoder encoder,
                                                           final ByteString location) throws Http2Exception {
            frameListener.onHeadersRead(ctx, STREAM_ID, ackDeleteHeaders(location), 0, (short) 22, false, 0, false);
            final ArgumentCaptor<Http2Headers> captor = ArgumentCaptor.forClass(Http2Headers.class);
            verify(encoder, atLeastOnce()).writeHeaders(eq(ctx), eq(PROMISE_STEAM_ID), captor.capture(), eq(0), eq(true),
                    any(ChannelPromise.class));
            return captor.getValue();
        }

    }

    private static class AppServer {
        private static Http2Headers sendPush(final WebPushFrameListener frameListener,
                                             final ChannelHandlerContext ctx,
                                             final Http2ConnectionEncoder encoder,
                                             final Http2Headers subHeaders,
                                             final ByteBuf data) throws Http2Exception {
            final ByteString push = getLinkUri(asciiString(PUSH), subHeaders.getAll(LINK_HEADER));
            final Optional<ByteString> receipts = Optional.ofNullable(getLinkUri(asciiString(WebLink.RECEIPTS), subHeaders.getAll(LINK_HEADER)));
            frameListener.onHeadersRead(ctx, STREAM_ID, sendHeaders(push, receipts), 0, (short) 22, false, 0, false);
            frameListener.onDataRead(ctx, STREAM_ID, data, 0, true);
            return verifyAndCapture(ctx, encoder, true);
        }

        private static Http2Headers receiptsSubcsribe(final ByteString receiptsUri,
                                                      final WebPushFrameListener frameListener,
                                                      final ChannelHandlerContext ctx,
                                                      final Http2ConnectionEncoder encoder) throws Http2Exception {
            frameListener.onHeadersRead(ctx, STREAM_ID, receiptsSubscribeHeaders(receiptsUri), 0, (short) 22, false, 0, true);
            final ArgumentCaptor<Http2Headers> captor = ArgumentCaptor.forClass(Http2Headers.class);
            verify(encoder, atLeastOnce()).writeHeaders(any(ChannelHandlerContext.class), eq(STREAM_ID), captor.capture(), eq(0), eq(true), any(ChannelPromise.class));
            return captor.getValue();
        }

    }

}
