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
 *     Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 */
package org.nuxeo.ecm.shell.commands.repository;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.shell.CommandLine;

/**
 * Command for adding local ace to the current document.
 * 
 * @author Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 * 
 */
public class AddLocalAceCommand extends AbstractCommand {

    public static final Log log = LogFactory.getLog(AddLocalAceCommand.class);

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.nuxeo.ecm.shell.commands.repository.AbstractCommand#run(org.nuxeo
     * .ecm.shell.CommandLine)
     */
    @Override
    public void run(CommandLine cmdLine) throws Exception {
        String[] parameters = cmdLine.getParameters();
        DocumentModel doc = context.fetchDocument();

        if (parameters.length != 3) {
            log.error(cmdLine.getCommand()
                    + " takes exactly 3 parameters: the username, the permission and a boolean (grant if true, deny if false)");
            return;
        }
        String user = parameters[0];
        String permission = parameters[1];
        boolean grant = new Boolean(parameters[2]);
        CoreSession session = context.getCoreSession();
        addLocalAce(session, doc, user, permission, grant);
        session.save();
    }

    /**
     * Add to the document a new local ace (on top, index 0).
     * 
     * @param session
     * @param doc
     * @param user
     * @param permission
     * @param grant
     */
    protected void addLocalAce(CoreSession session, DocumentModel doc,
            String user, String permission, boolean grant) throws Exception {
        ACP acp = session.getACP(doc.getRef());

        // creating ACEs
        // granting read and browse for admin
        ACL acl = acp.getACL(ACL.LOCAL_ACL);
        if (acl == null) {
            // create a new local acl if it doesn't exist
            acl = new ACLImpl(ACL.LOCAL_ACL);
            acp.addACL(acl);
        }

        acl.add(0, new ACE(user, permission, grant));
        session.setACP(doc.getRef(), acp, true);
    }

}
