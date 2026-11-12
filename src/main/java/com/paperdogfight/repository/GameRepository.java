package com.paperdogfight.repository;

import com.paperdogfight.model.Game;
import java.util.Optional;

/** Storage abstraction for games, so a database can replace the in-memory map later. */
public interface GameRepository {
    void save(Game game);

    Optional<Game> findById(String id);

    boolean existsById(String id);
}
