package com.oose2016.jsham2.dots;

import com.google.gson.JsonObject;

import java.util.Collections;
import java.util.Map;

import static spark.Spark.*;

/**
 * This class is the main controller class for the dots game. 
 * It holds all of the end points that can be called through the REST API.
 * 
 * @author jsham2, Jeffrey Sham CS421
 *
 */
public class DotsController {

	// The base path
	private static final String API_CONTEXT = "/dots/api/games";

	// The database service
	private final DotsService dotsService;
	
	/**
	 * The constructor that setups service and the end points
	 * @param dotsService the database service
	 */
	public DotsController(DotsService dotsService) {
		this.dotsService = dotsService;
		setupEndpoints();
	}
	
	/**
	 * This method sets up all the REST end points for the game.
	 */
	private void setupEndpoints() {
		post(API_CONTEXT, "application/json", (request, response) -> {
			try {
				Game game = dotsService.createGame(request.body());
				if (game != null) {
					response.status(201);
					JsonObject json = new JsonObject();
					json.addProperty("gameId", game.getId());
					json.addProperty("playerId", game.getPlayerOne().getId());
					json.addProperty("playerType", game.getPlayerOne().getType());
					return json;
				} else {
					response.status(400);
					JsonObject json = new JsonObject();
					json.addProperty("error", "Could not create game.");
					return json;
				}
			} catch (DotsService.DotsServiceException ex) {
				if (ex.getMessage().equals("400")) {
					response.status(Integer.valueOf(ex.getMessage()));
					JsonObject json = new JsonObject();
					json.addProperty("error", "Could not create game.");
					return json;
				}
			}
			
			return Collections.EMPTY_MAP;
		}, new JsonTransformer());
		
		put(API_CONTEXT + "/:gameId", "application/json", (request, response) -> {
			try {
				Game game = dotsService.joinGame(request.params("gameId"));
				response.status(200);
				JsonObject json = new JsonObject();
				json.addProperty("gameId", game.getId());
				json.addProperty("playerId", game.getPlayerTwo().getId());
				json.addProperty("playerType", game.getPlayerTwo().getType());
				return json;
			} catch (DotsService.DotsServiceException ex) {
				JsonObject json = new JsonObject();
				if (ex.getMessage().equals("404") || ex.getMessage().equals("410")) {
					response.status(Integer.valueOf(ex.getMessage()));
					json.addProperty("error", ex.getMessage().equals("404") ? 
							"Invalid game ID" : "Player already joined / game full");
					return json;
				} else if (ex.getMessage().equals("400")) {
					response.status(Integer.valueOf(ex.getMessage()));
					json.addProperty("error", "Could not join game.");
					return json;
				}
			}
			
			return Collections.EMPTY_MAP;
		}, new JsonTransformer());
		
		post(API_CONTEXT + "/:gameId/hmove", "application/json", (request, response) -> {
			try {
				dotsService.validateMove(request.params("gameId"), request.body(), "HOR");
				response.status(200);
			} catch (DotsService.DotsServiceException ex) {
				JsonObject json = new JsonObject();
				if (ex.getMessage().equals("404") || ex.getMessage().equals("422")) {
					response.status(Integer.valueOf(ex.getMessage()));
					json.addProperty("error", ex.getMessage().equals("404") ? 
							"Invalid game or player ID" : "Incorrect turn or illegal move");
					return json;
				} else if (ex.getMessage().equals("400")) {
					response.status(Integer.valueOf(ex.getMessage()));
					json.addProperty("error", "Could not make horizontal move.");
					return json;
				}
			}
			return Collections.EMPTY_MAP;
		}, new JsonTransformer());
		
		post(API_CONTEXT + "/:gameId/vmove", "application/json", (request, response) -> {
			try {
				dotsService.validateMove(request.params("gameId"), request.body(), "VERT");
				response.status(200);
			} catch (DotsService.DotsServiceException ex) {
				JsonObject json = new JsonObject();
				if (ex.getMessage().equals("404") || ex.getMessage().equals("422")) {
					response.status(Integer.valueOf(ex.getMessage()));
					json.addProperty("error", ex.getMessage().equals("404") ? 
							"Invalid game or player ID" : "Incorrect turn or illegal move");
					return json;
				} else if (ex.getMessage().equals("400")) {
					response.status(Integer.valueOf(ex.getMessage()));
					json.addProperty("error", "Could not make vertical move.");
					return json;
				}
			}
			return Collections.EMPTY_MAP;
		}, new JsonTransformer());
		
		get(API_CONTEXT + "/:gameId/state", "application/json", (request, response) -> {
			try {
				Map<String, Object> result = dotsService.getGameState(request.params("gameId"));
				
				if (!result.containsKey("redScore")) {
					result.put("redScore", 0);
				}
				
				if (!result.containsKey("blueScore")) {
					result.put("blueScore", 0);
				}
				
				if (!result.containsKey("state")) {
					result.put("state", "WAITING_TO_START");
				}
				
				if (!result.containsKey("whoseTurn")) {
					result.put("whoseTurn", "RED");
				}
				response.status(200);
				return result;
			} catch (DotsService.DotsServiceException ex) {
				JsonObject json = new JsonObject();
				if (ex.getMessage().equals("404")) {
					response.status(Integer.valueOf(ex.getMessage()));
					json.addProperty("error", "Invalid game ID");
					return json;
				} else if (ex.getMessage().equals("400")) {
					response.status(Integer.valueOf(ex.getMessage()));
					json.addProperty("error", "Could not get game state.");
					return json;
				}
			}
			return Collections.EMPTY_MAP;
		}, new JsonTransformer());
		
		get(API_CONTEXT + "/:gameId/board", "application/json", (request, response) -> {
			try {
				Map<String, Object> result = dotsService.getBoard(request.params("gameId"));
				response.status(200);
				return result;
			} catch (DotsService.DotsServiceException ex) {
				JsonObject json = new JsonObject();
				if (ex.getMessage().equals("404")) {
					response.status(Integer.valueOf(ex.getMessage()));
					json.addProperty("error", "Invalid game ID");
					return json;
				} else if (ex.getMessage().equals("400")) {
					response.status(Integer.valueOf(ex.getMessage()));
					json.addProperty("error", "Could not get game state.");
					return json;
				}
			}
			return Collections.EMPTY_MAP;
		}, new JsonTransformer());
	}
	
}
