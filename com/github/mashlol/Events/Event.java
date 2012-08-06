package com.github.mashlol.Events;

public class Event {

	private String message;
	private boolean up;
	private int frequency;
	
	public Event(String message, boolean up, int freq) {
		this.message = message;
		this.up = up;
		frequency = freq;
	}
	
	public String getMessage() {
		return message;
	}
	
	public boolean getUp () {
		return up;
	}
	
	public int getFrequency() {
		return frequency;
	}
	
}
