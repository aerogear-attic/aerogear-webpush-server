package org.jboss.aerogear.webpush;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.AsciiString;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.Http2Flags;
import io.netty.handler.codec.http2.Http2FrameListener;
import io.netty.handler.codec.http2.Http2FrameReader;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.util.CharsetUtil;

import static io.netty.handler.codec.http.HttpHeaderNames.LOCATION;
import static io.netty.util.internal.ObjectUtil.checkNotNull;

public class WebPushFrameReader implements Http2FrameReader {

    private final Http2FrameReader reader;
    private final ResponseHandler callback;

    private final static AsciiString LINK = new AsciiString("link");

    public WebPushFrameReader(final ResponseHandler callback, final Http2FrameReader reader) {
        this.reader = checkNotNull(reader, "reader");
        this.callback = callback;
    }

    @Override
    public void readFrame(final ChannelHandlerContext ctx, final ByteBuf input, final Http2FrameListener listener)
            throws Http2Exception {

        reader.readFrame(ctx, input, new Http2FrameListener() {

            @Override
            public int onDataRead(ChannelHandlerContext ctx, int streamId, ByteBuf data,
                                  int padding, boolean endOfStream) throws Http2Exception {
                callback.notification(data.toString(CharsetUtil.UTF_8), streamId);
                return listener.onDataRead(ctx, streamId, data, padding, endOfStream);
            }

            @Override
            public void onHeadersRead(ChannelHandlerContext ctx, int streamId,
                                      Http2Headers headers, int padding, boolean endStream)
                    throws Http2Exception {
                processHeaders(headers, streamId);
                listener.onHeadersRead(ctx, streamId, headers, padding, endStream);
            }

            private void processHeaders(final Http2Headers headers, final int streamId) {
                if (headers.contains(LINK) && headers.contains(LOCATION)) {
                    callback.registerResponse(headers, streamId);
                } else if (headers.contains(LOCATION)) {
                    callback.channelResponse(headers, streamId);
                } else {
                    callback.channelStatus(headers, streamId);
                }
            }

            @Override
            public void onHeadersRead(ChannelHandlerContext ctx, int streamId,
                                      Http2Headers headers, int streamDependency, short weight, boolean exclusive,
                                      int padding, boolean endStream) throws Http2Exception {
                processHeaders(headers, streamId);
                listener.onHeadersRead(ctx, streamId, headers, streamDependency, weight, exclusive,
                        padding, endStream);
            }

            @Override
            public void onPriorityRead(ChannelHandlerContext ctx, int streamId,
                                       int streamDependency, short weight, boolean exclusive) throws Http2Exception {
                listener.onPriorityRead(ctx, streamId, streamDependency, weight, exclusive);
            }

            @Override
            public void onRstStreamRead(ChannelHandlerContext ctx, int streamId, long errorCode)
                    throws Http2Exception {
                listener.onRstStreamRead(ctx, streamId, errorCode);
            }

            @Override
            public void onSettingsAckRead(ChannelHandlerContext ctx) throws Http2Exception {
                listener.onSettingsAckRead(ctx);
            }

            @Override
            public void onSettingsRead(ChannelHandlerContext ctx, Http2Settings settings)
                    throws Http2Exception {
                listener.onSettingsRead(ctx, settings);
            }

            @Override
            public void onPingRead(ChannelHandlerContext ctx, ByteBuf data) throws Http2Exception {
                listener.onPingRead(ctx, data);
            }

            @Override
            public void onPingAckRead(ChannelHandlerContext ctx, ByteBuf data) throws Http2Exception {
                listener.onPingAckRead(ctx, data);
            }

            @Override
            public void onPushPromiseRead(ChannelHandlerContext ctx, int streamId,
                                          int promisedStreamId, Http2Headers headers, int padding) throws Http2Exception {
                callback.channelStatus(headers, streamId);
                listener.onPushPromiseRead(ctx, streamId, promisedStreamId, headers, padding);
            }

            @Override
            public void onGoAwayRead(ChannelHandlerContext ctx, int lastStreamId, long errorCode,
                                     ByteBuf debugData) throws Http2Exception {
                System.out.println("go away read: lastStreamId: " + lastStreamId
                        + ", errorCode: " + errorCode
                        + ", debugData: " + debugData.toString(CharsetUtil.UTF_8));
                listener.onGoAwayRead(ctx, lastStreamId, errorCode, debugData);
            }

            @Override
            public void onWindowUpdateRead(ChannelHandlerContext ctx, int streamId, int windowSizeIncrement)
                    throws Http2Exception {
                listener.onWindowUpdateRead(ctx, streamId, windowSizeIncrement);
            }

            @Override
            public void onUnknownFrame(ChannelHandlerContext ctx, byte frameType, int streamId,
                                       Http2Flags flags, ByteBuf payload) {
                listener.onUnknownFrame(ctx, frameType, streamId, flags, payload);
            }
        });
    }

    @Override
    public void close() {
        reader.close();
    }

    @Override
    public Configuration configuration() {
        return reader.configuration();
    }
}
