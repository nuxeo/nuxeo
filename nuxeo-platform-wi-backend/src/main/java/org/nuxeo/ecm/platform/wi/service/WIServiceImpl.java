package org.nuxeo.ecm.platform.wi.service;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.wi.backend.Backend;
import org.nuxeo.ecm.platform.wi.filter.SessionCacheHolder;
import org.nuxeo.ecm.platform.wi.filter.WISession;

import java.security.Principal;

/**
 * @author Organization: Gagnavarslan ehf
 */
public class WIServiceImpl implements WIService {

    public String getPathById(String uuid, CoreSession session) throws ClientException {
        DocumentModel model = session.getDocument(new IdRef(uuid));
        if (model == null) {
            return null;
        }

        String path = model.getPathAsString();
        Backend backend = getBackend(session);
        return backend.getVirtualPath(path);
    }

    private Backend getBackend(CoreSession session) {
        if (session == null) {
            return null;
        }
        Principal principal = session.getPrincipal();
        if (principal == null || StringUtils.isEmpty(principal.getName())) {
            return null;
        }
        String principalName = principal.getName();
        WISession wiSession = SessionCacheHolder.getInstance().getCache().get(principalName);
        wiSession.setAttribute(WISession.CORESESSION_KEY, session);
        return new PluggableBackendFactory().getBackend(wiSession);
    }

}
