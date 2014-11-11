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
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.shell.CommandLine;

/**
 * Command for viewing local acl to the current document.
 *
 * @author Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 *
 */
public class ViewLocalAclCommand extends AbstractCommand {

    public static final Log log = LogFactory.getLog(ViewLocalAclCommand.class);

    /*
     * (non-Javadoc)
     *
     * @see
     * org.nuxeo.ecm.shell.commands.repository.AbstractCommand#run(org.nuxeo
     * .ecm.shell.CommandLine)
     */
    @Override
    public void run(CommandLine cmdLine) throws Exception {
        String[] elements = cmdLine.getParameters();
        DocumentModel doc;
        if (elements.length == 1) {
            Path path = new Path(elements[0]);
            try {
                doc = context.fetchDocument(path);
            } catch (Exception e) {
                log.error("Failed to retrieve the given folder", e);
                return;
            }
        } else {
            doc = context.fetchDocument();
        }
        viewLocalAce(context.getCoreSession(), doc);
    }

    /**
     * Display current local ace
     *
     * @param session
     * @param doc
     * @throws Exception
     */
    protected void viewLocalAce(CoreSession session, DocumentModel doc)
            throws Exception {

        ACP acp = session.getACP(doc.getRef());
        ACL acl = acp.getACL(ACL.LOCAL_ACL);

        if (acl == null) {
            log.info("No local acl for the current document");
            return;
        }

        ACE[] aces = acl.getACEs();
        if (aces.length <= 0) {
            log.info("No local acl for the current document");
            return;
        }

        for (int i = 0; i < aces.length; i++) {
            ACE ace = aces[i];
            log.info(i + ".\tUsername:" + ace.getUsername() + "\tPermission:"
                    + ace.getPermission() + "\tGrant:" + ace.isGranted());
        }

    }
}
