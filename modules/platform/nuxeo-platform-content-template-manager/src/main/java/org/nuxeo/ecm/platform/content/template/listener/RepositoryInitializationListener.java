/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.content.template.listener;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.repository.RepositoryInitializationHandler;
import org.nuxeo.ecm.platform.content.template.service.ContentTemplateService;
import org.nuxeo.runtime.api.Framework;

public class RepositoryInitializationListener extends RepositoryInitializationHandler {

    @Override
    public void doInitializeRepository(CoreSession session) {
        // This method gets called as a system user
        // so we have all needed rights to do the check and the creation
        DocumentModel root = session.getRootDocument();
        ContentTemplateService service = Framework.getService(ContentTemplateService.class);
        service.executeFactoryForType(root);
        // Allow queries to see changes during
        // postContentCreationHandler executions
        session.save();
        service.executePostContentCreationHandlers(session);
        session.save();
    }

}
