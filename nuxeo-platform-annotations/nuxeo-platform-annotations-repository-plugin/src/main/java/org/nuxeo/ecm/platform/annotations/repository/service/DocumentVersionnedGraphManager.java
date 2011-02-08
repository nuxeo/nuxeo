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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.repository.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.api.facet.VersioningDocument;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.platform.annotations.api.Annotation;
import org.nuxeo.ecm.platform.annotations.api.AnnotationsConstants;
import org.nuxeo.ecm.platform.annotations.api.AnnotationsService;
import org.nuxeo.ecm.platform.annotations.repository.URNDocumentViewTranslator;
import org.nuxeo.ecm.platform.relations.api.Graph;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.impl.StatementImpl;
import org.nuxeo.runtime.api.Framework;

import java.net.URI;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
 *
 */
public class DocumentVersionnedGraphManager implements
        GraphManagerEventListener {

    private static final Log log = LogFactory.getLog(DocumentVersionnedGraphManager.class);

    private URNDocumentViewTranslator translator;

    public void manage(Event event) {
        if (translator == null) {
            translator = new URNDocumentViewTranslator();
        }
        EventContext context = event.getContext();
        NuxeoPrincipal user = null;
        Principal principal = context.getPrincipal();
        if (principal instanceof NuxeoPrincipal) {
            user = (NuxeoPrincipal) principal;
        } else {
            log.debug("Discading event on a non NuxeoPrincipal user");
            return;
        }

        DocumentModel docModel = (DocumentModel) context.getArguments()[0];
        String docId = docModel.getId();
        String repo = docModel.getRepositoryName();

        if (DocumentEventTypes.DOCUMENT_CHECKEDIN.equals(event.getName())) {
            DocumentRef versionRef = (DocumentRef) context.getProperty("checkedInVersionRef");
            copyGraphFor(repo, docId, versionRef.toString(), user);
        } else if (DocumentEventTypes.DOCUMENT_REMOVED.equals(event.getName())
                || DocumentEventTypes.VERSION_REMOVED.equals(event.getName())) {
            removeGraphFor(repo, docId, user);
        } else if (DocumentEventTypes.DOCUMENT_RESTORED.equals(event.getName())) {
            String versionUUID = (String) context.getProperty(VersioningDocument.RESTORED_VERSION_UUID_KEY);
            restoreGraphFor(repo, versionUUID, docId, user);
        }
    }

    private void copyGraphFor(String repositoryName, String fromId,
            String toId, NuxeoPrincipal principal) {
        try {
            copyGraphFor(translator.getNuxeoUrn(repositoryName, fromId),
                    translator.getNuxeoUrn(repositoryName, toId), principal);
        } catch (Exception e) {
            log.error(e);
        }
    }

    private static void copyGraphFor(URI current, URI copied,
            NuxeoPrincipal user) throws Exception {
        List<Statement> newStatements = new ArrayList<Statement>();
        AnnotationsService service = Framework.getService(AnnotationsService.class);
        List<Annotation> annotations = service.queryAnnotations(current, null,
                user);
        log.debug("Copying annotations graph from " + current + " to " + copied
                + " for " + annotations.size() + " annotations.");
        for (Annotation annotation : annotations) {
            List<Statement> statements = annotation.getStatements();
            for (Statement statement : statements) {
                if (statement.getPredicate().equals(
                        AnnotationsConstants.a_annotates)) {
                    Resource resource = (Resource) statement.getObject();
                    if (current.toString().equals(resource.getUri())) {
                        // copy only the statements associated to the current
                        // URI
                        Statement newStatement = new StatementImpl(
                                statement.getSubject(),
                                statement.getPredicate(), new ResourceImpl(
                                        copied.toString()));
                        newStatements.add(newStatement);
                    }
                }
            }
        }
        Graph graph = service.getAnnotationGraph();
        graph.add(newStatements);
    }

    private void removeGraphFor(String repositoryName, String id,
            NuxeoPrincipal principal) {
        try {
            removeGraphFor(translator.getNuxeoUrn(repositoryName, id),
                    principal);
        } catch (Exception e) {
            log.error(e);
        }
    }

    private static void removeGraphFor(URI uri, NuxeoPrincipal user)
            throws Exception {
        log.debug("Removing annotations graph for " + uri);
        AnnotationsService service = Framework.getService(AnnotationsService.class);
        List<Annotation> annotations = service.queryAnnotations(uri, null, user);
        for (Annotation annotation : annotations) {
            service.deleteAnnotationFor(uri, annotation, user);
        }
    }

    private void restoreGraphFor(String repositoryName, String versionId,
            String docId, NuxeoPrincipal principal) {
        log.debug("Restoring annotations graph for docId:" + docId
                + " and versionId:" + versionId);
        try {
            removeGraphFor(translator.getNuxeoUrn(repositoryName, docId),
                    principal);
            copyGraphFor(repositoryName, versionId, docId, principal);
        } catch (Exception e) {
            log.error(e);
        }
    }

}
