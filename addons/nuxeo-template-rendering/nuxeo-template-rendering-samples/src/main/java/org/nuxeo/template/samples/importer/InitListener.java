/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.template.samples.importer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.content.template.service.PostContentCreationHandler;

/**
 * Called by the ContentTemplateService at repository init time to trigger the models and samples import
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public class InitListener implements PostContentCreationHandler {

    protected final static Log log = LogFactory.getLog(InitListener.class);

    @Override
    public void execute(CoreSession session) {
        ModelImporter importer = new ModelImporter(session);
        int nbImportedDocs = importer.importModels();
        log.info("Template sample import done : " + nbImportedDocs + " documents imported");
    }

}
