/*
 * (C) Copyright 2006-20012 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.template.samples;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.content.template.service.PostContentCreationHandler;
import org.nuxeo.runtime.api.Framework;

/**
 * Called by the ContentTemplateService at repository init time to trigger the
 * models and samples import
 * 
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * 
 */
public class InitListener implements PostContentCreationHandler {

    protected final static Log log = LogFactory.getLog(InitListener.class);

    @Override
    public void execute(CoreSession session) {

        ModelImporter importer = new ModelImporter(session);
        try {
            if (!Framework.isTestModeSet()) {
                ModelImporter.expandResources();
            }
            int nbImportedDocs = importer.importModels();
            log.info("Template sample import done : " + nbImportedDocs
                    + " documents imported");
        } catch (Exception e) {
            log.error("Error during template samples import", e);
        }
    }

}
