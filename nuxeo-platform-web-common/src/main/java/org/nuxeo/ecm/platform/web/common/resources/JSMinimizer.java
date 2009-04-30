package org.nuxeo.ecm.platform.web.common.resources;

/**
 * Interface for minimizing JS script
 *
 * @author tiry
 *
 */
public interface JSMinimizer {

	public String minimize(String jsScriptContent);

}
