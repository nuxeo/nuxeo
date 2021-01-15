/*
 * (C) Copyright 2011-2019 Nuxeo (http://nuxeo.com/) and others.
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
 *    Mariana Cedica
 */
package org.nuxeo.ecm.platform.importer.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.runtime.RuntimeMessage.Level;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

public class DefaultImporterComponent extends DefaultComponent {

    private static final Logger log = LogManager.getLogger(DefaultImporterComponent.class);

    protected DefaultImporterServiceImpl importerService;

    public static final String IMPORTER_CONFIGURATION_XP = "importerConfiguration";

    public static final String DEFAULT_FOLDERISH_DOC_TYPE = "Folder";

    public static final String DEFAULT_LEAF_DOC_TYPE = "File";

    @Override
    public void start(ComponentContext context) {
        importerService = new DefaultImporterServiceImpl();
        this.<ImporterConfigurationDescriptor> getRegistryContribution(IMPORTER_CONFIGURATION_XP).ifPresent(desc -> {
            try {
                importerService.configure(desc);
            } catch (ReflectiveOperationException e) {
                addRuntimeMessage(Level.ERROR, e.getMessage());
                log.error(e, e);
            }
        });
    }

    @Override
    public void stop(ComponentContext context) throws InterruptedException {
        importerService = null;
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(DefaultImporterService.class)) {
            return adapter.cast(importerService);
        }
        return super.getAdapter(adapter);
    }

}
