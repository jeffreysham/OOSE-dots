package com.oose2016.jsham2.dots;

/**
 * This is the player class for the dots game.
 * It holds the basic color information and id number.
 * 
 * @author jsham2, Jeffrey Sham CS421
 */
public class Player {
	private String id;
	private String type;
	
	public Player(String id, String type) {
		this.id = id;
		this.type = type;
	}
	
	public String getId() {
		return id;
	}

	public String getType() {
		return type;
	}
}
