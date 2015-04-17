package org.nuxeo.webengine.gwt.codeserver;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.NuxeoException;

@XObject("classpath")
public class CodeServerClasspath {

	protected URL[] entries = new URL[0];

	@XNode
	public void setLibdir(File dir) {
		List<URL> entries = new ArrayList<URL>();
		for (File entry : dir.listFiles()) {
			try {
				entries.add(entry.toURI().toURL());
			} catch (MalformedURLException e) {
				throw new NuxeoException("Cannot find location of " + entry);
			}
		}
		this.entries = entries.toArray(new URL[entries.size()]);
	}
}
