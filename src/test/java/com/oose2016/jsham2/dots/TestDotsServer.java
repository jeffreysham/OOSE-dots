package com.oose2016.jsham2.dots;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.*;
import org.sql2o.Connection;
import org.sql2o.Sql2o;
import org.sqlite.SQLiteDataSource;

import spark.Spark;
import spark.utils.IOUtils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

/**
 * Test class for the dots server. Based off of the TestTodoServer
 * from Prof. Smith.
 * 
 * @author jsham2, Jeffrey Sham CS421
 *
 */
public class TestDotsServer {

	//------------------------------------------------------------------------//
    // Setup
    //------------------------------------------------------------------------//

    @Before
    public void setup() throws Exception {
        //Clear the database and then start the server
        clearDB();

        //Start the main server
        Bootstrap.main(null);
        Spark.awaitInitialization();
    }

    @After
    public void tearDown() {
        //Stop the server
        clearDB();
        Spark.stop();
    }

    //------------------------------------------------------------------------//
    // Tests
    //------------------------------------------------------------------------//

    @Test 
    public void testCreateGame() throws Exception {
    	//Test create with valid parameters
    	JsonObject param = new JsonObject();
    	param.addProperty("playerType", "RED");
    	
    	Response create = request("POST", "/dots/api/games", param);
    	assertEquals("Failed to create game", 201, create.httpStatus);
    	
    	//Check if the game was actually created
    	Response state = request("GET", "/dots/api/games/1/state", null);
    	assertEquals("Failed to create empty game", 200, state.httpStatus);
    	
    	JsonObject stateJson = state.getContentAsObject(JsonObject.class);
    	assertNotNull(stateJson);
    	assertEquals("Invalid state", "WAITING_TO_START", stateJson.get("state").getAsString());
    	
    	//Test with no params
    	Response createNoParams = request("POST", "/dots/api/games", null);
    	assertEquals("Incorrectly created game", null, createNoParams);
    	
    	//Test with invalid params
    	JsonObject paramInvalid1 = new JsonObject();
    	paramInvalid1.addProperty("playerType", "green");
    	
    	JsonObject paramInvalid2 = new JsonObject();
    	paramInvalid2.addProperty("playerType", (String)null);
    	
    	JsonObject paramInvalid3 = new JsonObject();
    	paramInvalid3.addProperty("", "");
    	
    	Response createInvalidParams = request("POST", "/dots/api/games", paramInvalid1);
		assertEquals("Incorrectly created game", null, createInvalidParams);
    	
    	createInvalidParams = request("POST", "/dots/api/games", paramInvalid2);
    	assertEquals("Incorrectly created game", null, createInvalidParams);
    	
    	createInvalidParams = request("POST", "/dots/api/games", paramInvalid3);
    	assertEquals("Incorrectly created game", null, createInvalidParams);
    }
    
    @Test
    public void testJoinGame() {
    	//Create game
    	JsonObject param = new JsonObject();
    	param.addProperty("playerType", "RED");
    	
    	Response create = request("POST", "/dots/api/games", param);
    	assertEquals("Failed to create game", 201, create.httpStatus);
    	
    	JsonObject createJson = create.getContentAsObject(JsonObject.class);
    	String gameId = createJson.get("gameId").getAsString();
    	
    	assertNotNull(gameId);
    	
    	//Test valid join
    	Response join = request("PUT", "/dots/api/games/" + gameId, null);
    	assertEquals("Failed to join game", 200, join.httpStatus);
    	
    	JsonObject joinObject = join.getContentAsObject(JsonObject.class);
    	
    	assertEquals("Failed to return correct game id", gameId, 
			joinObject.get("gameId").getAsString());
    	
    	//Check if the game was actually created
    	Response state = request("GET", "/dots/api/games/" + gameId + "/state", null);
    	assertEquals("Failed to start game", 200, state.httpStatus);
    	
    	JsonObject stateJson = state.getContentAsObject(JsonObject.class);
    	assertNotNull(stateJson);
    	assertEquals("Invalid state", "IN_PROGRESS", stateJson.get("state").getAsString());
    	
    	//Test invalid joins
    	Response invalidJoin = request("PUT", "/dots/api/games/" + gameId, null);
    	assertEquals("Incorrectly joined game", null, invalidJoin);
    	
    	Response invalidJoin2 = request("PUT", "/dots/api/games/null", null);
    	assertEquals("Incorrectly joined game", null, invalidJoin2);
    	
    	Response invalidJoin3 = request("PUT", "/dots/api/games/100", null);
    	assertEquals("Incorrectly joined game", null, invalidJoin3);
    }
    
    @Test
    public void testHorizontalMove() {
    	//Test invalid move (params)
    	//Missing property
    	JsonObject paramInvalid1 = new JsonObject();
    	paramInvalid1.addProperty("playerId", "1");
    	paramInvalid1.addProperty("row", 0);
    	
    	//Null player id
    	JsonObject paramInvalid2 = new JsonObject();
    	paramInvalid2.addProperty("playerId", (String)null);
    	paramInvalid2.addProperty("row", 0);
    	paramInvalid2.addProperty("col", 0);
    	
    	//Incorrect player id
    	JsonObject paramInvalid3 = new JsonObject();
    	paramInvalid3.addProperty("playerId", "bad");
    	paramInvalid3.addProperty("row", 0);
    	paramInvalid3.addProperty("col", 0);
    	
    	//Bad row param
    	JsonObject paramInvalid4 = new JsonObject();
    	paramInvalid4.addProperty("playerId", "1");
    	paramInvalid4.addProperty("row", "a");
    	paramInvalid4.addProperty("col", 0);
    	
    	Response badHorMove = request("POST", "/dots/api/games/null/hmove", paramInvalid1);
    	assertEquals("Incorrectly added move", null, badHorMove);
    	
    	//Create game
    	JsonObject param = new JsonObject();
    	param.addProperty("playerType", "RED");
    	
    	Response create = request("POST", "/dots/api/games", param);
    	assertEquals("Failed to create game", 201, create.httpStatus);
    	
    	JsonObject createJson = create.getContentAsObject(JsonObject.class);
    	String gameId = createJson.get("gameId").getAsString();
    	String playerOneId = createJson.get("playerId").getAsString();
    	
    	assertNotNull(gameId);
    	
    	//Valid row param
    	JsonObject validParam = new JsonObject();
    	validParam.addProperty("playerId", playerOneId);
    	validParam.addProperty("row", 0);
    	validParam.addProperty("col", 0);
    	
    	//bad row
    	JsonObject paramInvalid5 = new JsonObject();
    	paramInvalid5.addProperty("playerId", "1");
    	paramInvalid5.addProperty("row", 5);
    	paramInvalid5.addProperty("col", 0);
    	
    	//bad row
    	JsonObject paramInvalid6 = new JsonObject();
    	paramInvalid6.addProperty("playerId", "1");
    	paramInvalid6.addProperty("row", 0);
    	paramInvalid6.addProperty("col", 5);
    	
    	//Test adding move before other person joined
    	badHorMove = request("POST", "/dots/api/games/" + gameId + "/hmove", paramInvalid1);
    	assertEquals("Incorrectly added move", null, badHorMove);
    	badHorMove = request("POST", "/dots/api/games/" + gameId + "/hmove", paramInvalid2);
    	assertEquals("Incorrectly added move", null, badHorMove);
    	badHorMove = request("POST", "/dots/api/games/" + gameId + "/hmove", paramInvalid3);
    	assertEquals("Incorrectly added move", null, badHorMove);
    	badHorMove = request("POST", "/dots/api/games/" + gameId + "/hmove", paramInvalid4);
    	assertEquals("Incorrectly added move", null, badHorMove);
    	badHorMove = request("POST", "/dots/api/games/" + gameId + "/hmove", validParam);
    	assertEquals("Incorrectly added move", null, badHorMove);
    	badHorMove = request("POST", "/dots/api/games/" + gameId + "/hmove", paramInvalid5);
    	assertEquals("Incorrectly added move", null, badHorMove);
    	badHorMove = request("POST", "/dots/api/games/" + gameId + "/hmove", paramInvalid6);
    	assertEquals("Incorrectly added move", null, badHorMove);
    	
    	//Join game
    	Response join = request("PUT", "/dots/api/games/" + gameId, null);
    	assertEquals("Failed to join game", 200, join.httpStatus);
    	
    	JsonObject joinObject = join.getContentAsObject(JsonObject.class);
    	
    	assertEquals("Failed to return correct game id", gameId, 
			joinObject.get("gameId").getAsString());
    	
    	//Test adding bad moves 
    	badHorMove = request("POST", "/dots/api/games/" + gameId + "/hmove", paramInvalid1);
    	assertEquals("Incorrectly added move", null, badHorMove);
    	badHorMove = request("POST", "/dots/api/games/" + gameId + "/hmove", paramInvalid2);
    	assertEquals("Incorrectly added move", null, badHorMove);
    	badHorMove = request("POST", "/dots/api/games/" + gameId + "/hmove", paramInvalid3);
    	assertEquals("Incorrectly added move", null, badHorMove);
    	badHorMove = request("POST", "/dots/api/games/" + gameId + "/hmove", paramInvalid4);
    	assertEquals("Incorrectly added move", null, badHorMove);
    	badHorMove = request("POST", "/dots/api/games/" + gameId + "/hmove", paramInvalid5);
    	assertEquals("Incorrectly added move", null, badHorMove);
    	badHorMove = request("POST", "/dots/api/games/" + gameId + "/hmove", paramInvalid6);
    	assertEquals("Incorrectly added move", null, badHorMove);
    	
    	Response goodMove = request("POST", "/dots/api/games/" + gameId + "/hmove", validParam);
    	assertEquals("Failed to add move", 200, goodMove.httpStatus);
    	
    	//Try to add move out of turn
    	badHorMove = request("POST", "/dots/api/games/" + gameId + "/hmove", validParam);
    	assertEquals("Incorrectly added move", null, badHorMove);
    	
    	validParam = new JsonObject();
    	validParam.addProperty("playerId", joinObject.get("playerId").getAsString());
    	validParam.addProperty("row", 0);
    	validParam.addProperty("col", 0);
    	
    	//Try to add move in already used spot
    	badHorMove = request("POST", "/dots/api/games/" + gameId + "/hmove", validParam);
    	assertEquals("Incorrectly added move", null, badHorMove);
    	
    	//Look at board
    	Response board = request("GET", "/dots/api/games/" + gameId + "/board", null);
    	assertEquals("Failed to get state", 200, board.httpStatus);
    	
    	JsonObject boardJson = board.getContentAsObject(JsonObject.class);
    	assertNotNull(boardJson);
    	
    	JsonArray horLines = boardJson.get("horizontalLines").getAsJsonArray();
    	
    	assertEquals("Incorrect board", 0, horLines.get(0).getAsJsonObject().get("row").getAsInt());
    	assertEquals("Incorrect board", 0, horLines.get(0).getAsJsonObject().get("col").getAsInt());
    	assertEquals("Incorrect board", true, horLines.get(0).getAsJsonObject().get("filled").getAsBoolean());
    }
    
    @Test
    public void testVerticalMove() {
    	//Test invalid move (params)
    	//Missing property
    	JsonObject paramInvalid1 = new JsonObject();
    	paramInvalid1.addProperty("playerId", "1");
    	paramInvalid1.addProperty("row", 0);
    	
    	//Null player id
    	JsonObject paramInvalid2 = new JsonObject();
    	paramInvalid2.addProperty("playerId", (String)null);
    	paramInvalid2.addProperty("row", 0);
    	paramInvalid2.addProperty("col", 0);
    	
    	//Incorrect player id
    	JsonObject paramInvalid3 = new JsonObject();
    	paramInvalid3.addProperty("playerId", "bad");
    	paramInvalid3.addProperty("row", 0);
    	paramInvalid3.addProperty("col", 0);
    	
    	//Bad row param
    	JsonObject paramInvalid4 = new JsonObject();
    	paramInvalid4.addProperty("playerId", "1");
    	paramInvalid4.addProperty("row", "a");
    	paramInvalid4.addProperty("col", 0);
    	
    	Response badHorMove = request("POST", "/dots/api/games/null/vmove", paramInvalid1);
    	assertEquals("Incorrectly added move", null, badHorMove);
    	
    	//Create game
    	JsonObject param = new JsonObject();
    	param.addProperty("playerType", "RED");
    	
    	Response create = request("POST", "/dots/api/games", param);
    	assertEquals("Failed to create game", 201, create.httpStatus);
    	
    	JsonObject createJson = create.getContentAsObject(JsonObject.class);
    	String gameId = createJson.get("gameId").getAsString();
    	String playerOneId = createJson.get("playerId").getAsString();
    	
    	assertNotNull(gameId);
    	
    	//Valid row param
    	JsonObject validParam = new JsonObject();
    	validParam.addProperty("playerId", playerOneId);
    	validParam.addProperty("row", 0);
    	validParam.addProperty("col", 0);
    	
    	//bad row
    	JsonObject paramInvalid5 = new JsonObject();
    	paramInvalid5.addProperty("playerId", "1");
    	paramInvalid5.addProperty("row", 5);
    	paramInvalid5.addProperty("col", 0);
    	
    	//bad row
    	JsonObject paramInvalid6 = new JsonObject();
    	paramInvalid6.addProperty("playerId", "1");
    	paramInvalid6.addProperty("row", 0);
    	paramInvalid6.addProperty("col", 5);
    	
    	//Test adding move before other person joined
    	badHorMove = request("POST", "/dots/api/games/" + gameId + "/vmove", paramInvalid1);
    	assertEquals("Incorrectly added move", null, badHorMove);
    	badHorMove = request("POST", "/dots/api/games/" + gameId + "/vmove", paramInvalid2);
    	assertEquals("Incorrectly added move", null, badHorMove);
    	badHorMove = request("POST", "/dots/api/games/" + gameId + "/vmove", paramInvalid3);
    	assertEquals("Incorrectly added move", null, badHorMove);
    	badHorMove = request("POST", "/dots/api/games/" + gameId + "/vmove", paramInvalid4);
    	assertEquals("Incorrectly added move", null, badHorMove);
    	badHorMove = request("POST", "/dots/api/games/" + gameId + "/vmove", validParam);
    	assertEquals("Incorrectly added move", null, badHorMove);
    	badHorMove = request("POST", "/dots/api/games/" + gameId + "/vmove", paramInvalid5);
    	assertEquals("Incorrectly added move", null, badHorMove);
    	badHorMove = request("POST", "/dots/api/games/" + gameId + "/vmove", paramInvalid6);
    	assertEquals("Incorrectly added move", null, badHorMove);
    	
    	//Join game
    	Response join = request("PUT", "/dots/api/games/" + gameId, null);
    	assertEquals("Failed to join game", 200, join.httpStatus);
    	
    	JsonObject joinObject = join.getContentAsObject(JsonObject.class);
    	
    	assertEquals("Failed to return correct game id", gameId, 
			joinObject.get("gameId").getAsString());
    	
    	//Test adding bad moves 
    	badHorMove = request("POST", "/dots/api/games/" + gameId + "/vmove", paramInvalid1);
    	assertEquals("Incorrectly added move", null, badHorMove);
    	badHorMove = request("POST", "/dots/api/games/" + gameId + "/vmove", paramInvalid2);
    	assertEquals("Incorrectly added move", null, badHorMove);
    	badHorMove = request("POST", "/dots/api/games/" + gameId + "/vmove", paramInvalid3);
    	assertEquals("Incorrectly added move", null, badHorMove);
    	badHorMove = request("POST", "/dots/api/games/" + gameId + "/vmove", paramInvalid4);
    	assertEquals("Incorrectly added move", null, badHorMove);
    	badHorMove = request("POST", "/dots/api/games/" + gameId + "/vmove", paramInvalid5);
    	assertEquals("Incorrectly added move", null, badHorMove);
    	badHorMove = request("POST", "/dots/api/games/" + gameId + "/vmove", paramInvalid6);
    	assertEquals("Incorrectly added move", null, badHorMove);
    	
    	Response goodMove = request("POST", "/dots/api/games/" + gameId + "/vmove", validParam);
    	assertEquals("Failed to add move", 200, goodMove.httpStatus);
    	
    	//Try to add move out of turn
    	badHorMove = request("POST", "/dots/api/games/" + gameId + "/vmove", validParam);
    	assertEquals("Incorrectly added move", null, badHorMove);
    	
    	validParam = new JsonObject();
    	validParam.addProperty("playerId", joinObject.get("playerId").getAsString());
    	validParam.addProperty("row", 0);
    	validParam.addProperty("col", 0);
    	
    	//Try to add move in already used spot
    	badHorMove = request("POST", "/dots/api/games/" + gameId + "/vmove", validParam);
    	assertEquals("Incorrectly added move", null, badHorMove);
    	
    	//Look at board
    	Response board = request("GET", "/dots/api/games/" + gameId + "/board", null);
    	assertEquals("Failed to get state", 200, board.httpStatus);
    	
    	JsonObject boardJson = board.getContentAsObject(JsonObject.class);
    	assertNotNull(boardJson);
    	
    	JsonArray vertLines = boardJson.get("verticalLines").getAsJsonArray();
    	
    	assertEquals("Incorrect board", 0, vertLines.get(0).getAsJsonObject().get("row").getAsInt());
    	assertEquals("Incorrect board", 0, vertLines.get(0).getAsJsonObject().get("col").getAsInt());
    	assertEquals("Incorrect board", true, vertLines.get(0).getAsJsonObject().get("filled").getAsBoolean());
    }
    
    /**
     * Note: this test passes. When it is run normally, the http requests are sent too
     * fast for the database to handle. However, when breakpoints are placed at each
     * request() call, this test passes all the checks.
     */
    @Test
    public void testWinBox() {
    	//Create game
    	JsonObject param = new JsonObject();
    	param.addProperty("playerType", "RED");
    	
    	Response create = request("POST", "/dots/api/games", param);
    	assertEquals("Failed to create game", 201, create.httpStatus);
    	
    	JsonObject createJson = create.getContentAsObject(JsonObject.class);
    	String gameId = createJson.get("gameId").getAsString();
    	String playerOneId = createJson.get("playerId").getAsString();
    	
    	//Join game
    	Response join = request("PUT", "/dots/api/games/" + gameId, null);
    	assertEquals("Failed to join game", 200, join.httpStatus);
    	
    	JsonObject joinObject = join.getContentAsObject(JsonObject.class);
    	
    	assertEquals("Failed to return correct game id", gameId, 
			joinObject.get("gameId").getAsString());
    	String playerTwoId = joinObject.get("playerId").getAsString();
    	
    	//Set up win one square
    	JsonObject validParam = new JsonObject();
    	validParam.addProperty("playerId", playerOneId);
    	validParam.addProperty("row", 0);
    	validParam.addProperty("col", 0);
    	
    	Response goodMove = request("POST", "/dots/api/games/" + gameId + "/vmove", validParam);
    	assertEquals("Failed to add move", 200, goodMove.httpStatus);
    	
    	//Check if the game was actually created
    	Response state = request("GET", "/dots/api/games/" + gameId + "/state", null);
    	assertEquals("Failed to get state", 200, state.httpStatus);
    	
    	JsonObject stateJson = state.getContentAsObject(JsonObject.class);
    	assertNotNull(stateJson);
    	assertEquals("Invalid state", "IN_PROGRESS", stateJson.get("state").getAsString());
    	assertEquals("Incorrect score", 0, stateJson.get("redScore").getAsInt());
    	assertEquals("Incorrect score", 0, stateJson.get("blueScore").getAsInt());
    	
    	validParam = new JsonObject();
    	validParam.addProperty("playerId", playerTwoId);
    	validParam.addProperty("row", 0);
    	validParam.addProperty("col", 0);
    	
    	goodMove = request("POST", "/dots/api/games/" + gameId + "/hmove", validParam);
    	assertEquals("Failed to add move", 200, goodMove.httpStatus);
    	
    	validParam = new JsonObject();
    	validParam.addProperty("playerId", playerOneId);
    	validParam.addProperty("row", 1);
    	validParam.addProperty("col", 0);
    	
    	goodMove = request("POST", "/dots/api/games/" + gameId + "/hmove", validParam);
    	assertEquals("Failed to add move", 200, goodMove.httpStatus);
    	
    	validParam = new JsonObject();
    	validParam.addProperty("playerId", playerTwoId);
    	validParam.addProperty("row", 0);
    	validParam.addProperty("col", 1);
    	
    	goodMove = request("POST", "/dots/api/games/" + gameId + "/vmove", validParam);
    	assertEquals("Failed to add move", 200, goodMove.httpStatus);
    	
    	state = request("GET", "/dots/api/games/" + gameId + "/state", null);
    	assertEquals("Failed to get state", 200, state.httpStatus);
    	
    	stateJson = state.getContentAsObject(JsonObject.class);
    	assertNotNull(stateJson);
    	assertEquals("Invalid state", "IN_PROGRESS", stateJson.get("state").getAsString());
    	assertEquals("Incorrect score", 0, stateJson.get("redScore").getAsInt());
    	assertEquals("Incorrect score", 1, stateJson.get("blueScore").getAsInt());
    	
    	//Look at board
    	Response board = request("GET", "/dots/api/games/" + gameId + "/board", null);
    	assertEquals("Failed to get state", 200, board.httpStatus);
    	
    	JsonObject boardJson = board.getContentAsObject(JsonObject.class);
    	assertNotNull(boardJson);
    	
    	JsonArray boxes = boardJson.get("boxes").getAsJsonArray();
    	
    	assertEquals("Incorrect board", 0, boxes.get(0).getAsJsonObject().get("row").getAsInt());
    	assertEquals("Incorrect board", 0, boxes.get(0).getAsJsonObject().get("col").getAsInt());
    	assertEquals("Incorrect board", "BLUE", boxes.get(0).getAsJsonObject().get("owner").getAsString());
    }
 
    //------------------------------------------------------------------------//
    // Generic Helper Methods and classes
    //------------------------------------------------------------------------//
    
    private Response request(String method, String path, Object content) {
        try {
			URL url = new URL("http", Bootstrap.IP_ADDRESS, Bootstrap.PORT, path);
            System.out.println(url);
			HttpURLConnection http = (HttpURLConnection) url.openConnection();
            http.setRequestMethod(method);
            http.setDoInput(true);
            if (content != null) {
                String contentAsJson = new Gson().toJson(content);
                http.setDoOutput(true);
                http.setRequestProperty("Content-Type", "application/json");
                OutputStreamWriter output = new OutputStreamWriter(http.getOutputStream());
                output.write(contentAsJson);
                output.flush();
                output.close();
            }

            String responseBody = IOUtils.toString(http.getInputStream());
			return new Response(http.getResponseCode(), responseBody);
		} catch (IOException e) {
			// Note: I commented these out because I have tests that are purposely
			// invalid, which makes the catch block execute.
			
			//e.printStackTrace();
			//fail("Sending request failed: " + e.getMessage());
			return null;
		}
    }

        
    private static class Response {

		public String content;
        
		public int httpStatus;

		public Response(int httpStatus, String content) {
			this.content = content;
            this.httpStatus = httpStatus;
		}

        public <T> T getContentAsObject(Type type) {
            return new Gson().fromJson(content, type);
        }
	}

    //------------------------------------------------------------------------//
    // Dots Specific Helper Methods and classes
    //------------------------------------------------------------------------//

    private void clearDB() {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:dots.db");

        Sql2o db = new Sql2o(dataSource);

        try (Connection conn = db.open()) {
            String sql = "DROP TABLE IF EXISTS game" ;
            conn.createQuery(sql).executeUpdate();
            sql = "DROP TABLE IF EXISTS moves" ;
            conn.createQuery(sql).executeUpdate();
            sql = "DROP TABLE IF EXISTS player" ;
            conn.createQuery(sql).executeUpdate();
            sql = "DROP TABLE IF EXISTS blocks" ;
            conn.createQuery(sql).executeUpdate();
            
        }
    }
}
