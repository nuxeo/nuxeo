package org.nuxeo.ecm.platform.wi.service;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;

/**
 * @author Organization: Gagnavarslan ehf
 */
public interface WIService {

    String getPathById(String uuid, CoreSession session) throws ClientException;

}
