package example.micronaut;

import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.SleepingIdleStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.aeron.Aeron;
import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;

public class AeronClusterClient {

    static AeronClusterClient instance = null;
    static final Logger LOGGER = LoggerFactory.getLogger(AeronClusterClient.class);

    Publication publication = null;
    Subscription subscription = null;
    private static final int PUB_STREAM_ID = 10;
    private static final int SUB_STREAM_ID = 11;
    SleepingIdleStrategy senderIdleStrategy = new SleepingIdleStrategy();
    SleepingIdleStrategy revceiverIdleStrategy = new SleepingIdleStrategy();

    SendAgent sender;
    ReceiveAgent receiver;

    private AeronClusterClient() {

        final MediaDriver.Context mediaDriverCtx = new MediaDriver.Context()
            .dirDeleteOnStart(true)
            .threadingMode(ThreadingMode.SHARED)
            .sharedIdleStrategy(new SleepingIdleStrategy())
            .dirDeleteOnShutdown(true);

        final MediaDriver mediaDriver = MediaDriver.launchEmbedded(mediaDriverCtx);


        Aeron.Context ctx = new Aeron.Context()
            .aeronDirectoryName(mediaDriver.aeronDirectoryName());
        
        Aeron aeron = Aeron.connect(ctx);
        publication = aeron.addPublication("aeron:udp?endpoint=localhost:5555", PUB_STREAM_ID);
        subscription = aeron.addSubscription("aeron:udp?endpoint=localhost:5556", SUB_STREAM_ID);

        sender = new SendAgent(publication);
        AgentRunner sendRunner = new AgentRunner(senderIdleStrategy, Throwable::printStackTrace, null, sender);
        AgentRunner.startOnThread(sendRunner);

        receiver = new ReceiveAgent(subscription, (sessionId, message) -> WSGateway.getInstance().received(sessionId, message));
        AgentRunner receiveRunner = new AgentRunner(revceiverIdleStrategy, Throwable::printStackTrace, null, receiver);
        AgentRunner.startOnThread(receiveRunner);
    }

    public static AeronClusterClient getInstance() {
        if (instance == null) {
            instance = new AeronClusterClient();
        }

        return instance;
    }
    
    public void send(String sessionId, String message) {
        sender.setMessage(sessionId, message);
    }
    
    @FunctionalInterface
    public interface OnMessageReceived {
        void received(String sessionId, String message);        
    }
}
