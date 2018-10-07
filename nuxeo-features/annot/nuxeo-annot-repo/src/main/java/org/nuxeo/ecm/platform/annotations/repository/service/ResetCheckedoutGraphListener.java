/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     matic
 */
package org.nuxeo.ecm.platform.annotations.repository.service;

import java.net.URI;
import java.util.List;

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

        removeGraphFor(session, repo, doc, context.getPrincipal());
    }

    protected void removeGraphFor(CoreSession session, String repositoryName, DocumentModel doc, NuxeoPrincipal user)
            {
        URI uri = translator.getNuxeoUrn(repositoryName, doc.getId());
        AnnotationsService service = Framework.getService(AnnotationsService.class);

        List<Annotation> annotations = service.queryAnnotations(uri, user);
        for (Annotation annotation : annotations) {
            new AnnotationsFulltextInjector().removeAnnotationText(doc, annotation.getId());
            session.saveDocument(doc);
            service.deleteAnnotationFor(uri, annotation, user);
        }
    }

}
