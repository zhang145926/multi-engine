package com.baidu.unbiz.multiengine.transport;

/**
 * Created by wangchongjie on 16/3/31.
 */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * Handler implementation for the echo client.  It initiates the ping-pong
 * traffic between the echo client and server by sending the first message to
 * the server.
 */
public class TaskClientHandler extends ChannelInboundHandlerAdapter {

    private static final Log LOG = LogFactory.getLog(TaskServerHandler.class);

    /**
     * Creates a client-side handler.
     */
    public TaskClientHandler() {
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {

    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof ByteBuf) {
            TaskClient.fillResult(msg);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        LOG.debug("channelReadComplete");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Close the connection when an exception is raised.
        ctx.close();
        LOG.debug("exceptionCaught", cause);
    }

}