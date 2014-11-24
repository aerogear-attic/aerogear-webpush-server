package org.jboss.aerogear.webpush.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.AsciiString;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2Connection.Endpoint;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2Stream;
import io.netty.util.CharsetUtil;
import org.jboss.aerogear.webpush.Registration.WebLink;
import org.junit.Test;
import org.mockito.ArgumentCaptor;


import java.util.List;

import static io.netty.handler.codec.http.HttpHeaderNames.CACHE_CONTROL;
import static io.netty.handler.codec.http.HttpHeaderNames.LOCATION;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.*;
import static org.jboss.aerogear.webpush.netty.WebPushFrameListener.LINK;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
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
        final ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        final WebPushFrameListener frameListener = new WebPushFrameListener(MockWebPushServerBuilder
                .withRegistrationid("9999")
                .registrationMaxAge(10000L)
                .build());
        final Http2ConnectionEncoder encoder = mockEncoder();
        frameListener.encoder(encoder);

        final Http2Headers responseHeaders = register(frameListener, ctx, encoder);
        assertThat(responseHeaders.status(), equalTo(asciiString("200")));
        assertThat(responseHeaders.get(LOCATION), equalTo(asciiString("/webpush/9999/monitor")));
        assertThat(responseHeaders.getAll(LINK), hasItems(
                asciiString(WebLink.MONITOR.weblink("/webpush/9999/monitor")),
                asciiString(WebLink.CHANNEL.weblink("/webpush/9999/channel"))));
        assertThat(responseHeaders.get(CACHE_CONTROL), equalTo(asciiString("private, max-age=10000")));
    }

    @Test
    public void channelCreation() throws Exception {
        final ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        final WebPushFrameListener frameListener = new WebPushFrameListener(MockWebPushServerBuilder
                .withRegistrationid("9999")
                .registrationMaxAge(10000L)
                .channelMaxAge(50000L)
                .build());
        final Http2ConnectionEncoder encoder = mockEncoder();
        frameListener.encoder(encoder);

        final Http2Headers registrationHeaders = register(frameListener, ctx, encoder);
        final Http2Headers channelResponseHeaders = channel(frameListener, ctx, encoder, registrationHeaders);
        assertThat(channelResponseHeaders.status(), equalTo(asciiString("201")));
        assertThat(channelResponseHeaders.get(LOCATION), equalTo(asciiString("webpush/endpointToken")));
        assertThat(channelResponseHeaders.get(CACHE_CONTROL), equalTo(asciiString("private, max-age=50000")));
    }

    @Test
    public void monitor() throws Exception {
        final ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        final WebPushFrameListener frameListener = new WebPushFrameListener(MockWebPushServerBuilder
                .withRegistrationid("9999")
                .registrationMaxAge(10000L)
                .channelMaxAge(50000L)
                .build());
        final Http2ConnectionEncoder encoder = mockEncoder();
        frameListener.encoder(encoder);

        final Http2Headers registrationHeaders = register(frameListener, ctx, encoder);
        monitor(frameListener, ctx, registrationHeaders);
        verify(encoder).writePushPromise(eq(ctx), eq(3), eq(4), any(Http2Headers.class), eq(0),
                any(ChannelPromise.class));
    }

    @Test
    public void notification() throws Exception {
        final ByteBuf data = Unpooled.copiedBuffer("some payload", CharsetUtil.UTF_8);
        final int pushStreamId = 4;
        try {
            final ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
            final WebPushFrameListener frameListener = new WebPushFrameListener(MockWebPushServerBuilder
                    .withRegistrationid("9999")
                    .registrationMaxAge(10000L)
                    .channelMaxAge(50000L)
                    .build());
            final Http2ConnectionEncoder encoder = mockEncoder();
            frameListener.encoder(encoder);

            final Http2Headers registrationHeaders = register(frameListener, ctx, encoder);
            final Http2Headers channelHeaders = channel(frameListener, ctx, encoder, registrationHeaders);
            assertThat(channelHeaders.status(), equalTo(asciiString("201")));
            monitor(frameListener, ctx, registrationHeaders);
            verify(encoder).writePushPromise(eq(ctx), eq(3), eq(pushStreamId), any(Http2Headers.class), eq(0),
                    any(ChannelPromise.class));

            notify(frameListener, ctx, channelHeaders, data);
            verify(encoder).writeData(eq(ctx), eq(pushStreamId), eq(data), eq(0), eq(false), any(ChannelPromise.class));
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
        return verifyAndCapture(ctx, encoder, true, 1);
    }

    private static Http2Headers channel(final WebPushFrameListener frameListener,
                                       final ChannelHandlerContext ctx,
                                       final Http2ConnectionEncoder encoder,
                                       final Http2Headers registrationHeaders) throws Http2Exception {
        final AsciiString channelUri = getLinkUri(asciiString(WebLink.CHANNEL), registrationHeaders.getAll(LINK));
        frameListener.onHeadersRead(ctx, 3, channelHeaders(channelUri), 0, (short) 22, false, 0, false);
        frameListener.onDataRead(ctx, 3, Unpooled.buffer() , 0, true);
        return verifyAndCapture(ctx, encoder, true, 2);
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

    private static void notify(final WebPushFrameListener frameListener,
                                final ChannelHandlerContext ctx,
                                final Http2Headers channelHeaders,
                                final ByteBuf data) throws Http2Exception {
        final AsciiString location = channelHeaders.get(LOCATION);
        frameListener.onHeadersRead(ctx, 3, notifyHeaders(location), 0, (short) 22, false, 0, false);
        frameListener.onDataRead(ctx, 3, data , 0, true);
    }

    private static Http2Headers verifyAndCapture(final ChannelHandlerContext ctx,
                                                 final Http2ConnectionEncoder encoder,
                                                 final boolean endStream,
                                                 final int times) {
        final ArgumentCaptor<Http2Headers> captor = ArgumentCaptor.forClass(Http2Headers.class);
        verify(encoder, times(times)).writeHeaders(eq(ctx), eq(3), captor.capture(), eq(0), eq(endStream),
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
        requestHeaders.method(asciiString(HttpMethod.POST.name()));
        requestHeaders.path(asciiString("/webpush/register"));
        return requestHeaders;
    }

    private static Http2Headers channelHeaders(final AsciiString resourceUrl) {
        final Http2Headers requestHeaders = new DefaultHttp2Headers(false);
        requestHeaders.method(asciiString(HttpMethod.POST.name()));
        requestHeaders.path(resourceUrl);
        return requestHeaders;
    }

    private static Http2Headers monitorHeaders(final AsciiString resourceUrl) {
        final Http2Headers requestHeaders = new DefaultHttp2Headers(false);
        requestHeaders.method(asciiString(HttpMethod.GET.name()));
        requestHeaders.path(resourceUrl);
        return requestHeaders;
    }

    private static Http2Headers notifyHeaders(final AsciiString resourceUrl) {
        final Http2Headers requestHeaders = new DefaultHttp2Headers(false);
        requestHeaders.method(asciiString(HttpMethod.PUT.name()));
        requestHeaders.path(resourceUrl);
        return requestHeaders;
    }

    private static Http2ConnectionEncoder mockEncoder() {
        final Http2ConnectionEncoder encoder = mock(Http2ConnectionEncoder.class);
        final Http2Connection connection = mock(Http2Connection.class);
        final Endpoint local = mock(Endpoint.class);
        final Http2Stream stream = mock(Http2Stream.class);
        when(local.nextStreamId()).thenReturn(4);
        when(stream.getProperty("webpush.path"))
                .thenReturn("/webpush/9999/register")
                .thenReturn("/webpush/9999/channel")
                .thenReturn("/webpush/9999/endpointToken");
        when(connection.local()).thenReturn(local);
        when(connection.stream(anyInt())).thenReturn(stream);
        when(encoder.connection()).thenReturn(connection);
        return encoder;
    }

}