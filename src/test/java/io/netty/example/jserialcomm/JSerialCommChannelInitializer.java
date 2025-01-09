package io.netty.example.jserialcomm;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.jsc.JSerialCommChannel;
import io.netty.channel.jsc.JSerialCommChannelConfig;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

public class JSerialCommChannelInitializer  extends ChannelInitializer<JSerialCommChannel> {
    @Override
    public void initChannel(JSerialCommChannel ch) throws Exception {
        JSerialCommChannelConfig config= ch.config();
        config.setBaudrate(9600);  //JSerialCommChannelConfig
        config.setDatabits(8);
        config.setStopbits(JSerialCommChannelConfig.Stopbits.STOPBITS_1);
        config.setParitybit(JSerialCommChannelConfig.Paritybit.NONE);
        config.setReadTimeout(5000);
        ChannelPipeline pipeline= ch.pipeline();
        //pipeline.addLast(new TimeoutHandler(2));
        pipeline.addLast(new LineBasedFrameDecoder(32768));
        pipeline.addLast(new StringEncoder());
        pipeline.addLast(new StringDecoder());

        pipeline.addLast(new JSerialCommClientHandler());
        //pipeline.addLast(new IdleStateHandler(10, 10, 10, TimeUnit.SECONDS));

        //pipeline.addLast(new JSerialCommReadTimeoutHandler(2));
        pipeline.addLast(new ExceptionCaughtHandler());//把异常处理器放在pipeline最后
    }

}
