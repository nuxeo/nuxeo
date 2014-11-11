package org.nuxeo.ecm.platform.ui.web.auth.interfaces;

import org.nuxeo.ecm.platform.ui.web.auth.CachableUserIdentificationInfo;

public interface NuxeoAuthenticationPropagator {

	/**
	 * Propagate userIdentification information from the web context to the ejb context
	 * 
	 * @param cachableUserIdent
	 */
	void propagateUserIdentificationInformation(CachableUserIdentificationInfo cachableUserIdent);
	
}
