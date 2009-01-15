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
 *     arussel
 */
package org.nuxeo.ecm.platform.jbpm.core.helper;

import java.io.Serializable;

import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.taskmgmt.exe.Assignable;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

public interface PublicationHelper extends Serializable {

    /*
     * (non-Javadoc)
     *
     * @see org.nuxeo.ecm.platform.jbpm.core.helper.PublicationHelper#decide(org.jbpm.graph.exe.ExecutionContext)
     */
    String decide(ExecutionContext executionContext) throws Exception;

    /*
     * (non-Javadoc)
     *
     * @see org.nuxeo.ecm.platform.jbpm.core.helper.PublicationHelper#publishDocument(org.nuxeo.ecm.core.api.CoreSession,
     *      org.nuxeo.ecm.core.api.DocumentModel,
     *      org.nuxeo.ecm.core.api.DocumentModel)
     */
    void publishDocument(CoreSession session, DocumentModel docToPublish,
            DocumentModel secionToPublish) throws ClientException;

    /*
     * (non-Javadoc)
     *
     * @see org.nuxeo.ecm.platform.jbpm.core.helper.PublicationHelper#assign(org.jbpm.taskmgmt.exe.Assignable,
     *      org.jbpm.graph.exe.ExecutionContext)
     */
    void assign(Assignable assignable, ExecutionContext executionContext)
            throws Exception;

}