package org.jboss.aerogear.webpush;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.Http2Flags;
import io.netty.handler.codec.http2.Http2FrameListener;
import io.netty.handler.codec.http2.Http2FrameReader;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.util.CharsetUtil;

import static io.netty.util.internal.ObjectUtil.checkNotNull;

public class WebPushFrameReader implements Http2FrameReader {
    private final Http2FrameReader reader;

    public WebPushFrameReader(Http2FrameReader reader) {
        this.reader = checkNotNull(reader, "reader");
    }

    @Override
    public void readFrame(ChannelHandlerContext ctx, ByteBuf input, final Http2FrameListener listener)
            throws Http2Exception {

        reader.readFrame(ctx, input, new Http2FrameListener() {

            @Override
            public int onDataRead(ChannelHandlerContext ctx, int streamId, ByteBuf data,
                                  int padding, boolean endOfStream) throws Http2Exception {
                System.out.println("Got notification: " + data.toString(CharsetUtil.UTF_8));
                System.out.print("> ");
                return listener.onDataRead(ctx, streamId, data, padding, endOfStream);
            }

            @Override
            public void onHeadersRead(ChannelHandlerContext ctx, int streamId,
                                      Http2Headers headers, int padding, boolean endStream)
                    throws Http2Exception {
                System.out.println("Headers: " + headers);
                System.out.print("> ");
                listener.onHeadersRead(ctx, streamId, headers, padding, endStream);
            }

            @Override
            public void onHeadersRead(ChannelHandlerContext ctx, int streamId,
                                      Http2Headers headers, int streamDependency, short weight, boolean exclusive,
                                      int padding, boolean endStream) throws Http2Exception {
                System.out.println("Headers: " + headers);
                System.out.print("> ");
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
                listener.onPushPromiseRead(ctx, streamId, promisedStreamId, headers, padding);
            }

            @Override
            public void onGoAwayRead(ChannelHandlerContext ctx, int lastStreamId, long errorCode,
                                     ByteBuf debugData) throws Http2Exception {
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
