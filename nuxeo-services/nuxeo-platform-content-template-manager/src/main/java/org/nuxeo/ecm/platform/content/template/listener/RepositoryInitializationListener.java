/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.content.template.listener;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.repository.RepositoryInitializationHandler;
import org.nuxeo.ecm.platform.content.template.service.ContentTemplateService;
import org.nuxeo.runtime.api.Framework;

public class RepositoryInitializationListener  extends RepositoryInitializationHandler{

    private ContentTemplateService service;

    @Override
    public void doInitializeRepository(CoreSession session) throws ClientException {
        // This method gets called as a system user
        // so we have all needed rights to do the check and the creation
        DocumentModel root = session.getRootDocument();
        getService().executeFactoryForType(root);
        session.save();
    }

    private ContentTemplateService getService() {
        if (service == null) {
            service = Framework.getLocalService(ContentTemplateService.class);
        }
        return service;
    }

}
