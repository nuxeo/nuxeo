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

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

/**
 * @author Alexandre Russel
 *
 */
public class AnnotationsRepositoryServiceImpl implements
        AnnotationsRepositoryService {

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

    public boolean isAnnotable(DocumentModel document) throws ClientException {
        return annotability.isAnnotable(document);
    }
    // for testing
    DocumentAnnotability getAnnotability() {
        return annotability;
    }

    void setAnnotability(DocumentAnnotability annotability) {
        this.annotability = annotability;
    }

    public boolean check(NuxeoPrincipal user, String permission,
            DocumentModel model) {
        return securityManager.check(user, permission, model);
    }

}
