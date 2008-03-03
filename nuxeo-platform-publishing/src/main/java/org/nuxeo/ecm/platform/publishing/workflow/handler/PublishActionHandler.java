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
 * $Id: PublishActionHandler.java 29075 2008-01-16 09:12:59Z jcarsique $
 */

package org.nuxeo.ecm.platform.publishing.workflow.handler;

import org.jbpm.graph.exe.ExecutionContext;
import org.nuxeo.ecm.platform.workflow.jbpm.handlers.api.client.AbstractWorkflowDocumentActionHandler;

/**
 * Handler that deals with what happends to a document when puslished.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class PublishActionHandler extends AbstractWorkflowDocumentActionHandler {

    private static final long serialVersionUID = 1L;

    public void execute(ExecutionContext ec) throws Exception {
        // Remove workflow specific ACP => document is visible now.
        removeWFACL(ec);

        // Notify of document publication.
        // INA-71 : NP : already thrown from action bean
//        notifyEvent(ec, EventNames.DOCUMENT_PUBLICATION_APPROVED);
    }

}
