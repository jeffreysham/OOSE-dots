package com.oose2016.jsham2.dots;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;
import org.sql2o.Sql2o;
import org.sql2o.Sql2oException;
import org.sql2o.data.Row;
import org.sql2o.data.Table;

import javax.sql.DataSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class holds all the game logic and database calls for the dots game.
 * It also performs all of the game actions.
 * 
 * @author jsham2, Jeffrey Sham CS421
 *
 */
public class DotsService {
	// Database to hold all the objects
	private Sql2o database;
	
	// Logger
	private final Logger logger = LoggerFactory.getLogger(DotsService.class);
	
	// Upper limit for grid
	public static final int GRID_UPPER_SIZE = 5;
	
	// Smaller side for grid
	public static final int GRID_MID_SIZE = 4;
	
	/**
	 * This is the constructor for the service. It sets up all the
	 * database tables.
	 * @param dataSource The data source for the database
	 * @throws DotsServiceException 
	 */
	public DotsService(DataSource dataSource) throws DotsServiceException {
		database = new Sql2o(dataSource);
		
		try (Connection conn = database.open()) {
			String gameSql = "CREATE TABLE IF NOT EXISTS game (id INTEGER PRIMARY KEY AUTOINCREMENT, " +
							 "							  	  player_one_id INTEGER, " +
						     "							      player_two_id INTEGER, " +
							 "							      state TEXT);";
			
			String movesSql = "CREATE TABLE IF NOT EXISTS moves (game_id INTEGER, " +
							  " 								player_id INTEGER, " + 
							  "									type TEXT, " + 
							  " 								row INTEGER, " +
							  " 								col INTEGER, " + 
							  "									won_box INTEGER, " +	
							  "									time TIMESTAMP DEFAULT CURRENT_TIMESTAMP);";
			
			String playerSql = "CREATE TABLE IF NOT EXISTS player (id INTEGER PRIMARY KEY AUTOINCREMENT, " +
							   " 								  type TEXT, " +
							   " 								  score INTEGER);";
			
			String boardSql = "CREATE TABLE IF NOT EXISTS blocks (game_id INTEGER, " +
							  " 								 color TEXT, " + 
							  "  								 row INTEGER, " +
							  "									 col INTEGER);";
			
			conn.createQuery(gameSql).executeUpdate();
			conn.createQuery(movesSql).executeUpdate();
			conn.createQuery(playerSql).executeUpdate();
			conn.createQuery(boardSql).executeUpdate();
		} catch (Sql2oException ex) {
			logger.error("Failed to create table", ex);
			throw new DotsServiceException("Failed to create table", ex);
		}
	}
	
	/**
	 * This method takes in a request body and creates a
	 * new player and a new game.
	 * 
	 * @param body request body
	 * @return The new Game object
	 * @throws DotsServiceException
	 */
	public Game createGame(String body) throws DotsServiceException {
		// Get body info, create player
		JsonObject json = null;
		try {
			json = new Gson().fromJson(body, JsonObject.class);
		} catch (Exception ex) {
			// Bad parameters
			throw new DotsServiceException("400", null);
		}

		String playerType = null;
		
		if (json.has("playerType") && !json.get("playerType").isJsonNull()) {
			playerType = json.get("playerType").getAsString();
			playerType = playerType.toUpperCase();
			if (!(playerType.equals("RED") || playerType.equals("BLUE"))) {
				// Wrong type
				throw new DotsServiceException("400", null);
			}
		} else {
			throw new DotsServiceException("400", null);
		}
		
		String createPlayerSql = "INSERT INTO player (type, score) " +
								 " 				VALUES (:playerType, 0);";
		
		try (Connection conn = database.open()) {
			Object playerKey = conn.createQuery(createPlayerSql)
				.addParameter("playerType", playerType)
				.executeUpdate()
				.getKey();
			if (playerKey != null) {
				int playerId = (int) playerKey;
				
				// Create game
				String createGameSql = "INSERT INTO game (player_one_id, state) " +
									   "  			VALUES (:playerId, 'WAITING_TO_START');";
				Object gameKey = conn.createQuery(createGameSql)
									.addParameter("playerId", playerId)
									.executeUpdate()
									.getKey();
				if (gameKey != null) {
					int gameId = (int) gameKey;
					
					// Return game
					Player player = new Player(String.valueOf(playerId), playerType);
					Game game = new Game(String.valueOf(gameId), player, null);
					return game;
				}
			}
		} catch (Exception ex) {
			logger.error("DotsService.createGame: Failed to create game", ex);
			throw new DotsServiceException("DotsService.createGame: Failed to create game", ex);
		}
		
		return null;
	}
	
	/**
	 * This method takes a game id as a parameter and
	 * attempts to join that game. It returns the game
	 * object with the newly created player 2.
	 * 
	 * @param gameId The game id
	 * @return the Game with Player 2
	 * @throws DotsServiceException
	 */
	public Game joinGame(String gameId) throws DotsServiceException {
		if (gameId == null || gameId.equals("null") || !isNumeric(gameId)) {
			// Bad game parameter
			throw new DotsServiceException("400", null);
		}
		
		// Get the game
		String gameSql = "SELECT g.id, g.player_two_id, p.type FROM game g, player p " + 
						 " 		   WHERE g.id = :gameId " + 
						 "		   		AND g.player_one_id = p.id;";
		
		try (Connection conn = database.open()) {
			Table gameTable = conn.createQuery(gameSql)
								.addParameter("gameId", Integer.valueOf(gameId))
								.executeAndFetchTable();
			if (!gameTable.rows().isEmpty()) {
				Row gameRow = gameTable.rows().get(0);
				
				if (gameRow.getInteger("player_two_id") != null) {
					// Game full
					throw new DotsServiceException("410", null);
				}
				
				// Get first player's type
				String playerOneType = gameRow.getString("type");
				
				String playerTwoType = "RED";
				if (playerOneType.equals(playerTwoType)) {
					playerTwoType = "BLUE";
				}
				
				// Insert second player
				String createPlayerTwoSql = "INSERT INTO player (type, score) " +
						 				 " 				VALUES (:playerType, 0);";
				Object playerKey = conn.createQuery(createPlayerTwoSql)
						.addParameter("playerType", playerTwoType)
						.executeUpdate()
						.getKey();
				
				if (playerKey != null) {
					int playerId = (int) playerKey;
					
					// Update and return game
					String updateGameSql = "UPDATE game SET player_two_id = :playerId, state = 'IN_PROGRESS' " + 
										   "			WHERE id = :id;";
					conn.createQuery(updateGameSql)
						.addParameter("playerId", playerId)
						.addParameter("id", gameId)
						.executeUpdate();
					Player playerTwo = new Player(String.valueOf(playerId), playerTwoType);
					return new Game(gameId, null, playerTwo);
				}
			} else {
				// Invalid Game Id
				throw new DotsServiceException("404", null);
			}
		}
		
		return null;
	}
	
	
	
	
	/**
	 * This method validates if a move will be valid. It uses
	 * a lot of game logic and adds the move to the board
	 * if all the tests pass.
	 * @param gameId The game id
	 * @param body The json request body
	 * @throws DotsServiceException
	 */
	public void validateMove(String gameId, String body, String moveType) 
			throws DotsServiceException {
		// Get body info, create player
		JsonObject json = null;
		try {
			json = new Gson().fromJson(body, JsonObject.class);
		} catch (Exception ex) {
			// Bad parameters
			throw new DotsServiceException("400", null);
		}
		
		if (gameId == null || gameId.equals("null") || !isNumeric(gameId) || 
				!json.has("playerId") || !json.has("row") || !json.has("col")) {
			// Bad parameters
			throw new DotsServiceException("400", null);
		}
		
		String playerIdString = null;

		if (!json.get("playerId").isJsonNull()) {
			playerIdString = json.get("playerId").getAsString();
		} 
		
		int row = 0;
		int col = 0;
		
		try {
			row = json.get("row").getAsInt();
			col = json.get("col").getAsInt();
		} catch (Exception ex) {
			// Bad parameters
			throw new DotsServiceException("400", ex);
		}
		
		if (playerIdString == null || !isNumeric(playerIdString)) {
			// Invalid Player Id or Invalid Game Id
			throw new DotsServiceException("404", null);
		}
		
		int playerId = Integer.valueOf(playerIdString);
		
		// Get the game
		String gameSql = "SELECT * FROM game g " + 
						 " 		   WHERE g.id = :gameId;";
		
		try (Connection conn = database.open()) {
			Table gameTable = conn.createQuery(gameSql)
								  .addParameter("gameId", Integer.valueOf(gameId))
								  .executeAndFetchTable();
			if (!gameTable.rows().isEmpty()) {
				Row gameRow = gameTable.rows().get(0);
				String state = gameRow.getString("state");
				if (!state.equals("IN_PROGRESS")) {
					// Illegal Move.
					throw new DotsServiceException("422", null);
				}
				
				if (gameRow.getInteger("player_one_id") != playerId) {
					if (gameRow.getInteger("player_two_id") != playerId) {
						// Invalid Player Id
						throw new DotsServiceException("404", null);
					}
				}
				
				String playerSql = "SELECT type FROM player " +
								   " 			WHERE id = :id " +
								   "			LIMIT 1;";
				List<Row> playerRows = conn.createQuery(playerSql)
										   .addParameter("id", playerId)
										   .executeAndFetchTable()
										   .rows();
				
				String playerOneType = "RED";
				
				if (!playerRows.isEmpty()) {
					playerOneType = playerRows.get(0).getString("type");
				}
				
				boolean playerTurn = false;
				
				if (playerOneType.equals("RED")) {
					playerTurn = isPlayersTurn(playerId, gameId, true);
				} else if (playerOneType.equals("BLUE")) {
					playerTurn = isPlayersTurn(playerId, gameId, false);
				}
				
				if (!playerTurn) {
					// Incorrect turn / not this player's turn
					throw new DotsServiceException("422", null);
				}
				
				Game game = new Game(gameId, null, null);
				fillBoard(game);
				
				if (moveType.equals("HOR")) {
					addHorizontalMove(row, col, game, playerId, playerOneType);
				} else {
					addVerticalMove(row, col, game, playerId, playerOneType);
				}
			} else {
				// Invalid Game Id
				throw new DotsServiceException("404", null);
			}
		}
	}
	
	/**
	 * This method adds the horizontal move if the row and column are within the bounds
	 * @param row the row
	 * @param col the col
	 * @param game the game object
	 * @param playerId the player's id
	 * @param playerOneType the color of the player
	 * @throws DotsServiceException
	 */
	private void addHorizontalMove(int row, int col, Game game, int playerId, String playerOneType) 
			throws DotsServiceException {
		if (row >= 0 && row < GRID_UPPER_SIZE && col >= 0 && col < GRID_MID_SIZE) {
			if (game.getHorizontalGrid()[row][col]) {
				// Illegal Move.
				throw new DotsServiceException("422", null);
			} else {
				addMove("HOR", row, col, game, playerId, playerOneType);
			}
		} else {
			// Illegal Move.
			throw new DotsServiceException("422", null);
		}
	}
	
	/**
	 * This method adds the vertical move if the row and column are within the bounds
	 * @param row the row
	 * @param col the col
	 * @param game the game object
	 * @param playerId the player's id
	 * @param playerOneType the color of the player
	 * @throws DotsServiceException
	 */
	private void addVerticalMove(int row, int col, Game game, int playerId, String playerOneType) 
			throws DotsServiceException {
		if (row >= 0 && row < GRID_MID_SIZE && col >= 0 && col < GRID_UPPER_SIZE) {
			if (game.getVerticalGrid()[row][col]) {
				// Illegal Move.
				throw new DotsServiceException("422", null);
			} else {
				addMove("VERT", row, col, game, playerId, playerOneType);
			}
		} else {
			// Illegal Move.
			throw new DotsServiceException("422", null);
		}
	}
	
	/**
	 * This method checks if it is a specific player's turn.
	 * @param playerId the player id
	 * @param gameId the game id
	 * @param isPlayerRed is the player red
	 * @return true if it is the player's turn, false otherwise
	 * @throws DotsServiceException
	 */
	private boolean isPlayersTurn(int playerId, String gameId, boolean isPlayerRed) 
			throws DotsServiceException {
		String sql = "SELECT * FROM moves " +
					 " 		   WHERE game_id = :gameId " +
					 "		   ORDER BY time DESC " +
					 "		   LIMIT 1;";
		
		try (Connection conn = database.open()) {
			Table movesTable = conn.createQuery(sql)
					  .addParameter("gameId", Integer.valueOf(gameId))
					  .executeAndFetchTable();
			if (movesTable.rows().isEmpty()) {
				return isPlayerRed;
			} else {
				Row move = movesTable.rows().get(0);
				if (playerId == move.getInteger("player_id") && move.getInteger("won_box") == 1) {
					// Player played last, they won a box.
					return true;
				} else if (playerId != move.getInteger("player_id") && move.getInteger("won_box") == 1) {
					// Other player played last, they won a box.
					return false;
				} else if (playerId == move.getInteger("player_id")){
					// Player played last, did not win a box.
					return false;
				} else if (playerId != move.getInteger("player_id")) {
					// Other player played last, did not win a box.
					return true;
				}
			}
		} catch (Exception ex) {
			logger.error("DotsService.isPlayerTurn: Failed to determine turn", ex);
			throw new DotsServiceException("DotsService.isPlayerTurn: Failed to determine turn", ex);
		}
		
		return false;
	}
	
	/**
	 * This method gets the current board and puts the information in a hash map.
	 * @param gameId the game id
	 * @return a hash map of all the board information
	 * @throws DotsServiceException
	 */
	public Map<String, Object> getBoard(String gameId) throws DotsServiceException {
		if (gameId == null || gameId.equals("null") || !isNumeric(gameId)) {
			// Bad game parameter
			throw new DotsServiceException("400", null);
		}
		
		Map<String, Object> boardMap = new HashMap<>();
		
		List<Map<String, Object> > horLines = new ArrayList<>();
		List<Map<String, Object> > vertLines = new ArrayList<>();
		List<Map<String, Object> > boxes = new ArrayList<>();
		
		// Get the game
		String gameSql = "SELECT * FROM game g " + 
						 " 		   WHERE g.id = :gameId;";
		try (Connection conn = database.open()) {
			List<Row> gameRows = conn.createQuery(gameSql)
									  .addParameter("gameId", Integer.valueOf(gameId))
									  .executeAndFetchTable()
									  .rows();
			if (!gameRows.isEmpty()) {
				Game game = new Game(gameId, null, null);
				fillBoard(game);
				
				boolean[][] horGrid = game.getHorizontalGrid();
				boolean[][] vertGrid = game.getVerticalGrid();
				String[][] gameBoxes = game.getBoxes();
				
				for (int i = 0; i < horGrid.length; i++) {
					for (int j = 0; j < horGrid[0].length; j++) {
						boolean filled = horGrid[i][j];
						Map<String, Object> tempMap = new HashMap<>();
						tempMap.put("row", i);
						tempMap.put("col", j);
						tempMap.put("filled", filled);
						horLines.add(tempMap);
					}
				}
				boardMap.put("horizontalLines", horLines);
				
				for (int i = 0; i < vertGrid.length; i++) {
					for (int j = 0; j < vertGrid[0].length; j++) {
						boolean filled = vertGrid[i][j];
						Map<String, Object> tempMap = new HashMap<>();
						tempMap.put("row", i);
						tempMap.put("col", j);
						tempMap.put("filled", filled);
						vertLines.add(tempMap);
					}
				}
				boardMap.put("verticalLines", vertLines);
				
				for (int i = 0; i < gameBoxes.length; i++) {
					for (int j = 0; j < gameBoxes[0].length; j++) {
						String color = gameBoxes[i][j];
						Map<String, Object> tempMap = new HashMap<>();
						tempMap.put("row", i);
						tempMap.put("col", j);
						
						if (color != null) {
							tempMap.put("owner", color);
						} else {
							tempMap.put("owner", "NONE");
						}
						
						boxes.add(tempMap);
					}
				}
				boardMap.put("boxes", boxes);
				
				return boardMap;
			} else {
				throw new DotsServiceException("404", null);
			}
		}
	}
	
	/**
	 * This method gives the state of the game at any moment.
	 * @param gameId the game id
	 * @return a map of the game state information
	 * @throws DotsServiceException
	 */
	public Map<String, Object> getGameState(String gameId) throws DotsServiceException {
		if (gameId == null || gameId.equals("null") || !isNumeric(gameId)) {
			// Bad game parameter
			throw new DotsServiceException("400", null);
		}
		
		Map<String, Object> stateMap = new HashMap<>();
		
		// Get the game
		String gameSql = "SELECT * FROM game g " + 
						 " 		   WHERE g.id = :gameId;";
		
		try (Connection conn = database.open()) {
			List<Row> gameRows = conn.createQuery(gameSql)
								  .addParameter("gameId", Integer.valueOf(gameId))
								  .executeAndFetchTable()
								  .rows();
			if (!gameRows.isEmpty()) {
				Row gameRow = gameRows.get(0);
				
				stateMap.put("state", gameRow.getString("state"));
				
				String playerSql = "SELECT * FROM player " +
									"		  WHERE id = :id;";
				List<Row> playerOneRows = conn.createQuery(playerSql)
											  .addParameter("id", gameRow.getInteger("player_one_id"))
											  .executeAndFetchTable()
											  .rows();
				List<Row> playerTwoRows = conn.createQuery(playerSql)
						  .addParameter("id", gameRow.getInteger("player_two_id"))
						  .executeAndFetchTable()
						  .rows();
				
				if (!playerOneRows.isEmpty()) {
					Row playerOne = playerOneRows.get(0);
					boolean isPlayerTurn = false;
					if (playerOne.getString("type").equals("RED")) {
						stateMap.put("redScore", playerOne.getInteger("score"));
						isPlayerTurn = isPlayersTurn(playerOne.getInteger("id"), gameId, true);
						stateMap.put("whoseTurn", isPlayerTurn ? "RED" : "BLUE");
					} else {
						stateMap.put("blueScore", playerOne.getInteger("score"));
						isPlayerTurn = isPlayersTurn(playerOne.getInteger("id"), gameId, false);
						stateMap.put("whoseTurn", isPlayerTurn ? "BLUE" : "RED");
					}
				}
				
				if (!playerTwoRows.isEmpty()) {
					Row playerTwo = playerTwoRows.get(0);
					if (playerTwo.getString("type").equals("RED")) {
						stateMap.put("redScore", playerTwo.getInteger("score"));
					} else {
						stateMap.put("blueScore", playerTwo.getInteger("score"));
					}
				}
				
				if (gameRow.getString("state").equals("FINISHED")) {
					stateMap.put("whoseTurn", "FINISHED");
				}
				
				return stateMap;
			} else {
				throw new DotsServiceException("404", null);
			}
		}
	}
	
	/**
	 * This method actually adds the move to the database and updates the database
	 * if boxes are won.
	 * 
	 * @param type the type of move
	 * @param row the row
	 * @param col the col
	 * @param game the game object
	 * @param playerId the player id
	 * @param playerType the player's color
	 * @throws DotsServiceException
	 */
	private void addMove(String type, int row, int col, Game game, int playerId, String playerType) 
			throws DotsServiceException {
		// Create move
		String createMoveSql = "INSERT INTO moves (game_id, player_id, type, row, col, won_box) " +
							   "  			VALUES (:gameId, :playerId, :type, :row, :col, :wonBox);";
		
		Map<String, Integer> winBoxes = willWinBox(game, type, row, col);
		try (Connection conn = database.open()) {
			if (winBoxes == null) {
				// Did not win boxes 
				conn.createQuery(createMoveSql)
					.addParameter("gameId", Integer.valueOf(game.getId()))
					.addParameter("playerId", playerId)
					.addParameter("type", type)
					.addParameter("row", row)
					.addParameter("col", col)
					.addParameter("wonBox", 0)
					.executeUpdate();
			} else {
				// Won either 1 or 2 boxes
				conn.createQuery(createMoveSql)
					.addParameter("gameId", Integer.valueOf(game.getId()))
					.addParameter("playerId", playerId)
					.addParameter("type", type)
					.addParameter("row", row)
					.addParameter("col", col)
					.addParameter("wonBox", 1)
					.executeUpdate();
				
				String boxSql = "INSERT INTO blocks (game_id, color, row, col) " +
								" 			 VALUES (:gameId, :color, :row, :col);";	
				
				String scoreSql = "UPDATE player SET score = score + :score " + 
						   				   "			WHERE id = :id;";
				
				conn.createQuery(boxSql)
					.addParameter("gameId", game.getId())
					.addParameter("color", playerType)
					.addParameter("row", winBoxes.get("row1"))
					.addParameter("col", winBoxes.get("col1"))
					.executeUpdate();
				
				if (winBoxes.size() == 4) {
					conn.createQuery(boxSql)
						.addParameter("gameId", game.getId())
						.addParameter("color", playerType)
						.addParameter("row", winBoxes.get("row2"))
						.addParameter("col", winBoxes.get("col2"))
						.executeUpdate();
					conn.createQuery(scoreSql)
						.addParameter("score", 2)
						.addParameter("id", playerId)
						.executeUpdate();
				} else {
					conn.createQuery(scoreSql)
						.addParameter("score", 1)
						.addParameter("id", playerId)
						.executeUpdate();
				}
				
				// Check if the move ended the game.
				String gameBlocksSql = "SELECT * FROM blocks " +
						 			"		   WHERE game_id = :gameId;";
				List<Row> gameBlocks = conn.createQuery(gameBlocksSql)
											.addParameter("gameId", Integer.valueOf(game.getId()))
											.executeAndFetchTable()
											.rows();
				if (gameBlocks.size() == 16) {
					String gameEndSql = "UPDATE game SET state = 'FINISHED' " + 
							   			"			WHERE id = :id;";
					conn.createQuery(gameEndSql)
						.addParameter("id", Integer.valueOf(game.getId()))
						.executeUpdate();
				}
			}
		} catch (Exception ex) {
			logger.error("DotsService.addMove: Failed to add move", ex);
			throw new DotsServiceException("DotsService.addMove: Failed to add move", ex);
		}
		
	}
	
	/**
	 * This method checks if a move will win a box(es). It fills a map with rows and cols
	 * if the move will win a box(es).
	 *  
	 * @param game the game object
	 * @param type the type of move
	 * @param row the row
	 * @param col the col
	 * @return a map of row and cols if the move will win a box, null otherwise
	 */
	private Map<String, Integer> willWinBox(Game game, String type, int row, int col) {
		Map<String, Integer> box = new HashMap<>();
		boolean[][] horGrid = game.getHorizontalGrid();
		boolean[][] vertGrid = game.getVerticalGrid();
		if (type.equals("HOR")) {
			if (row == 0) {
				// Only check bottom
				boolean botResult = willWinBotHorizontal(horGrid, vertGrid, row, col);
				if (botResult) {
					box.put("row1", row);
					box.put("col1", col);
					return box;
				} else {
					return null;
				}
			} else if (row == horGrid[0].length) {
				// Only check top
				boolean topResult = willWinTopHorizontal(horGrid, vertGrid, row, col);
				if (topResult) {
					box.put("row1", row - 1);
					box.put("col1", col);
					return box;
				} else {
					return null;
				}
			} else {
				// Check top and bottom
				boolean botResult = willWinBotHorizontal(horGrid, vertGrid, row, col);
				boolean topResult = willWinTopHorizontal(horGrid, vertGrid, row, col);
				
				if (botResult && topResult) {
					// Will win 2 boxes
					box.put("row1", row);
					box.put("col1", col);
					box.put("row2", row - 1);
					box.put("col2", col);
				} else if (botResult) {
					box.put("row1", row);
					box.put("col1", col);
				} else if (topResult) {
					box.put("row1", row - 1);
					box.put("col1", col);
				} else {
					box = null;
				}
				return box;
			}
		} else {
			if (col == 0) {
				// Only check right
				boolean rightResult = willWinRightVertical(horGrid, vertGrid, row, col);
				if (rightResult) {
					box.put("row1", row);
					box.put("col1", col);
					return box;
				} else {
					return null;
				}
			} else if (col == vertGrid.length) {
				// Only check left
				boolean leftResult = willWinLeftVertical(horGrid, vertGrid, row, col);
				if (leftResult) {
					box.put("row1", row);
					box.put("col1", col - 1);
					return box;
				} else {
					return null;
				}
			} else {
				// Check both left and right
				boolean rightResult = willWinRightVertical(horGrid, vertGrid, row, col);
				boolean leftResult = willWinLeftVertical(horGrid, vertGrid, row, col);
				
				if (rightResult && leftResult) {
					box.put("row1", row);
					box.put("col1", col);
					box.put("row2", row);
					box.put("col2", col - 1);
				} else if (rightResult) {
					box.put("row1", row);
					box.put("col1", col);
				} else if (leftResult) {
					box.put("row1", row);
					box.put("col1", col - 1);
				} else {
					box = null;
				}
				return box;
			}
		}
	}
	
	/**
	 * This method checks if a vertical move will win a box to its left
	 * @param horGrid the horizontal grid
	 * @param vertGrid the vertical grid
	 * @param row the row
	 * @param col the col
	 * @return true if won, false otherwise
	 */
	private boolean willWinLeftVertical(boolean[][] horGrid, boolean[][] vertGrid, int row, int col) {
		boolean topHor = horGrid[row][col - 1];
		boolean leftVert = vertGrid[row][col - 1];
		boolean botHor = horGrid[row + 1][col - 1];
		return topHor && leftVert && botHor;
	}
	
	/**
	 * The method checks if a vertical move will win a box to its right
	 * @param horGrid the horizontal grid
	 * @param vertGrid the vertical grid
	 * @param row the row
	 * @param col the col
	 * @return true if won, false otherwise
	 */
	private boolean willWinRightVertical(boolean[][] horGrid, boolean[][] vertGrid, int row, int col) {
		boolean topHor = horGrid[row][col];
		boolean rightVert = vertGrid[row][col + 1];
		boolean botHor = horGrid[row + 1][col];
		return topHor && rightVert && botHor;
	}
	
	/**
	 * The method checks if a horizontal move will win a box below it
	 * @param horGrid the horizontal grid
	 * @param vertGrid the vertical grid
	 * @param row the row
	 * @param col the col
	 * @return true if won, false otherwise
	 */
	private boolean willWinBotHorizontal(boolean[][] horGrid, boolean[][] vertGrid, int row, int col) {
		boolean leftVert = vertGrid[row][col];
		boolean lowerHor = horGrid[row + 1][col];
		boolean rightVert = vertGrid[row][col + 1];
		return leftVert && lowerHor && rightVert;
	}
	
	/**
	 * The method checks if a horizontal move will win a box above it
	 * @param horGrid the horizontal grid
	 * @param vertGrid the vertical grid
	 * @param row the row
	 * @param col the col
	 * @return true if won, false otherwise
	 */
	private boolean willWinTopHorizontal(boolean[][] horGrid, boolean[][] vertGrid, int row, int col) {
		boolean leftVert = vertGrid[row - 1][col];
		boolean topHor = horGrid[row - 1][col];
		boolean rightVert = vertGrid[row - 1][col + 1];
		return leftVert && topHor && rightVert;
	}
	
	/**
	 * This method fills the game board will all of the moves that have happened in the game
	 * @param game the game object
	 * @return the game object with all of the arrays filled
	 * @throws DotsServiceException
	 */
	private Game fillBoard(Game game) throws DotsServiceException {
		String sql = "SELECT * FROM blocks " +
					 "		   WHERE game_id = :gameId;";
		
		try (Connection conn = database.open()) {
			List<Row> gameTable = conn.createQuery(sql)
								  .addParameter("gameId", Integer.valueOf(game.getId()))
								  .executeAndFetchTable()
								  .rows();
			String[][] boxes = new String[4][4];
			for (Row block : gameTable) {
				boxes[block.getInteger("row")][block.getInteger("col")] = block.getString("color");
			}
			
			game.setBoxes(boxes);
			
			String gridSql = "SELECT * FROM moves " +
							 " 		   WHERE game_id = :gameId " +
							 "		    AND type = :type;";
			
			List<Row> horizontalTable = conn.createQuery(gridSql)
										.addParameter("gameId", Integer.valueOf(game.getId()))
										.addParameter("type", "HOR")
										.executeAndFetchTable()
										.rows();
			boolean[][] horGrid = new boolean[5][4];
			for (Row row : horizontalTable) {
				horGrid[row.getInteger("row")][row.getInteger("col")] = true;
			}
			
			game.setHorizontalGrid(horGrid);
	
			List<Row> verticalTable = conn.createQuery(gridSql)
										.addParameter("gameId", Integer.valueOf(game.getId()))
										.addParameter("type", "VERT")
										.executeAndFetchTable()
										.rows();
			boolean[][] vertGrid = new boolean[4][5];
			for (Row row : verticalTable) {
				vertGrid[row.getInteger("row")][row.getInteger("col")] = true;
			}
			
			game.setVerticalGrid(vertGrid);
	
			return game;
		} catch (Exception ex) {
			logger.error("DotsService.fillBoard: Failed to fill board", ex);
			throw new DotsServiceException("DotsService.fillBoard: Failed to fill board", ex);
		}
	}
	
	//-------------------------------//
	// Helper Classes and Methods
	//-------------------------------//
	
	/**
	 * This a custom exception class.
	 * @author jsham2, Jeffrey Sham CS421
	 *
	 */
	public static class DotsServiceException extends Exception {
		public DotsServiceException(String message, Throwable cause) {
			super(message, cause);
		}
	}
	
	private static boolean isNumeric(String value) {
		try {
			Integer.valueOf(value);
			return true;
		} catch (NumberFormatException ex) {
			return false;
		}
	}
}
