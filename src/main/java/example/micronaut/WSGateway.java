package example.micronaut;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micronaut.websocket.WebSocketSession;
import io.micronaut.websocket.annotation.OnClose;
import io.micronaut.websocket.annotation.OnMessage;
import io.micronaut.websocket.annotation.OnOpen;
import io.micronaut.websocket.annotation.ServerWebSocket;

@ServerWebSocket("/ws")
public class WSGateway {

    public class GatewayStatistics
    {
        public static int messagesSent = 0;
        public static int messagesReceived = 0;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(WSGateway.class);
    private static WSGateway instance = null;

    HashMap<String, WebSocketSession> sessions = new HashMap<>();

    WSGateway()
    {
        instance = this;
    }

    public static WSGateway getInstance() {
        return instance;
    }

    @OnOpen
    public void onOpen(WebSocketSession session) {
        LOGGER.info("Client connected {}", session.getId());
    }

    @OnMessage
    public void onMessage(WebSocketSession session, String message) {
        // is this checking needed?
        String sessionId = session.getId();
        GatewayStatistics.messagesReceived++;
        sessions.computeIfAbsent(sessionId, k -> session);
//        LOGGER.info("Sent SessionId: {}, Message: {}", sessionId, message);
        ClusterInteractionAgent.getInstance().send(sessionId, message);
    }

    @OnClose
    public void onClose(WebSocketSession session) {
        LOGGER.info("Client disconnected");
        sessions.remove(session.getId());
    }

    public void received(String sessionId, String message) {
        // is this checking needed?
//        LOGGER.info("Received SessionId: {}, Message: {}", sessionId, message);
        if(sessions.containsKey(sessionId))
        {
            WebSocketSession session = sessions.get(sessionId);
            if(session.isOpen())
            {
                session.sendAsync(message);
            }
        }
        else
        {
            LOGGER.info("Session not found: {}", sessionId);
            // crash everything
        }
    }

}
