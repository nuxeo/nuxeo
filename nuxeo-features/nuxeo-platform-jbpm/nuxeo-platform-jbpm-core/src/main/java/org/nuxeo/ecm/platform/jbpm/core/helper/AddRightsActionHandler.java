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

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.jbpm.graph.exe.ExecutionContext;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.jbpm.AbstractJbpmHandlerHelper;
import org.nuxeo.ecm.platform.jbpm.NuxeoJbpmException;
import org.nuxeo.ecm.platform.jbpm.VirtualTaskInstance;

/**
 * Action handler that add READ rights to given participants.
 *
 * @author Anahide Tchertchian
 *
 */
public class AddRightsActionHandler extends AbstractJbpmHandlerHelper {

    private static final long serialVersionUID = 1L;

    private static final String RIGHT_PARAMETER = "right";

    private String list;

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
    @SuppressWarnings("unchecked")
    public void execute(ExecutionContext executionContext) throws Exception {
        this.executionContext = executionContext;
        if (nuxeoHasStarted() && list != null) {
            List<VirtualTaskInstance> participants
                    = (List<VirtualTaskInstance>) executionContext.getContextInstance().getTransientVariable(list);
            if (participants == null) {
                participants = (List<VirtualTaskInstance>) executionContext.getVariable(list);
            }
            CoreSession session = null;
            try {
                session = getSystemSession();
                DocumentRef docRef = getDocumentRef();
                ACP acp = session.getACP(docRef);
                String aclName = getACLName();
                ACL acl = acp.getOrCreateACL(aclName);
                // add back read and write permissions on doc for initiator in
                // case they're lost during the review
                String initiator = getInitiator();
                if (initiator != null) {
                    if (initiator.startsWith(NuxeoPrincipal.PREFIX)) {
                        initiator = initiator.substring(NuxeoPrincipal.PREFIX.length());
                    }
                    acl.add(new ACE(initiator, SecurityConstants.READ_WRITE,
                            true));
                }
                // add permission for every review participant according to
                // the 'right' parameter of the VirtualTaskInstance
                for (VirtualTaskInstance participant : participants) {
                    for (String pname : participant.getActors()) {
                        // get rid of user/group prefix
                        if (pname.startsWith(NuxeoPrincipal.PREFIX)) {
                            pname = pname.substring(NuxeoPrincipal.PREFIX.length());
                        } else if (pname.startsWith(NuxeoGroup.PREFIX)) {
                            pname = pname.substring(NuxeoGroup.PREFIX.length());
                        }
                        String permission = SecurityConstants.READ;
                        Map<String, Serializable> parameters = participant.getParameters();
                        if (parameters.containsKey(RIGHT_PARAMETER)) {
                            permission = (String) parameters.get(RIGHT_PARAMETER);
                        }
                        acl.add(new ACE(pname, permission, true));
                    }
                }
                acp.addACL(acl);
                AddRightUnrestricted runner = new AddRightUnrestricted(session,
                        docRef, acp);
                runner.runUnrestricted();
            } finally {
                if (session != null) {
                    closeCoreSession(session);
                }
            }
        }
        executionContext.getToken().signal();
    }

}
