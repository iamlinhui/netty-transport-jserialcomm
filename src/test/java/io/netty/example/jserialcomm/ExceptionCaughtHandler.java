package io.netty.example.jserialcomm;

import com.fazecast.jSerialComm.SerialPortTimeoutException;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.jsc.JSerialCommReadTimeoutException;

/**
 * 定义一个异常处理类，放在pipeline的最后,处理各类异常
 */
public class ExceptionCaughtHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        //System.out.println("Test Exception");
        //if (cause instanceof SerialPortTimeoutException) {
        if (cause instanceof JSerialCommReadTimeoutException) {

            System.out.println("串口读取超时");
            //超时后，克定义业务需要的处理，例如可重新读取数据
        }
        else {
            super.exceptionCaught(ctx, cause);
         }
    }
}
