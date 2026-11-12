package com.paperdogfight.controller;

import com.paperdogfight.dto.GameStateDto;
import com.paperdogfight.dto.MoveRequest;
import com.paperdogfight.dto.SessionDto;
import com.paperdogfight.service.ErrorCode;
import com.paperdogfight.service.GameRuleException;
import com.paperdogfight.service.GameService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST API. Roll, move and rematch identify the player by the {@code X-Player-Token} header. */
@RestController
@RequestMapping("/api/games")
public class GameController {

    private static final String TOKEN_HEADER = "X-Player-Token";

    private final GameService service;

    public GameController(GameService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<SessionDto> create() {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create());
    }

    @PostMapping("/{id}/join")
    public SessionDto join(@PathVariable String id) {
        return service.join(id);
    }

    @GetMapping("/{id}")
    public GameStateDto get(@PathVariable String id) {
        return service.get(id);
    }

    @PostMapping("/{id}/roll")
    public GameStateDto roll(@PathVariable String id,
                             @RequestHeader(value = TOKEN_HEADER, required = false) String token) {
        return service.roll(id, token);
    }

    @PostMapping("/{id}/move")
    public GameStateDto move(@PathVariable String id,
                             @RequestHeader(value = TOKEN_HEADER, required = false) String token,
                             @RequestBody(required = false) MoveRequest request) {
        if (request == null || request.jetId() == null || request.jetId().isBlank()) {
            throw new GameRuleException(ErrorCode.INVALID_JET, "Body must contain a jetId.");
        }
        return service.move(id, token, request.jetId());
    }

    @PostMapping("/{id}/rematch")
    public GameStateDto rematch(@PathVariable String id,
                                @RequestHeader(value = TOKEN_HEADER, required = false) String token) {
        return service.rematch(id, token);
    }
}
