package org.jboss.aerogear.webpush.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.AsciiString;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.Http2Headers;
import org.junit.Test;
import org.mockito.ArgumentCaptor;


import static io.netty.handler.codec.http.HttpHeaderNames.CACHE_CONTROL;
import static io.netty.handler.codec.http.HttpHeaderNames.LOCATION;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.*;
import static org.jboss.aerogear.webpush.netty.WebPushFrameListener.LINK;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
        final Http2ConnectionEncoder encoder = mock(Http2ConnectionEncoder.class);
        frameListener.encoder(encoder);

        final Http2Headers responseHeaders = register(frameListener, ctx, encoder);
        assertThat(responseHeaders.status(), equalTo(asciiString("200")));
        assertThat(responseHeaders.get(LOCATION), equalTo(asciiString("webpush/9999/monitor")));
        assertThat(responseHeaders.get(LINK), equalTo(asciiString("<webpush/9999/channel>;rel=\"push:channel\"")));
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
        final Http2ConnectionEncoder encoder = mock(Http2ConnectionEncoder.class);
        frameListener.encoder(encoder);

        final Http2Headers registrationHeaders = register(frameListener, ctx, encoder);
        final Http2Headers channelResponseHeaders = channel(frameListener, ctx, encoder, registrationHeaders);
        assertThat(channelResponseHeaders.status(), equalTo(asciiString("201")));
        assertThat(channelResponseHeaders.get(LOCATION), equalTo(asciiString("webpush/9999/endpointToken")));
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
        final Http2ConnectionEncoder encoder = mock(Http2ConnectionEncoder.class);
        frameListener.encoder(encoder);

        final Http2Headers registrationHeaders = register(frameListener, ctx, encoder);
        monitor(frameListener, ctx, registrationHeaders);
        verify(encoder).writePushPromise(eq(ctx), eq(3), eq(2), any(Http2Headers.class), eq(0),
                any(ChannelPromise.class));
    }

    private static Http2Headers register(final WebPushFrameListener frameListener,
                                         final ChannelHandlerContext ctx,
                                         final Http2ConnectionEncoder encoder) throws Http2Exception {
        frameListener.onHeadersRead(ctx, 3, registerHeaders(), 0, (short) 22, false, 0, true);
        return verifyAndCapture(ctx, encoder, 1);
    }

    private static Http2Headers channel(final WebPushFrameListener frameListener,
                                       final ChannelHandlerContext ctx,
                                       final Http2ConnectionEncoder encoder,
                                       final Http2Headers registrationHeaders) throws Http2Exception {
        final AsciiString link = registrationHeaders.get(LINK);
        final AsciiString channelUri = link.subSequence(1, link.indexOf(";")-1);
        //final AsciiString channelUri = link.subSequence(link.indexOf('/')+1, link.indexOf("/channel"));
        frameListener.onHeadersRead(ctx, 3, channelHeaders(channelUri), 0, (short) 22, false, 0, true);
        return verifyAndCapture(ctx, encoder, 2);
    }

    private static void monitor(final WebPushFrameListener frameListener,
                                        final ChannelHandlerContext ctx,
                                        final Http2Headers registrationHeaders) throws Http2Exception {
        final AsciiString location = registrationHeaders.get(LOCATION);
        frameListener.onHeadersRead(ctx, 3, monitorHeaders(location), 0, (short) 22, false, 0, true);
    }

    private static Http2Headers verifyAndCapture(final ChannelHandlerContext ctx,
                                                 final Http2ConnectionEncoder encoder,
                                                 final int times) {
        final ArgumentCaptor<Http2Headers> captor = ArgumentCaptor.forClass(Http2Headers.class);
        verify(encoder, times(times)).writeHeaders(eq(ctx), eq(3), captor.capture(), eq(0), eq(false),
                any(ChannelPromise.class));
        return captor.getValue();

    }

    private static AsciiString asciiString(final String str) {
        return new AsciiString(str);
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

}