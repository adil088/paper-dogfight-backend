package com.paperdogfight.repository;

import com.paperdogfight.model.Game;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

/** Keeps games in a {@link ConcurrentHashMap}; everything is lost when the server stops. */
@Repository
public class InMemoryGameRepository implements GameRepository {

    private final Map<String, Game> games = new ConcurrentHashMap<>();

    @Override
    public void save(Game game) {
        games.put(game.getId(), game);
    }

    @Override
    public Optional<Game> findById(String id) {
        return Optional.ofNullable(games.get(id));
    }

    @Override
    public boolean existsById(String id) {
        return games.containsKey(id);
    }
}
