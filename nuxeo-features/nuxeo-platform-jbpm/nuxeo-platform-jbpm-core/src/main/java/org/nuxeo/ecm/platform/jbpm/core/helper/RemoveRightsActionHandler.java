/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.jbpm.core.helper;

import org.jbpm.graph.exe.ExecutionContext;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.platform.jbpm.AbstractJbpmHandlerHelper;
import org.nuxeo.ecm.platform.jbpm.NuxeoJbpmException;

/**
 * Action handler that removes rights
 *
 * @author Anahide Tchertchian
 *
 */
public class RemoveRightsActionHandler extends AbstractJbpmHandlerHelper {

    private static final long serialVersionUID = 1L;

    // XXX open a system session to set rights: running a workflow only requires
    // "write"
    protected CoreSession getSystemSession() throws Exception {
        String repositoryName = getDocumentRepositoryName();
        try {
            return CoreInstance.getInstance().open(repositoryName, null);
        } catch (ClientException e) {
            throw new NuxeoJbpmException(e);
        }
    }

    @Override
    public void execute(ExecutionContext executionContext) throws Exception {
        this.executionContext = executionContext;
        if (nuxeoHasStarted()) {
            CoreSession session = null;
            try {
                session = getSystemSession();
                DocumentRef docRef = getDocumentRef();
                RemoveRightsUnrestricted runner = new RemoveRightsUnrestricted(
                        session, docRef, getACLName());
                runner.runUnrestricted();
            } finally {
                if (session != null) {
                    closeCoreSession(session);
                }
            }
        }
    }
}
