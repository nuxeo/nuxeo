package org.nuxeo.ecm.platform.ui.web.richfaces;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.ajax4jsf.resource.InternetResource;
import org.ajax4jsf.resource.JarResource;
import org.ajax4jsf.resource.ResourceContext;

/**
 *
 * Fake (in Memory) {@link InternetResource} implementation
 *
 * @author tiry
 *
 */
public class AggregatedResources extends JarResource {

	protected StringBuffer sb = new StringBuffer();

	public AggregatedResources() {
		super();
	}

	public AggregatedResources(StringBuffer sb, String key) {
		super();
		this.sb=sb;
		this.setKey(key);
	}

	public AggregatedResources(String key) {
		super();
		this.setKey(key);
	}

	@Override
	public InputStream getResourceAsStream(ResourceContext context) {
		try {
			return new ByteArrayInputStream(sb.toString().getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}
}
