package org.nuxeo.ecm.webengine.webcomments;

import javax.security.auth.login.LoginContext;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.jbpm.AbstractJbpmHandlerHelper;
import org.nuxeo.ecm.platform.jbpm.NuxeoJbpmException;
import org.nuxeo.runtime.api.Framework;

/**
 * Handler with helper methods for comment jbpm action handlers.
 *
 */
public abstract class CommentHandlerHelper extends AbstractJbpmHandlerHelper {

    protected CoreSession getSystemSession() throws Exception {
        String repositoryName = getDocumentRepositoryName();
        try {
            return CoreInstance.getInstance().open(repositoryName, null);
        } catch (ClientException e) {
            throw new NuxeoJbpmException(e);
        }
    }

    // need to open a system session for this to work ok: users may not have the
    // 'WriteLifeCycle' permission on doc
    protected void followTransition(String transition) throws Exception {
        String postId = getStringVariable("postRef");
        DocumentRef postRef = new IdRef(postId);
        LoginContext loginContext = null;
        CoreSession systemSession = null;
        try {
            loginContext = Framework.login();
            systemSession = getSystemSession();
            systemSession.followTransition(postRef, transition);
            systemSession.save();
        } finally {
            if (loginContext != null) {
                loginContext.logout();
            }
            if (systemSession != null) {
                closeCoreSession(systemSession);
            }
        }
    }
}
