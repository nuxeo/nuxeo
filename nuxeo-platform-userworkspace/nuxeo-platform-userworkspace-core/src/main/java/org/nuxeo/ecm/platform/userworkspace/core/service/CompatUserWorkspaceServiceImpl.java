package org.nuxeo.ecm.platform.userworkspace.core.service;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Alternate implementation that is backward compatible
 * (Allow to have one UserWorkspace per user and per domain)
 *
 * @author Thierry Delprat
 *
 */
public class CompatUserWorkspaceServiceImpl extends
        DefaultUserWorkspaceServiceImpl {

    private static final long serialVersionUID = 1L;

    @Override
    protected String getDomainName(CoreSession userCoreSession, DocumentModel currentDocument) {
        if (currentDocument!=null && currentDocument.getPath().segmentCount()>0) {
            return currentDocument.getPath().segment(0);
        }
        else {
            return UserWorkspaceServiceImplComponent.getTargetDomainName();
        }
    }


}
