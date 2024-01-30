package example.micronaut;

import org.agrona.DirectBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aeroncookbook.sbe.MessageHeaderDecoder;
import com.aeroncookbook.sbe.SimpleMessageDecoder;

import example.micronaut.AeronClusterClient.OnMessageReceived;
import io.aeron.cluster.client.EgressListener;
import io.aeron.logbuffer.Header;

public class ClientEgressListener implements EgressListener {

    static final Logger LOGGER = LoggerFactory.getLogger(ClientEgressListener.class);
    private final OnMessageReceived onReceiveHandler;

    final MessageHeaderDecoder headerDecoder = new MessageHeaderDecoder();
    final SimpleMessageDecoder decoder = new SimpleMessageDecoder();

    public ClientEgressListener(final OnMessageReceived onReceiveHandler) {
        this.onReceiveHandler = onReceiveHandler;
    }

    @Override
    public void onMessage(long clusterSessionId, long timestamp, DirectBuffer buffer, int offset, int length, Header header) {
        LOGGER.info("Received message from {}", clusterSessionId);
        // NOTE: we don't care about the MessageHeader RN
        headerDecoder.wrap(buffer, offset);
        decoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        String sessionId = decoder.sessionId();
        String message = decoder.message();
        onReceiveHandler.received(sessionId, message);
    }

}
