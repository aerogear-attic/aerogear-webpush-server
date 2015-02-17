package org.jboss.aerogear.webpush;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

public class HttpResponseHandler extends SimpleChannelInboundHandler<FullHttpResponse> {

    private final SortedMap<Integer, ChannelPromise> streamidPromiseMap;
    private final EventHandler handler;

    public HttpResponseHandler(final EventHandler handler) {
        streamidPromiseMap = new TreeMap<>();
        this.handler = handler;
    }

    /**
     * Create an association between an anticipated response stream id and a {@link ChannelPromise}
     *
     * @param streamId The stream for which a response is expected
     * @param promise The promise object that will be used to wait/notify events
     * @return The previous object associated with {@code streamId}
     * @see HttpResponseHandler#awaitResponses(long, TimeUnit)
     */
    public ChannelPromise put(int streamId, ChannelPromise promise) {
        return streamidPromiseMap.put(streamId, promise);
    }

    /**
     * Wait (sequentially) for a time duration for each anticipated response
     *
     * @param timeout Value of time to wait for each response
     * @param unit Units associated with {@code timeout}
     * @see HttpResponseHandler#put(int, ChannelPromise)
     */
    public void awaitResponses(long timeout, TimeUnit unit) {
        Iterator<Entry<Integer, ChannelPromise>> itr = streamidPromiseMap.entrySet().iterator();
        while (itr.hasNext()) {
            Entry<Integer, ChannelPromise> entry = itr.next();
            ChannelPromise promise = entry.getValue();
            if (!promise.awaitUninterruptibly(timeout, unit)) {
                throw new IllegalStateException("Timed out waiting for response on stream id " + entry.getKey());
            }
            if (!promise.isSuccess()) {
                throw new RuntimeException(promise.cause());
            }
            System.out.println("---Stream id: " + entry.getKey() + " received---");
            itr.remove();
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
    }

    @Override
    public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
        handler.message("Channel with id " + ctx.channel().id() + ", became inactive/disonnected.");
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        super.exceptionCaught(ctx, cause);
    }
}
