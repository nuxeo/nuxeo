package org.nuxeo.ecm.platform.ui.web.richfaces;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.ajax4jsf.javascript.JSMin;
import org.nuxeo.ecm.platform.web.common.resources.JSMinimizer;

/**
 * Implementation of the {@link JSMinimizer} interface based on RichFaces
 *
 * @author tiry
 *
 */
public class RichFacesJSMinimizer implements JSMinimizer {

	public String minimize(String jsScriptContent) {

		try {
			InputStream in = new ByteArrayInputStream(jsScriptContent.getBytes("UTF-8"));
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			JSMin jsmin = new JSMin(in,out);
			jsmin.jsmin();
			return out.toString("UTF-8");
		}
		catch (Exception e) {
			return jsScriptContent;
		}
	}
}
