package example.micronaut;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.SleepingIdleStrategy;
import org.agrona.concurrent.SystemEpochClock;
import org.agrona.concurrent.UnsafeBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aeroncookbook.sbe.MessageHeaderEncoder;
import com.aeroncookbook.sbe.SimpleMessageEncoder;

import example.micronaut.Application.OnMessageReceived;
import example.micronaut.WSGateway.GatewayStatistics;
import io.aeron.Publication;
import io.aeron.cluster.client.AeronCluster;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import io.aeron.samples.cluster.ClusterConfig;

public class ClusterInteractionAgent implements Agent {

    static final Logger LOGGER = LoggerFactory.getLogger(ClusterInteractionAgent.class);
    static ClusterInteractionAgent instance = null;

    public static ClusterInteractionAgent getInstance() {
        return instance;
    }

    private static final long HEARTBEAT_INTERVAL = 250;
    private static final String INGRESS_CHANNEL = "aeron:udp?term-length=64k";
    private long lastHeartbeatTime = Long.MIN_VALUE;
    private AeronCluster aeronCluster;
    private MediaDriver mediaDriver;
    private static final int MAX_MESSAGE_SIZE = 1024 * 1024 * 64;
    private UnsafeBuffer sendBuffer = new UnsafeBuffer(ByteBuffer.allocateDirect(MAX_MESSAGE_SIZE));


    private static final int PORT_BASE = 9000;

    public ClusterInteractionAgent(final OnMessageReceived onReceiveHandler) {
        instance = this;

        final List<String> hostnames = Arrays.asList("127.0.0.1");

        final String ingressEndpoints = ClusterConfig.ingressEndpoints(
            hostnames, PORT_BASE, ClusterConfig.CLIENT_FACING_PORT_OFFSET);

        int port = PORT_BASE;

        final String egressChannel = "aeron:udp?endpoint=localhost:" + port;
        final ClientEgressListener clientEgressListener = new ClientEgressListener(onReceiveHandler);

        mediaDriver = MediaDriver.launch(new MediaDriver.Context()
            .threadingMode(ThreadingMode.SHARED)
            .dirDeleteOnStart(true)
            .errorHandler(this::logError)
            .dirDeleteOnShutdown(true));
        
        aeronCluster = AeronCluster.connect(
            new AeronCluster.Context()
                .egressListener(clientEgressListener)
                .egressChannel(egressChannel)
                .ingressChannel(INGRESS_CHANNEL)
                .ingressEndpoints(ingressEndpoints)
                .errorHandler(this::logError)
                .aeronDirectoryName(mediaDriver.aeronDirectoryName()));

        LOGGER.info("Connected to cluster leader, node {}", aeronCluster.leaderMemberId());
    }
    
    @Override
    public int doWork() throws Exception {
        //send cluster heartbeat roughly every 250ms
        final long now = SystemEpochClock.INSTANCE.time();
        if (now >= (lastHeartbeatTime + HEARTBEAT_INTERVAL))
        {
            lastHeartbeatTime = now;
            aeronCluster.sendKeepAlive();
        }

        //poll outbound messages from the cluster
        if (null != aeronCluster && !aeronCluster.isClosed())
        {
            aeronCluster.pollEgress();
        }
        
        return 0;
    }

    @Override
    public String roleName() {
        return "ClusterInteractionAgent";
    }

    private void logError(final Throwable throwable)
    {
        LOGGER.error("Error: {}", throwable.getMessage());
    }

    public void send(String sessionId, String message) {
        final SimpleMessageEncoder encoder = new SimpleMessageEncoder()
            .wrapAndApplyHeader(sendBuffer, 0, new MessageHeaderEncoder());

        encoder.sessionId(sessionId);
        encoder.message(message);

        // TODO: this should not be here
        SleepingIdleStrategy idleStrategy = new SleepingIdleStrategy();

        int RETRY_COUNT = 3;
        int retries = 0;
        do
        {
            final long result = aeronCluster.offer(sendBuffer, 0, MessageHeaderEncoder.ENCODED_LENGTH + encoder.encodedLength());
            if (result > 0L)
            {
                GatewayStatistics.messagesSent++;
                LOGGER.info("Statistics: sent {}, received {}", GatewayStatistics.messagesSent, GatewayStatistics.messagesReceived);    
                return;
            }
            else if (result == Publication.BACK_PRESSURED)
            {
                LOGGER.warn("backpressure on session offer");
            }
            else if (result == Publication.ADMIN_ACTION)
            {
                LOGGER.warn("admin action on session offer");
            }
            else if (result == Publication.NOT_CONNECTED || result == Publication.MAX_POSITION_EXCEEDED)
            {
                LOGGER.error("unexpected state on session offer: {}", result);
                return;
            }

            idleStrategy.idle();
            retries += 1;
        }
        while (retries < RETRY_COUNT);
    }
}
