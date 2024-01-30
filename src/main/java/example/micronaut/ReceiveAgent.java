package example.micronaut;

import org.agrona.DirectBuffer;
import org.agrona.concurrent.Agent;

import com.aeroncookbook.sbe.MessageHeaderDecoder;
import com.aeroncookbook.sbe.SimpleMessageDecoder;

import example.micronaut.AeronClusterClient.OnMessageReceived;
import io.aeron.Subscription;
import io.aeron.logbuffer.Header;

class ReceiveAgent implements Agent {

    final Subscription subscription;
    final OnMessageReceived onReceiveHandler;
    final MessageHeaderDecoder headerDecoder = new MessageHeaderDecoder();
    final SimpleMessageDecoder decoder = new SimpleMessageDecoder();

    ReceiveAgent(Subscription subscription, OnMessageReceived onReceiveHandler) {
        this.subscription = subscription;
        this.onReceiveHandler = onReceiveHandler;
    }

    @Override
    public int doWork() throws Exception {
        subscription.poll(this::handler, 1);
        return 0;
    }

    private void handler(DirectBuffer buffer, int offset, int length, Header header) {
        // NOTE: we don't care about the MessageHeader RN
        headerDecoder.wrap(buffer, offset);

        decoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        
        String sessionId = decoder.sessionId();
        String message = decoder.message();
        onReceiveHandler.received(sessionId, message);
    }

    @Override
    public String roleName() {
        return "Receiver";
    }
}