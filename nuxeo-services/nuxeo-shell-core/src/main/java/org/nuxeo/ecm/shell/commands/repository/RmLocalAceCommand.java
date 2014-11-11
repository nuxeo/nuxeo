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
import org.nuxeo.ecm.shell.CommandLine;

/**
 * Command for removing local ace to the current document.
 * 
 * @author Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 * 
 */
public class RmLocalAceCommand extends AbstractCommand {

    public static final Log log = LogFactory.getLog(RmLocalAceCommand.class);

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

        if (parameters.length != 1) {
            log.error(cmdLine.getCommand()
                    + " takes exactly 1 parameter: the ace number to be removed (from viewlocalace).");
            return;
        }
        int aceindex = new Integer(parameters[0]);

        CoreSession session = context.getCoreSession();
        removeLocalAce(session, doc, aceindex);
        session.save();
    }

    /**
     * Remove to the document the specified ace
     * 
     * @param session
     * @param doc
     * @param user
     * @param permission
     * @param grant
     */
    protected void removeLocalAce(CoreSession session, DocumentModel doc,
            int aceindex) throws Exception {

        ACP acp = session.getACP(doc.getRef());

        ACL acl = acp.getACL(ACL.LOCAL_ACL);
        if (acl == null) {
            log.warn("No local acl for the current document");
            return;
        }
        ACE[] aces = acl.getACEs();
        if (aces.length < aceindex) {
            log.error("aceindex can't be greater than the number of aces: "
                    + aces.length);
            return;
        }

        acl.remove(aceindex);

        session.setACP(doc.getRef(), acp, true);
    }

}
