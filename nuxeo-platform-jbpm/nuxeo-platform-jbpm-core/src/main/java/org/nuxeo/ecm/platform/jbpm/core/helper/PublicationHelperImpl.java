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

import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.taskmgmt.exe.Assignable;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.publishing.api.PublishingService;
import org.nuxeo.runtime.api.Framework;

/**
 * @author arussel
 *
 */
public class PublicationHelperImpl extends AbstractHelper implements PublicationHelper {
    private static final long serialVersionUID = 1L;

    private transient PublishingService publishingService;

    @Override
    public String decide(ExecutionContext executionContext) throws Exception {
        DocumentModel document = getTransientVariable(executionContext, "document");
        DocumentModel section = getTransientVariable(executionContext, "section");
        String user = executionContext.getJbpmContext().getActorId();
        // publicationservice.canPublish(document, section, user);
        return "can publish";
    }

    @SuppressWarnings("unchecked")
    private <T> T getTransientVariable(ExecutionContext executionContext, String name) {
        return (T) executionContext.getContextInstance().getTransientVariable(name);
    }

    public void publishDocument(CoreSession session,
            DocumentModel docToPublish, DocumentModel secionToPublish)
            throws ClientException {
        session.publishDocument(docToPublish, secionToPublish);
        //publicationService.publishDocument(docToPublish, sectionToPublish, coreSession)
    }

    @Override
    public void assign(Assignable assignable, ExecutionContext executionContext)
            throws Exception {
        // List l = getPublishingService().getValidatorsFor(section, documents);
        assignable.setPooledActors(new String[] { "bob", "jack" });
    }

    protected PublishingService getPublishingService() throws Exception {
        if (publishingService == null) {
            publishingService = Framework.getService(PublishingService.class);
        }
        return publishingService;
    }
}
