package org.nuxeo.ecm.platform.ui.flex.seam;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Unwrap;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.runtime.api.Framework;

@Name("flexDocumentManager")
@Scope(ScopeType.SESSION)
public class FlexDocumentManager {

    private CoreSession session = null;

    @Unwrap
    public CoreSession getFlexDocumentManager() throws Exception {
        if (session == null) {
            RepositoryManager repositoryMgr = Framework.getService(RepositoryManager.class);
            Repository repository = repositoryMgr.getDefaultRepository();
            session = repository.open();
        }
        return session;
    }
}
