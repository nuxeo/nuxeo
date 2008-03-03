/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 * $Id: RejectActionHandler.java 23221 2007-07-31 13:41:41Z janguenot $
 */

package org.nuxeo.ecm.platform.publishing.workflow.handler;

import org.jbpm.graph.exe.ExecutionContext;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.workflow.jbpm.handlers.api.client.AbstractWorkflowDocumentActionHandler;

/**
 * Handler that deals with what happends to a document when publishing is
 * rejected.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class RejectActionHandler extends AbstractWorkflowDocumentActionHandler {

    private static final long serialVersionUID = 1L;

    public void execute(ExecutionContext ec) throws Exception {

        // Remove rights before deleting it.
        removeWFACL(ec);

        // Here we do remove the document in this case.
        CoreSession docMgr = getDocumentManager(ec);
        docMgr.removeDocument(getDocumentRef(ec));
        docMgr.save();
    }

}
