package org.nuxeo.opensocial.webengine.gadgets;

import java.io.InputStream;

public class GadgetStream  {
	private InputStream in;

	public GadgetStream(InputStream in) {
		this.in = in;
		
	}
	
	public InputStream getStream() {
		return in;
	}
}
