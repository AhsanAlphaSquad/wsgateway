package example.micronaut;

import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.SleepingMillisIdleStrategy;

import io.micronaut.runtime.Micronaut;

public class Application {

    @FunctionalInterface
    public interface OnMessageReceived {
        void received(String sessionId, String message);        
    }

    public static void main(String[] args) {

        final IdleStrategy idleStrategy = new SleepingMillisIdleStrategy();

        final ClusterInteractionAgent clusterInteractionAgent = new ClusterInteractionAgent((sessionId ,message) -> {
            WSGateway gateway = WSGateway.getInstance();
            if(gateway != null)
            {
                gateway.received(sessionId, message);
            }
        });
        
        final AgentRunner runner = new AgentRunner(idleStrategy, Throwable::printStackTrace, null, clusterInteractionAgent);
        AgentRunner.startOnThread(runner);

        Micronaut.run(Application.class, args);
    }

}
