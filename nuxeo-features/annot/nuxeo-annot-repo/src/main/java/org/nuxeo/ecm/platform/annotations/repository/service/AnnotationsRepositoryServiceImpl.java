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

package org.nuxeo.ecm.platform.annotations.repository.service;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

/**
 * @author Alexandre Russel
 */
public class AnnotationsRepositoryServiceImpl implements AnnotationsRepositoryService {

    private DocumentAnnotability annotability;

    private SecurityManager securityManager;

    public SecurityManager getSecurityManager() {
        return securityManager;
    }

    public void setSecurityManager(SecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    public void clear() {
    }

    public void setDocumentAnnotability(DocumentAnnotability annotability) {
        this.annotability = annotability;
    }

    @Override
    public boolean isAnnotable(DocumentModel document) {
        return annotability.isAnnotable(document);
    }

    // for testing
    DocumentAnnotability getAnnotability() {
        return annotability;
    }

    void setAnnotability(DocumentAnnotability annotability) {
        this.annotability = annotability;
    }

    @Override
    public boolean check(NuxeoPrincipal user, String permission, DocumentModel model) {
        return securityManager.check(user, permission, model);
    }

}
