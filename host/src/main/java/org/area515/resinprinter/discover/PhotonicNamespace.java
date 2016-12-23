package org.area515.resinprinter.discover;

import java.net.URI;

import org.fourthline.cling.model.Namespace;
import org.fourthline.cling.model.meta.Icon;

public class PhotonicNamespace extends Namespace {
	@Override
	public URI getIconPath(Icon icon) {
		return icon.getUri();
	}
}
