/*-
 * #%L
 * Leaderboard API
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
package com.redhat.developers.api;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.inject.Named;
import javax.json.bind.Jsonb;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import com.redhat.developers.data.GameTotal;
import com.redhat.developers.data.Player;
import com.redhat.developers.model.Leaderboard;
import com.redhat.developers.sql.PlayerQueries;

/**
 * LeaderBoardSource TODO: Auth
 */
@Path("/api")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class LeaderBoardResource {

  private Logger logger = Logger.getLogger(LeaderBoardResource.class.getName());

  @Inject
  Jsonb jsonb;

  @Inject
  PlayerQueries playerQueries;

  @GET
  @Path("leaderboard")
  public Response getLeaderBoard(
      @QueryParam("rowCount") String qRowCount) {
    logger.log(Level.INFO, "Getting Ranked {0} player(s) for game ", qRowCount);
    int rowCount = qRowCount != null ? Integer.parseInt(qRowCount) : 10;
    List<Player> leaders = playerQueries.rankPlayers(rowCount);
    logger.log(Level.INFO, "Got Ranked {0} player(s) for game ",
        leaders.size());
    Optional<GameTotal> gameTotals = playerQueries.gameTotals();
    logger.log(Level.INFO, "Game Totals ",
        gameTotals.isPresent());
    Leaderboard leaderboard = new Leaderboard();
    leaderboard.setLeaders(leaders);
    if (gameTotals.isPresent()) {
      GameTotal gameTotal = gameTotals.get();
      leaderboard.setDollars(gameTotal.getTotalDollars());
      leaderboard.setRights(gameTotal.getTotalRights());
      leaderboard.setWrongs(gameTotal.getTotalWrongs());
      leaderboard.setPlayers(gameTotal.getTotalPlayers());
    }
    logger.log(Level.INFO, "Leaderboard",
        jsonb.toJson(leaderboard));
    return Response.ok().entity(leaderboard).build();
  }

}
