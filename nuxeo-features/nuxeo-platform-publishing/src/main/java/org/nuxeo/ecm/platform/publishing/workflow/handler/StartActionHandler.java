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
 * $Id: StartActionHandler.java 22844 2007-07-22 20:50:07Z sfermigier $
 */

package org.nuxeo.ecm.platform.publishing.workflow.handler;

import org.jbpm.graph.exe.ExecutionContext;
import org.nuxeo.ecm.platform.workflow.jbpm.handlers.api.client.AbstractWorkflowDocumentActionHandler;

/**
 * Handler responsible for the fixtures at publishing startup.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class StartActionHandler extends
        AbstractWorkflowDocumentActionHandler {

    private static final long serialVersionUID = 1L;

    public void execute(ExecutionContext ec) throws Exception {
        bindDocumentToProcess(ec);
    }

}
