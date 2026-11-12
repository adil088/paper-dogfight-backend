package com.paperdogfight.websocket;

import com.paperdogfight.dto.GameStateDto;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/** Pushes the full game state to everyone subscribed to {@code /topic/games/{id}}. */
@Component
public class GameBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    public GameBroadcaster(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publish(GameStateDto state) {
        messagingTemplate.convertAndSend("/topic/games/" + state.id(), state);
    }
}
