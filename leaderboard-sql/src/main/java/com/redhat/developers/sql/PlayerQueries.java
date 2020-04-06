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
package com.redhat.developers.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import com.redhat.developers.data.Avatar;
import com.redhat.developers.data.GameTotal;
import com.redhat.developers.data.Player;

/**
 * PlayerQueries
 */
@ApplicationScoped
public class PlayerQueries {

  static Logger logger = Logger.getLogger(PlayerQueries.class.getName());

  @Inject
  Jsonb jsonb;


  public Optional<Player> findById(Connection dbConn, long id) {
    Player player = null;
    try {
      PreparedStatement pst =
          dbConn.prepareStatement("SELECT * from players where id=?");
      pst.setLong(1, id);
      ResultSet rs = pst.executeQuery();

      if (rs.next()) {
        player = from(rs);
      } else {

      }
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Error Finding player with id " + id,
          e);
    }
    return Optional.ofNullable(player);

  }

  /**
   * 
   * @param dbConn
   * @param rowCount
   * @return
   */
  public List<Player> rankPlayers(Connection dbConn,
      int rowCount) {
    ArrayList<Player> players = new ArrayList<>();
    try {
      PreparedStatement pst =
          dbConn.prepareStatement("SELECT p.* FROM players p"
              + " WHERE p.game_id=(SELECT g.game_id from games g ORDER BY g.game_date DESC FETCH FIRST 1 ROW ONLY)"
              + " ORDER BY p.guess_score DESC,"
              + " p.guess_right DESC,"
              + " p.guess_wrong ASC"
              + " FETCH FIRST ? ROW ONLY");
      pst.setLong(1, rowCount);
      ResultSet rs = pst.executeQuery();
      return this.playersList(rs);
    } catch (SQLException e) {
      logger.log(Level.SEVERE,
          "Error ranking players for game", e);
    }
    return players;
  }

  /**
   * 
   * @param dbConn
   * @return
   */
  public Optional<GameTotal> gameTotals(Connection dbConn) {
    GameTotal gameTotal = null;
    try {
      PreparedStatement pst =
          dbConn.prepareStatement("SELECT COUNT(*) as total_players,"
              + " SUM(p.guess_right) as total_guesses,"
              + " SUM(p.guess_score) as total_dollars"
              + " FROM players p"
              + " WHERE p.game_id=(SELECT g.game_id from games g ORDER BY g.game_date DESC FETCH FIRST 1 ROW ONLY)");
      ResultSet rs = pst.executeQuery();

      if (rs.next()) {
        gameTotal = gameTotal(rs);
      } else {

      }
    } catch (SQLException e) {
      logger.log(Level.SEVERE,
          "Error getting game totals for game", e);
    }
    return Optional.ofNullable(gameTotal);
  }


  /**
   * 
   * @param dbConn
   * @param player
   * @return
   */
  public Boolean upsert(Connection dbConn, Player player) {
    logger.info("Upserting player with id " + player.getPk());
    try {
      PreparedStatement pst = dbConn.prepareStatement("INSERT INTO players"
          + "(player_id,player_name,guess_right,"
          + "guess_wrong,guess_score,creation_server,"
          + "game_server,scoring_server,"
          + "player_avatar,game_id)"
          + " VALUES (?,?,?,?,?,?,?,?,?::JSON,?)"
          + " ON CONFLICT (player_id) WHERE game_id=?"
          + "  DO "
          + "   UPDATE  set "
          + "     player_name=?,guess_right=?,"
          + "     guess_wrong=?,guess_score=?,"
          + "     creation_server=?,game_server=?,"
          + "     scoring_server=?,player_avatar=?::JSON");
      playerParams(pst, player);
      int rowCount = pst.executeUpdate();
      return rowCount == 1;
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Error Inserting " + player.getPk(), e);
    }
    return false;
  }

  /**
   * 
   * @param dbConn
   * @param id
   * @return
   */
  public Boolean delete(Connection dbConn, long id) {

    try {
      PreparedStatement pst =
          dbConn.prepareStatement("DELETE FROM players WHERE id=?");
      pst.setLong(1, id);
      int rowCount = pst.executeUpdate();
      return rowCount == 1;
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Error Finding player with id " + id,
          e);
    }
    return false;

  }

  /**
   * 
   * @param row
   * @return
   */
  private Player from(ResultSet row) throws SQLException {
    Player player = Player.newPlayer()
        .pk(row.getLong("id"))
        .playerId(row.getString("player_id"))
        .username(row.getString("player_name"))
        .right(row.getInt("guess_right"))
        .wrong(row.getInt("guess_wrong"))
        .score(row.getInt("guess_score"))
        .creationServer(row.getString("creation_server"))
        .scoringServer(row.getString("scoring_server"))
        .gameServer(row.getString("game_server"))
        .avatar(jsonb.fromJson(row.getString("player_avatar"), Avatar.class))
        .gameId(row.getString("game_id"));
    return player;
  }

  /**
   * 
   * @param row
   * @return
   */
  private GameTotal gameTotal(ResultSet row) throws SQLException {
    GameTotal gameTotal = GameTotal.newGameTotal()
        .totalPlayers(row.getLong("total_players"))
        .totalDollars(row.getLong("total_dollars"))
        .totalGuesses(row.getLong("total_guesses"));
    return gameTotal;
  }

  /**
   * 
   * @param rs
   * @return
   * @throws SQLException
   */
  private List<Player> playersList(ResultSet rs) throws SQLException {
    List<Player> listOfPlayers = new ArrayList<>();
    while (rs.next()) {
      listOfPlayers.add(from(rs));
    }
    return listOfPlayers;
  }

  /**
   * 
   * @param pst
   * @param player
   * @throws SQLException
   */
  private void playerParams(PreparedStatement pst, Player player)
      throws SQLException {
    pst.setString(1, player.getPlayerId());// Param Order 1
    pst.setString(2, player.getUsername()); // Param Order 2
    pst.setInt(3, player.getRight()); // Param Order 3
    pst.setInt(4, player.getWrong()); // Param Order 4
    pst.setInt(5, player.getScore()); // Param Order 5
    pst.setString(6, player.getCreationServer()); // Param Order 6
    pst.setString(7, player.getGameServer());// Param Order 7
    pst.setString(8, player.getScoringServer()); // Param Order 8
    pst.setString(9, jsonb.toJson(player.getAvatar())); // Param Order 9
    pst.setString(10, player.getGameId()); // Param Order 10


    // TODO better way ?? UPDATE indexes
    pst.setString(11, player.getGameId()); // Param Order 11
    pst.setString(12, player.getUsername()); // Param Order 12
    pst.setInt(13, player.getRight()); // Param Order 13
    pst.setInt(14, player.getWrong()); // Param Order 14
    pst.setInt(15, player.getScore()); // Param Order 15
    pst.setString(16, player.getCreationServer()); // Param Order 16
    pst.setString(17, player.getGameServer());// Param Order 17
    pst.setString(18, player.getScoringServer()); // Param Order 18
    pst.setString(19, jsonb.toJson(player.getAvatar())); // Param Order 19

  }
}
