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
package org.nuxeo.ecm.platform.publishing;

import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

/**
 * @author arussel
 *
 */
public interface PublishingService {
    void submitToPublication(List<DocumentModel> documentsToPublish,
            List<DocumentModel> locationToPublishTo, NuxeoPrincipal principal);

    boolean isPublished(DocumentModel document, DocumentModel location,
            NuxeoPrincipal principal);

    List<DocumentModel> getPublishableLocation(DocumentModel document,
            NuxeoPrincipal principal);
}
