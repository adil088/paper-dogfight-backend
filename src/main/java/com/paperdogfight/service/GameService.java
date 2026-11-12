package com.paperdogfight.service;

import com.paperdogfight.config.GameRulesConfig;
import com.paperdogfight.dto.GameStateDto;
import com.paperdogfight.dto.SessionDto;
import com.paperdogfight.model.Game;
import com.paperdogfight.model.GameStatus;
import com.paperdogfight.model.Jet;
import com.paperdogfight.model.Move;
import com.paperdogfight.model.Team;
import com.paperdogfight.repository.GameRepository;
import com.paperdogfight.websocket.GameBroadcaster;
import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Application layer: authenticates players, runs the {@link GameEngine} under a per-game lock,
 * stores the result and broadcasts the new state.
 */
@Service
public class GameService {

    /** No 0/O/1/I so codes are easy to read out loud. */
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 5;

    private final GameRepository repository;
    private final GameRulesConfig rules;
    private final GameBroadcaster broadcaster;
    private final GameEngine engine;
    private final SecureRandom random = new SecureRandom();

    public GameService(GameRepository repository, GameRulesConfig rules, GameBroadcaster broadcaster) {
        this.repository = repository;
        this.rules = rules;
        this.broadcaster = broadcaster;
        this.engine = new GameEngine(rules, DieRoller.secure());
    }

    /** Creates a game; the creator plays Red. */
    public SessionDto create() {
        Game game = new Game(newCode());
        game.setRedToken(newToken());
        game.touch();
        repository.save(game);
        return new SessionDto(game.getId(), Team.RED, game.getRedToken(), view(game, "CREATED"));
    }

    /** Second player joins as Blue; the game starts and Red moves first. */
    public SessionDto join(String id) {
        Game game = find(id);
        synchronized (game) {
            if (game.getStatus() != GameStatus.WAITING) {
                throw new GameRuleException(ErrorCode.GAME_FULL, "This game already has two players.");
            }
            game.setBlueToken(newToken());
            engine.begin(game);
            GameStateDto state = commit(game, "JOINED");
            return new SessionDto(game.getId(), Team.BLUE, game.getBlueToken(), state);
        }
    }

    public GameStateDto get(String id) {
        Game game = find(id);
        synchronized (game) {
            return view(game, "STATE");
        }
    }

    public GameStateDto roll(String id, String token) {
        Game game = find(id);
        synchronized (game) {
            Team team = authenticate(game, token);
            engine.roll(game, team);
            return commit(game, "ROLL");
        }
    }

    public GameStateDto move(String id, String token, String jetId) {
        Game game = find(id);
        synchronized (game) {
            Team team = authenticate(game, token);
            Move move = engine.move(game, team, jetId);
            String event = game.getStatus() == GameStatus.FINISHED ? "GAME_OVER" : move.hit() ? "HIT" : "MOVE";
            return commit(game, event);
        }
    }

    public GameStateDto rematch(String id, String token) {
        Game game = find(id);
        synchronized (game) {
            Team team = authenticate(game, token);
            boolean reset = engine.requestRematch(game, team);
            return commit(game, reset ? "REMATCH_STARTED" : "REMATCH_REQUESTED");
        }
    }

    // ------------------------------------------------------------------ helpers

    private Game find(String id) {
        return repository.findById(id.toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new GameRuleException(ErrorCode.GAME_NOT_FOUND, "Game " + id + " was not found."));
    }

    private Team authenticate(Game game, String token) {
        if (token == null || token.isBlank()) {
            throw new GameRuleException(ErrorCode.INVALID_TOKEN, "Missing X-Player-Token header.");
        }
        if (token.equals(game.getRedToken())) {
            return Team.RED;
        }
        if (token.equals(game.getBlueToken())) {
            return Team.BLUE;
        }
        throw new GameRuleException(ErrorCode.INVALID_TOKEN, "You are not a player in this game.");
    }

    /** Bumps the version, stores the game, broadcasts and returns the new state. */
    private GameStateDto commit(Game game, String event) {
        game.touch();
        repository.save(game);
        GameStateDto state = view(game, event);
        broadcaster.publish(state);
        return state;
    }

    private GameStateDto view(Game game, String event) {
        List<String> selectable = game.getStatus() == GameStatus.IN_PROGRESS && game.getPendingRoll() != null
                ? engine.selectableJets(game, game.getCurrentTurn()).stream().map(Jet::getId).toList()
                : List.of();
        return GameStateDto.from(game, rules, selectable, event);
    }

    private String newCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                sb.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
            }
            code = sb.toString();
        } while (repository.existsById(code));
        return code;
    }

    private String newToken() {
        return UUID.randomUUID().toString();
    }
}
