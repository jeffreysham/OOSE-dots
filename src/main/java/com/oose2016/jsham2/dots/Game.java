package com.oose2016.jsham2.dots;

/**
 * This the Game object for the dots game.
 * It holds most of the information (players and grid)
 * 
 * @author jsham2, Jeffrey Sham CS421
 */
public class Game {
	private String id;
	private Player playerOne;
	private Player playerTwo;
	private boolean[][] horizontalGrid;
	private boolean[][] verticalGrid;
	private String[][] boxes;
	
	public Game(String id, Player playerOne, Player playerTwo) {
		this.id = id;
		this.playerOne = playerOne;
		this.playerTwo = playerTwo;
		this.horizontalGrid = new boolean[5][4];
		this.verticalGrid = new boolean[4][5];
		this.boxes = new String[4][4];
	}

	public String getId() {
		return id;
	}

	public Player getPlayerOne() {
		return playerOne;
	}

	public void setPlayerOne(Player playerOne) {
		this.playerOne = playerOne;
	}

	public Player getPlayerTwo() {
		return playerTwo;
	}

	public void setPlayerTwo(Player playerTwo) {
		this.playerTwo = playerTwo;
	}

	public boolean[][] getHorizontalGrid() {
		return horizontalGrid;
	}

	public void setHorizontalGrid(boolean[][] horizontalGrid) {
		this.horizontalGrid = horizontalGrid;
	}

	public boolean[][] getVerticalGrid() {
		return verticalGrid;
	}

	public void setVerticalGrid(boolean[][] verticalGrid) {
		this.verticalGrid = verticalGrid;
	}

	public String[][] getBoxes() {
		return boxes;
	}

	public void setBoxes(String[][] boxes) {
		this.boxes = boxes;
	}
}
