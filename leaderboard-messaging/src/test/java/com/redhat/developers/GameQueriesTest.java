/*-
 * #%L
 * Leaderboard SQL
 * %%
 * Copyright (C) 2020 Red Hat Inc.,
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.redhat.developers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import com.redhat.developers.data.Game;
import com.redhat.developers.data.GameState;
import com.redhat.developers.sql.GameQueries;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

/**
 * GameQueriesTest
 */
@QuarkusTest
@QuarkusTestResource(QuarkusTestEnv.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class GameQueriesTest {

  @Inject
  GameQueries gameQueries;

  @Order(1)
  @Test
  public void testAdd() throws Exception {
    Game game = Game.newGame()
        .gameId("saveTest001")
        .state(GameState.byCode(1))
        .configuration("{}");

    long pk = gameQueries.upsert(game);
    assertEquals(1, pk);
  }

  @Order(2)
  @Test
  public void testFindAll() throws Exception {
    Game game = Game.newGame()
        .pk(1)
        .gameId("saveTest001")
        .state(GameState.byCode(1))
        .configuration("{}");

    List<Game> games = gameQueries.findAll();
    assertNotNull(games);
    assertTrue(games.size() == 1);
    Game actualGame = games.get(0);
    assertNotNull(actualGame);
    assertEquals(game.getPk(), actualGame.getPk());
    assertEquals(game.getState(), actualGame.getState());
    assertEquals(game.getGameId(), actualGame.getGameId());
    assertEquals(game.getConfiguration(), actualGame.getConfiguration());
    assertNotNull(actualGame.getDate());
  }

  @Order(3)
  @Test
  public void testFindById() throws Exception {
    Game game = Game.newGame()
        .pk(1)
        .gameId("saveTest001")
        .state(GameState.byCode(1))
        .configuration("{}");

    Optional<Game> optGame = gameQueries.findById(1);
    assertTrue(optGame.isPresent());
    Game actualGame = optGame.get();
    assertNotNull(actualGame);
    assertEquals(game.getPk(), actualGame.getPk());
    assertEquals(game.getState(), actualGame.getState());
    assertEquals(game.getGameId(), actualGame.getGameId());
    assertEquals(game.getConfiguration(), actualGame.getConfiguration());
    assertNotNull(actualGame.getDate());
  }

  @Order(4)
  @Test
  public void testUpsert() throws Exception {
    Game game = Game.newGame()
        .pk(1)
        .gameId("saveTest001")
        .state(GameState.byCode(2))
        .configuration("{}");

    long pk = gameQueries.upsert(game);
    assertEquals(1, pk);

    // Query
    Optional<Game> optGame = gameQueries.findById(1);
    assertTrue(optGame.isPresent());
    Game actualGame = optGame.get();
    assertNotNull(actualGame);
    assertEquals(1, actualGame.getPk());
    assertEquals(GameState.byCode(2), actualGame.getState());
    assertEquals("saveTest001", actualGame.getGameId());
  }

  @Order(5)
  @Test
  public void testActiveGame() throws Exception {
    Game game3 = Game.newGame()
        .pk(3)
        .gameId("saveTest003")
        .state(GameState.byCode(1))
        .configuration("{}");

    Game game4 = Game.newGame()
        .pk(4)
        .gameId("saveTest004")
        .state(GameState.byCode(1))
        .configuration("{}");

    long pk = gameQueries.upsert(game3);
    assertEquals(pk, 3);

    pk = gameQueries.upsert(game4);
    assertEquals(pk, 4);

    // Query
    Optional<Game> optGame = gameQueries.findActiveGame();
    assertTrue(optGame.isPresent());
    Game actualGame = optGame.get();
    assertNotNull(actualGame);
    assertEquals(4, actualGame.getPk());
    assertEquals(GameState.byCode(1), actualGame.getState());
    assertEquals("saveTest004", actualGame.getGameId());
  }

  @Order(6)
  @Test
  public void testDelete() throws Exception {
    Boolean isDeleted = gameQueries.delete(1);
    assertTrue(isDeleted);
    // delete other rows as well
    gameQueries.delete(2);
    gameQueries.delete(3);
    gameQueries.delete(4);
  }

  @Order(7)
  @Test
  public void testNotFound() throws Exception {
    Optional<Game> og = gameQueries.findById(1);
    assertFalse(og.isPresent());
  }
}
