package example.micronaut;

import java.nio.ByteBuffer;

import org.agrona.concurrent.Agent;
import org.agrona.concurrent.UnsafeBuffer;

import com.aeroncookbook.sbe.SimpleMessageEncoder;

import io.aeron.Publication;
import com.aeroncookbook.sbe.MessageHeaderEncoder;

class SendAgent implements Agent {
    
    private final Publication publication;
    private boolean hasMessage = false;
    
    private static final int MAX_MESSAGE_SIZE = 1024;
    private UnsafeBuffer sendBuffer = new UnsafeBuffer(ByteBuffer.allocateDirect(MAX_MESSAGE_SIZE));
    private final SimpleMessageEncoder encoder = new SimpleMessageEncoder()
        .wrapAndApplyHeader(sendBuffer, 0, new MessageHeaderEncoder());

    public SendAgent(Publication publication) {
        this.publication = publication;
    }

    @Override
    public int doWork() throws Exception {
        if (hasMessage)
        {
            if (publication.isConnected())
            {
                int encodedLength = MessageHeaderEncoder.ENCODED_LENGTH + encoder.encodedLength();
                publication.offer(sendBuffer, 0, encodedLength);
                hasMessage = false;
                return 0;
            }
            return 1;
        }
        else 
        {
            return 0;
        }
    }

    @Override
    public String roleName() {
        return "Sender";
    }

    public void setMessage(String sessionId, String message) {
        if ((sessionId.length() + message.length() + Integer.BYTES * 2) > MAX_MESSAGE_SIZE) {
            return;
        }

        encoder.sessionId(sessionId);
        encoder.message(message);

        hasMessage = true;
    }
}