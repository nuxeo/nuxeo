/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     matic
 */
package org.nuxeo.ecm.platform.annotations.repository.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.platform.annotations.api.Annotation;
import org.nuxeo.ecm.platform.annotations.api.AnnotationsService;
import org.nuxeo.ecm.platform.annotations.repository.URNDocumentViewTranslator;
import org.nuxeo.runtime.api.Framework;

/**
 * @author matic
 * 
 */
public class ResetCheckedoutGraphListener implements GraphManagerEventListener {

    protected final URNDocumentViewTranslator translator = new URNDocumentViewTranslator();

    protected final GraphManagerEventListener copyManager = new DocumentVersionnedGraphManager();
   
    public void manage(Event event) {

        copyManager.manage(event);
        
        if (!DocumentEventTypes.DOCUMENT_CHECKEDIN.equals(event.getName())) {
            return;
        }
        
        // reset checked-out graph

        final EventContext context = event.getContext();
        final CoreSession session = context.getCoreSession();

        final DocumentModel doc = (DocumentModel) context.getArguments()[0];
        final String repo = doc.getRepositoryName();

        try {
            removeGraphFor(session, repo, doc, (NuxeoPrincipal) context.getPrincipal());
        } catch (Throwable e) {
            throw new ClientRuntimeException(
                    "Cannot remove annotations from checked-out version of "
                            + doc.getPathAsString(), e);
        }
    }

    protected void removeGraphFor(CoreSession session, String repositoryName, DocumentModel doc,
            NuxeoPrincipal user) throws URISyntaxException, ClientException {
        URI uri = translator.getNuxeoUrn(repositoryName, doc.getId());
        AnnotationsService service = Framework.getLocalService(AnnotationsService.class);

        List<Annotation> annotations = service.queryAnnotations(uri, null, user);
        for (Annotation annotation : annotations) {
            AnnotationsRepositoryComponent.instance.injector.removeAnnotationText(doc, annotation.getId());
            session.saveDocument(doc);
            service.deleteAnnotationFor(uri, annotation, user);
        }
    }


}
