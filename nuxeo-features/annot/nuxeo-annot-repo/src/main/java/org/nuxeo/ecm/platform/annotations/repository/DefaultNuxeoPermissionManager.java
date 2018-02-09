/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.repository;

import java.net.URI;

import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.annotations.repository.service.AnnotationsRepositoryService;
import org.nuxeo.ecm.platform.annotations.service.PermissionManager;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.runtime.api.Framework;

/**
 * @author Alexandre Russel
 */
public class DefaultNuxeoPermissionManager implements PermissionManager {

    private AnnotationsRepositoryService service;

    private final URNDocumentViewTranslator translator = new URNDocumentViewTranslator();

    public DefaultNuxeoPermissionManager() {
        service = Framework.getService(AnnotationsRepositoryService.class);
    }

    public boolean check(NuxeoPrincipal user, String permission, URI uri) {
        DocumentView view = translator.getDocumentViewFromUri(uri);
        try (CloseableCoreSession session = CoreInstance.openCoreSession(null)) {
            DocumentModel model = session.getDocument(view.getDocumentLocation().getDocRef());
            return service.check(user, permission, model);
        }
    }

}
