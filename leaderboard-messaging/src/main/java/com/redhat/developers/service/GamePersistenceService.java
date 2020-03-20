package com.redhat.developers.service;

import java.util.logging.Logger;
import static java.util.logging.Level.*;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import com.redhat.developers.data.Game;
import com.redhat.developers.data.GameStateBody;
import com.redhat.developers.data.GameStateMessage;
import com.redhat.developers.sql.GameQueries;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import io.vertx.axle.pgclient.PgPool;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

/**
 * GamePersistenceService
 */
@ApplicationScoped
public class GamePersistenceService {

  Logger logger = Logger.getLogger(GamePersistenceService.class.getName());

  @Inject
  Jsonb jsonb;

  @Inject
  PgPool client;

  @Inject
  GameQueries gameQueries;

  @Incoming("game-state")
  public void saveGame(Map<String, Object> map) {
    JsonObject jsonObject = new JsonObject(map);
    String payload = Json.encode(jsonObject);
    logger.log(FINE, "Received Game State Payload  {0} ", payload);
    GameStateMessage gameState =
        jsonb.fromJson(payload, GameStateMessage.class);
    GameStateBody body = gameState.getBody();
    Game game = body.getGame();
    logger.log(FINE, "Saving game {0} ", game.getId());
    gameQueries.upsert(client, game)
        .whenComplete((v, e) -> {
          if (e != null) {
            logger.log(SEVERE, "Error while saving game ", e);
          } else {
            logger.log(INFO, "Game {0} saved successfully", game.id);
          }
        });
  }

}
