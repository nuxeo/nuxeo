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

package org.nuxeo.ecm.platform.annotations.repository;

import java.net.URI;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.annotations.api.AnnotationException;
import org.nuxeo.ecm.platform.annotations.repository.service.AnnotationsRepositoryService;
import org.nuxeo.ecm.platform.annotations.service.PermissionManager;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.runtime.api.Framework;

/**
 * @author Alexandre Russel
 *
 */
public class DefaultNuxeoPermissionManager implements PermissionManager {

    private AnnotationsRepositoryService service;

    private final URNDocumentViewTranslator translator = new URNDocumentViewTranslator();

    public DefaultNuxeoPermissionManager() {
        service = Framework.getService(AnnotationsRepositoryService.class);
    }

    public boolean check(NuxeoPrincipal user, String permission, URI uri)
            throws AnnotationException {
        DocumentView view = translator.getDocumentViewFromUri(uri);
        try (CoreSession session = CoreInstance.openCoreSession(null)) {
            DocumentModel model = session.getDocument(view.getDocumentLocation().getDocRef());
            return service.check(user, permission, model);
        } catch (ClientException e) {
            throw new AnnotationException(e);
        }
    }

}
