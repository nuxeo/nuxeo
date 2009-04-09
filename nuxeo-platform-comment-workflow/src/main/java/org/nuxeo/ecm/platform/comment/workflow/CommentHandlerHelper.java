/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     mcedica
 */

package org.nuxeo.ecm.platform.comment.workflow;

import javax.security.auth.login.LoginContext;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants;
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
        String postId = getStringVariable(CommentsConstants.COMMENT_ID);
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
