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
import org.jboss.aerogear.webpush.AggregateChannel;
import org.jboss.aerogear.webpush.AggregateChannel.Entry;
import org.jboss.aerogear.webpush.Channel;
import org.jboss.aerogear.webpush.DefaultAggregateChannel;
import org.jboss.aerogear.webpush.DefaultAggregateChannel.DefaultEntry;
import org.jboss.aerogear.webpush.Registration.WebLink;
import org.jboss.aerogear.webpush.WebPushServer;
import org.junit.Ignore;
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
import static org.jboss.aerogear.webpush.Registration.WebLink.CHANNEL;
import static org.jboss.aerogear.webpush.Registration.WebLink.MONITOR;
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
        assertThat(responseHeaders.get(LOCATION), equalTo(asciiString(monitorPath(regId))));
        assertThat(responseHeaders.getAll(LINK), hasItems(
                asciiString(MONITOR.weblink(monitorPath(regId))),
                asciiString(CHANNEL.weblink(channelPath(regId))),
                asciiString(AGGREGATE.weblink(aggregatePath(regId)))));
        assertThat(responseHeaders.get(CACHE_CONTROL), equalTo(asciiString("private, max-age=10000")));
        frameListener.shutdown();
    }

    @Test
    public void channel() throws Exception {
        final String regId = "9999";
        final String endpoint = "endpoint1";
        final String channelId = "channel1";
        final ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        final WebPushFrameListener frameListener = new WebPushFrameListener(MockWebPushServerBuilder
                .withRegistrationid(regId)
                .registrationMaxAge(10000L)
                .channelMaxAge(50000L)
                .addChannel(mockChannel(regId, channelId, endpoint))
                .build());
        final Http2ConnectionEncoder encoder = mockEncoder(w -> w.thenReturn(registerPath(regId))
                .thenReturn(channelPath(regId)));
        frameListener.encoder(encoder);

        final Http2Headers registrationHeaders = register(frameListener, ctx, encoder);
        final Http2Headers channelHeaders = channel(frameListener, ctx, registrationHeaders, encoder);
        assertThat(channelHeaders.status(), equalTo(CREATED.codeAsText()));
        assertThat(channelHeaders.get(LOCATION), equalTo(asciiString(webpushPath(endpoint))));
        assertThat(channelHeaders.get(CACHE_CONTROL), equalTo(asciiString("private, max-age=50000")));
        frameListener.shutdown();
    }

    private static Channel mockChannel(final String regId, final String channelId, final String endpoint) {
        final Channel channel = mock(Channel.class);
        when(channel.endpointToken()).thenReturn(endpoint);
        when(channel.channelId()).thenReturn(channelId);
        when(channel.registrationId()).thenReturn(regId);
        when(channel.message()).thenReturn(Optional.empty());
        return channel;
    }

    private static Channel mockChannel(final String regId,
                                       final String channelId,
                                       final String endpoint,
                                       final String message) {
        final Channel channel = mockChannel(regId, channelId, endpoint);
        when(channel.message()).thenReturn(Optional.of(message));
        return channel;
    }

    @Test
    public void channelStatusNoMessage() throws Exception {
        final String regId = "9999";
        final String endpoint = "endpoint1";
        final String channelId = "channel1";
        final String message = "message1";
        final ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        final Channel channel = mockChannel(regId, channelId, endpoint, message);
        when(channel.message()).thenReturn(Optional.of(message)).thenReturn(Optional.<String>empty());
        final WebPushFrameListener frameListener = new WebPushFrameListener(MockWebPushServerBuilder
                .withRegistrationid(regId)
                .registrationMaxAge(10000L)
                .channelMaxAge(50000L)
                .addChannel(channel)
                .build());
        final Http2ConnectionEncoder encoder = mockEncoder(w -> w.thenReturn(registerPath(regId))
                .thenReturn(channelPath(regId)));
        frameListener.encoder(encoder);

        final Http2Headers registrationHeaders = register(frameListener, ctx, encoder);
        final Http2Headers channelResponseHeaders = channel(frameListener, ctx, registrationHeaders, encoder);
        assertThat(channelResponseHeaders.status(), equalTo(CREATED.codeAsText()));
        final Http2Headers channelStatusHeaders = channelStatus(frameListener, ctx, channelResponseHeaders, encoder,
                false);
        assertThat(channelStatusHeaders.status(), equalTo(OK.codeAsText()));
        final ByteBuf byteBuf = verifyAndCaptureData(ctx, encoder);
        assertThat(byteBuf.toString(UTF_8), equalTo(message));
        final Http2Headers channelStatusHeaders2 = channelStatus(frameListener, ctx, channelResponseHeaders, encoder,
                true);
        assertThat(channelStatusHeaders2.status(), equalTo(NO_CONTENT.codeAsText()));
        frameListener.shutdown();
    }

    @Test
    public void channelStatusMessage() throws Exception {
        final String regId = "9999";
        final String endpoint = "endpoint1";
        final String channelId = "channel1";
        final String message = "message1";
        final ByteBuf data = copiedBuffer("message1", UTF_8);
        final ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        final Channel channel = mockChannel(regId, channelId, endpoint, message);
        when(channel.message()).thenReturn(Optional.of(message)).thenReturn(Optional.<String>empty());
        final WebPushFrameListener frameListener = new WebPushFrameListener(MockWebPushServerBuilder
                .withRegistrationid(regId)
                .registrationMaxAge(10000L)
                .channelMaxAge(50000L)
                .addChannel(channel)
                .build());
        final Http2ConnectionEncoder encoder = mockEncoder(w -> w.thenReturn(registerPath(regId))
                .thenReturn(channelPath(regId)));
        frameListener.encoder(encoder);

        final Http2Headers registrationHeaders = register(frameListener, ctx, encoder);
        final Http2Headers channelResponseHeaders = channel(frameListener, ctx, registrationHeaders, encoder);
        assertThat(channelResponseHeaders.status(), equalTo(CREATED.codeAsText()));
        notify(frameListener, ctx, encoder, channelResponseHeaders, data);

        final Http2Headers channelStatusHeaders = channelStatus(frameListener, ctx, channelResponseHeaders, encoder,
                false);
        assertThat(channelStatusHeaders.status(), equalTo(OK.codeAsText()));
        final ByteBuf byteBuf = verifyAndCaptureData(ctx, encoder);
        assertThat(byteBuf.toString(UTF_8), equalTo(message));
        final Http2Headers channelStatusHeaders2 = channelStatus(frameListener, ctx, channelResponseHeaders, encoder,
                true);
        assertThat(channelStatusHeaders2.status(), equalTo(NO_CONTENT.codeAsText()));
        frameListener.shutdown();
    }

    @Test
    public void channelDelete() throws Exception {
        final String regId = "9999";
        final String endpoint = "endpoint1";
        final String channelId = "channel1";
        final ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        final WebPushServer webPushServer = MockWebPushServerBuilder
                .withRegistrationid(regId)
                .registrationMaxAge(10000L)
                .channelMaxAge(50000L)
                .addChannel(mockChannel(regId, channelId, endpoint))
                .build();
        final WebPushFrameListener frameListener = new WebPushFrameListener(webPushServer);
        final Http2ConnectionEncoder encoder = mockEncoder(w -> w.thenReturn(registerPath(regId))
                .thenReturn(channelPath(regId))
                .thenReturn(monitorPath(regId)));
        frameListener.encoder(encoder);

        final Http2Headers registrationHeaders = register(frameListener, ctx, encoder);
        final Http2Headers channelResponseHeaders = channel(frameListener, ctx, registrationHeaders, encoder);
        assertThat(channelResponseHeaders.status(), equalTo(CREATED.codeAsText()));

        final Http2Headers channelDeleteHeaders = channelDelete(frameListener, ctx, channelResponseHeaders, encoder);
        assertThat(channelDeleteHeaders.status(), equalTo(OK.codeAsText()));

        when(webPushServer.getChannel(endpoint)).thenReturn(Optional.empty());
        final Http2Headers channelStatusHeaders = channelStatus(frameListener, ctx, channelResponseHeaders, encoder,
                true);
        assertThat(channelStatusHeaders.status(), equalTo(HttpResponseStatus.NOT_FOUND.codeAsText()));
        frameListener.shutdown();
    }

    @Test
    public void monitor() throws Exception {
        final String regId = "9999";
        final String channelId = "channel1";
        final String endpoint = "endpoint1";
        final ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        final WebPushFrameListener frameListener = new WebPushFrameListener(MockWebPushServerBuilder
                .withRegistrationid(regId)
                .registrationMaxAge(10000L)
                .channelMaxAge(50000L)
                .addChannel(mockChannel(regId, channelId, endpoint))
                .build());
        final Http2ConnectionEncoder encoder = mockEncoder(w -> w.thenReturn(registerPath(regId))
                .thenReturn(monitorPath(regId)));
        frameListener.encoder(encoder);

        final Http2Headers registrationHeaders = register(frameListener, ctx, encoder);
        monitor(frameListener, ctx, registrationHeaders);
        final ArgumentCaptor<Http2Headers> captor = ArgumentCaptor.forClass(Http2Headers.class);
        verify(encoder).writePushPromise(eq(ctx), eq(3), eq(4), captor.capture(), eq(0),
                any(ChannelPromise.class));
        final Http2Headers monitorHeaders = captor.getValue();
        assertThat(monitorHeaders.status(), equalTo(OK.codeAsText()));
        assertThat(monitorHeaders.getAll(LINK), hasItems(
                asciiString(CHANNEL.weblink(channelPath(regId))),
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
        final String channelid = "channel1";
        final int pushStreamId = 4;
        try {
            final Channel channel = mockChannel(regId, channelid, endpoint);
            when(channel.message()).thenReturn(Optional.of(message)).thenReturn(Optional.of(message));

            final ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
            final WebPushServer webPushServer = MockWebPushServerBuilder.withRegistrationid(regId)
                    .registrationMaxAge(10000L)
                    .channelMaxAge(50000L)
                    .addChannel(channel)
                    .build();
            when(webPushServer.getChannel(endpoint)).thenReturn(Optional.of(channel));
            final WebPushFrameListener frameListener = new WebPushFrameListener(webPushServer);

            final Http2ConnectionEncoder encoder = mockEncoder(w -> w.thenReturn(registerPath(regId))
                    .thenReturn(channelPath(regId))
                    .thenReturn(webpushPath(regId, endpoint))
                    .thenReturn(monitorPath(regId)));
            frameListener.encoder(encoder);

            final Http2Headers registrationHeaders = register(frameListener, ctx, encoder);
            final Http2Headers channelHeaders = channel(frameListener, ctx, registrationHeaders, encoder);
            assertThat(channelHeaders.status(), equalTo(CREATED.codeAsText()));
            notify(frameListener, ctx, encoder, channelHeaders, data);

            monitorWithWait(frameListener, ctx, registrationHeaders, 0);
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
        final Channel channel1 = mockChannel(regId, "channel1", "endpoint1", "message1");
        final Channel channel2 = mockChannel(regId, "channel2", "endpoint2", "message2");
        final Channel channel3 = mockChannel(regId, "channel3", "aggChannel");
        final WebPushFrameListener frameListener = new WebPushFrameListener(MockWebPushServerBuilder
                .withRegistrationid(regId)
                .registrationMaxAge(10000L)
                .channelMaxAge(50000L)
                .addChannel(channel1)
                .addChannel(channel2)
                .addChannel(channel3)
                .channelOrder(w -> w.thenReturn(Optional.of(channel1))
                        .thenReturn(Optional.of(channel2))
                        .thenReturn(Optional.of(channel3)))
                .build());
        final Http2ConnectionEncoder encoder = mockEncoder(w -> w.thenReturn(registerPath(regId))
                .thenReturn(channelPath(regId))
                .thenReturn(channelPath(regId))
                .thenReturn(aggregatePath(regId)));
        frameListener.encoder(encoder);
        final Http2Headers registrationHeaders = register(frameListener, ctx, encoder);
        final String endpoint1 = channel(frameListener, ctx, registrationHeaders, encoder).get(LOCATION).toString();
        final String endpoint2 = channel(frameListener, ctx, registrationHeaders, encoder).get(LOCATION).toString();
        final AggregateChannel aggregateChannel = asAggregateChannel(
                new DefaultEntry(endpoint1, Optional.of(5000L)),
                new DefaultEntry(endpoint2));
        aggregateChannel(frameListener, ctx, registrationHeaders, toJson(aggregateChannel), encoder);
        final Http2Headers aggregateResponseHeaders = verifyAndCapture(ctx, encoder, true);
        assertThat(aggregateResponseHeaders.status(), equalTo(CREATED.codeAsText()));
        assertThat(aggregateResponseHeaders.get(LOCATION), equalTo(asciiString(webpushPath("aggChannel"))));
        assertThat(aggregateResponseHeaders.get(CACHE_CONTROL), equalTo(asciiString("private, max-age=50000")));
        frameListener.shutdown();
    }

    @Test
    public void notification() throws Exception {
        final String regId = "9999";
        final String endpoint = "endpoint1";
        final String channelId = "channel1";
        final String message = "message1";
        final ByteBuf data = copiedBuffer("some payload", UTF_8);
        final int pushStreamId = 4;
        try {
            final ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
            final WebPushFrameListener frameListener = new WebPushFrameListener(MockWebPushServerBuilder
                    .withRegistrationid(regId)
                    .registrationMaxAge(10000L)
                    .channelMaxAge(50000L)
                    .addChannel(mockChannel(regId, channelId, endpoint, message))
                    .build());
            final Http2ConnectionEncoder encoder = mockEncoder(w -> w.thenReturn(registerPath(regId))
                    .thenReturn(channelPath(regId))
                    .thenReturn(webpushPath(regId, endpoint)));
            frameListener.encoder(encoder);

            final Http2Headers registrationHeaders = register(frameListener, ctx, encoder);
            final Http2Headers channelHeaders = channel(frameListener, ctx, registrationHeaders, encoder);
            assertThat(channelHeaders.status(), equalTo(CREATED.codeAsText()));
            monitor(frameListener, ctx, registrationHeaders);
            verify(encoder).writePushPromise(eq(ctx), eq(3), eq(pushStreamId), any(Http2Headers.class), eq(0),
                    any(ChannelPromise.class));
            notify(frameListener, ctx, encoder, channelHeaders, data);
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
        final String channelId = "channel1";
        final String message = "message1";
        final ByteBuf data = copiedBuffer("some payload", UTF_8);
        try {
            final ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
            final WebPushFrameListener frameListener = new WebPushFrameListener(MockWebPushServerBuilder
                    .withRegistrationid(regId)
                    .registrationMaxAge(10000L)
                    .channelMaxAge(50000L)
                    .addChannel(mockChannel(regId, channelId, endpoint, message))
                    .build());
            final Http2ConnectionEncoder encoder = mockEncoder(w -> w.thenReturn(registerPath(regId))
                    .thenReturn(channelPath(regId))
                    .thenReturn(webpushPath(regId, endpoint)));
            frameListener.encoder(encoder);

            final Http2Headers registrationHeaders = register(frameListener, ctx, encoder);
            final Http2Headers channelHeaders = channel(frameListener, ctx, registrationHeaders, encoder);
            assertThat(channelHeaders.status(), equalTo(CREATED.codeAsText()));
            final Http2Headers notifyHeaders = notify(frameListener, ctx, encoder, channelHeaders, data);
            assertThat(notifyHeaders.status(), equalTo(HttpResponseStatus.ACCEPTED.codeAsText()));
            frameListener.shutdown();
        } finally {
            data.release();
        }
    }

    @Test
    public void aggregateChannelNotification() throws Exception {
        final String regId = "9999";
        final String channelId = "aggChannel";
        final ByteBuf data = copiedBuffer("aggregate payload", UTF_8);
        final Channel channel1 = mockChannel(regId, "channel1", "endpoint1");
        final Channel channel2 = mockChannel(regId, "channel2", "endpoint2");
        final Channel channel3 = mockChannel(regId, "channel3", "aggChannel");
        try {
            final int pushStreamId = 4;
            final ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
            final WebPushFrameListener frameListener = new WebPushFrameListener(MockWebPushServerBuilder
                    .withRegistrationid(regId)
                    .registrationMaxAge(10000L)
                    .channelMaxAge(50000L)
                    .addChannel(channel1)
                    .addChannel(channel2)
                    .addChannel(channel3)
                    .channelOrder(w -> w.thenReturn(Optional.of(channel1))
                            .thenReturn(Optional.of(channel2))
                            .thenReturn(Optional.of(channel3)))
                    .build());
            final Http2ConnectionEncoder encoder = mockEncoder(w -> w.thenReturn(registerPath(regId))
                    .thenReturn(channelPath(regId))
                    .thenReturn(channelPath(regId))
                    .thenReturn(aggregatePath(regId))
                    .thenReturn(webpushPath(channelId)));
            frameListener.encoder(encoder);

            final Http2Headers registrationHeaders = register(frameListener, ctx, encoder);
            final String endpoint1 = channel(frameListener, ctx, registrationHeaders, encoder).get(LOCATION).toString();
            final String endpoint2 = channel(frameListener, ctx, registrationHeaders, encoder).get(LOCATION).toString();
            final AggregateChannel aggregateChannel = asAggregateChannel(
                    new DefaultEntry(endpoint1, Optional.of(5000L)),
                    new DefaultEntry(endpoint2));
            final Http2Headers aggregateChannelHeaders = aggregateChannel(frameListener,
                    ctx,
                    registrationHeaders,
                    toJson(aggregateChannel),
                    encoder);
            assertThat(aggregateChannelHeaders.get(LOCATION), equalTo(asciiString(webpushPath(channelId))));
            monitor(frameListener, ctx, registrationHeaders);

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

    private static Http2Headers channel(final WebPushFrameListener frameListener,
                                        final ChannelHandlerContext ctx,
                                        final Http2Headers registrationHeaders,
                                        final Http2ConnectionEncoder encoder) throws Http2Exception {
        final AsciiString channelUri = getLinkUri(asciiString(CHANNEL), registrationHeaders.getAll(LINK));
        frameListener.onHeadersRead(ctx, 3, channelHeaders(channelUri), 0, (short) 22, false, 0, false);
        frameListener.onDataRead(ctx, 3, Unpooled.buffer(), 0, true);
        return verifyAndCapture(ctx, encoder, true);
    }

    private static Http2Headers aggregateChannel(final WebPushFrameListener frameListener,
                                                 final ChannelHandlerContext ctx,
                                                 final Http2Headers registrationHeaders,
                                                 final String json,
                                                 final Http2ConnectionEncoder encoder) throws Http2Exception {
        final AsciiString channelUri = getLinkUri(asciiString(AGGREGATE), registrationHeaders.getAll(LINK));
        frameListener.onHeadersRead(ctx, 3, channelHeaders(channelUri), 0, (short) 22, false, 0, false);
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
                                final Http2Headers channelHeaders,
                                final ByteBuf data) throws Http2Exception {
        final AsciiString location = channelHeaders.get(LOCATION);
        frameListener.onHeadersRead(ctx, 3, notifyHeaders(location), 0, (short) 22, false, 0, false);
        frameListener.onDataRead(ctx, 3, data, 0, true);
        return verifyAndCapture(ctx, encoder, true);
    }

    private static Http2Headers channelStatus(final WebPushFrameListener frameListener,
                               final ChannelHandlerContext ctx,
                               final Http2Headers channelHeaders,
                               final Http2ConnectionEncoder encoder,
                               final boolean endstream) throws Http2Exception {
        final AsciiString location = channelHeaders.get(LOCATION);
        frameListener.onHeadersRead(ctx, 3, channelStatusHeaders(location), 0, (short) 22, false, 0, false);
        return verifyAndCapture(ctx, encoder, endstream);
    }

    private static Http2Headers channelDelete(final WebPushFrameListener frameListener,
                                              final ChannelHandlerContext ctx,
                                              final Http2Headers channelHeaders,
                                              final Http2ConnectionEncoder encoder) throws Http2Exception {
        final AsciiString location = channelHeaders.get(LOCATION);
        frameListener.onHeadersRead(ctx, 3, channelDeleteHeaders(location), 0, (short) 22, false, 0, false);
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
        requestHeaders.path(asciiString("/webpush/register"));
        return requestHeaders;
    }

    private static Http2Headers channelHeaders(final AsciiString resourceUrl) {
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

    private static Http2Headers channelStatusHeaders(final AsciiString resourceUrl) {
        final Http2Headers requestHeaders = new DefaultHttp2Headers(false);
        requestHeaders.method(HttpMethod.GET.name());
        requestHeaders.path(resourceUrl);
        return requestHeaders;
    }

    private static Http2Headers channelDeleteHeaders(final AsciiString resourceUrl) {
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

    private static AggregateChannel asAggregateChannel(final Entry... entries) {
        final LinkedHashSet<Entry> channels = new LinkedHashSet<>(Arrays.asList(entries));
        return new DefaultAggregateChannel(channels);
    }

    private static String monitorPath(final String registrationId) {
        return webpushPath(registrationId, "monitor");
    }

    private static String channelPath(final String registrationId) {
        return webpushPath(registrationId, "channel");
    }

    private static String aggregatePath(final String registrationId) {
        return webpushPath(registrationId, "aggregate");
    }

    private static String registerPath(final String registrationId) {
        return webpushPath(registrationId, "register");
    }

    private static String webpushPath(final String registrationId, final String path) {
        return webpushPath(registrationId + "/" + path);
    }

    private static String webpushPath(final String path) {
        return "/webpush/" + path;
    }

}