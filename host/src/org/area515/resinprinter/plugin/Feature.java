package org.area515.resinprinter.plugin;

import java.net.URI;

public interface Feature {
	public void start(URI uri);
	public void stop();
}
