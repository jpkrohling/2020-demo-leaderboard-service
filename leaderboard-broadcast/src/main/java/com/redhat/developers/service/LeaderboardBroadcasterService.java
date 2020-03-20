package com.redhat.developers.service;

import static java.util.logging.Level.FINE;
import static java.util.logging.Level.SEVERE;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import com.redhat.developers.sql.PlayerQueries;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import io.smallrye.mutiny.Multi;
import io.vertx.axle.pgclient.PgPool;

/**
 * LeaderboardBroadcasterService
 */
@ApplicationScoped
public class LeaderboardBroadcasterService {

  Logger logger =
      Logger.getLogger(LeaderboardBroadcasterService.class.getName());

  @Inject
  Jsonb jsonb;

  @Inject
  PgPool client;

  @Inject
  PlayerQueries playerQueries;

  @ConfigProperty(name = "rhdemo.leaderboard-broadcast.rowCount")
  int rowCount;

  @ConfigProperty(name = "rhdemo.leaderboard-broadcast.tickInterval")
  int tickInterval;

  @Outgoing("leaderboard-broadcast")
  public Multi<String> broadcastLeaderboard() {
    return Multi.createFrom()
        .ticks().every(Duration.ofSeconds(tickInterval))
        .on().overflow().drop()
        .onItem().produceCompletionStage(this::rankedPlayerList).concatenate();
  }

  /**
   * 
   * @param tick
   * @return
   */
  public CompletionStage<String> rankedPlayerList(long tick) {
    logger.log(FINE, "Sending message for tick {0} ", tick);
    String playersList = jsonb.toJson(Collections.<String>emptyList());
    AtomicReference<String> playersJson = new AtomicReference<>();
    playersJson.set(playersList);
    return playerQueries
        .rankPlayers(client, rowCount)
        .thenApply(p -> {
          try {
            String strJson = jsonb.toJson(p);
            playersJson.set(strJson);
            logger.log(FINE, "Broadcast Leaderboard data {0} ",
                strJson);
          } catch (Exception e) {
            logger.log(SEVERE, "Error building players json", e);
          }
          return playersJson.get();
        })
        .exceptionally(e -> {
          logger.log(SEVERE, "Error retreiving player ranks ", e);
          return playersJson.get();
        });
  }
}