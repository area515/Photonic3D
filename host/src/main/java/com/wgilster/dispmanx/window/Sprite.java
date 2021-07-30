package com.wgilster.dispmanx.window;

public class Sprite {
	private Resource resource;
	private int element;
	
	Sprite(Resource resource, int element) {
		this.resource = resource;
		this.element = element;
	}
	
	int getElementHandle() {
		return element;
	}
	
	public Resource getResource() {
		return resource;
	}
}
