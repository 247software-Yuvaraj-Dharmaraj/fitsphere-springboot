package com.yuvaraj.fitsphere.realtime;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOServer;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Socket.IO server mirroring FitSphere's realtime layer: broadcasts live gym
 * "occupancy" snapshots and a "slots:changed" signal so clients refetch.
 */
@Component
public class RealtimeService {

    private static final Logger log = LoggerFactory.getLogger(RealtimeService.class);

    private final boolean enabled;
    private final int port;
    private final String clientOrigin;

    private SocketIOServer server;

    public RealtimeService(@Value("${app.socket.enabled}") boolean enabled,
                           @Value("${app.socket.port}") int port,
                           @Value("${app.client-origin}") String clientOrigin) {
        this.enabled = enabled;
        this.port = port;
        this.clientOrigin = clientOrigin;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void start() {
        if (!enabled || server != null) {
            return;
        }
        Configuration config = new Configuration();
        config.setPort(port);
        config.setOrigin(clientOrigin);
        try {
            server = new SocketIOServer(config);
            server.start();
            log.info("Socket.IO server started on port {}", port);
        } catch (Exception e) {
            log.warn("Socket.IO server failed to start: {}", e.getMessage());
            server = null;
        }
    }

    @PreDestroy
    public void stop() {
        if (server != null) {
            server.stop();
            server = null;
        }
    }

    public void emitOccupancy(Object snapshot) {
        if (server != null) {
            server.getBroadcastOperations().sendEvent("occupancy", snapshot);
        }
    }

    public void emitSlotsChanged() {
        if (server != null) {
            server.getBroadcastOperations().sendEvent("slots:changed");
        }
    }
}
