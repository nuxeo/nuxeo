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

import static java.util.Objects.requireNonNullElseGet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.importer.factories.DefaultDocumentModelFactory;
import org.nuxeo.ecm.platform.importer.log.ImporterLogger;
import org.nuxeo.ecm.platform.importer.service.ImporterConfigurationDescriptor.DocumentModelFactory;
import org.nuxeo.ecm.platform.importer.source.FileSourceNode;
import org.nuxeo.ecm.platform.importer.source.SourceNode;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

public class DefaultImporterComponent extends DefaultComponent {

    private static final Log log = LogFactory.getLog(DefaultImporterComponent.class);

    protected DefaultImporterService importerService;

    public static final String IMPORTER_CONFIGURATION_XP = "importerConfiguration";

    public static final String DEFAULT_FOLDERISH_DOC_TYPE = "Folder";

    public static final String DEFAULT_LEAF_DOC_TYPE = "File";

    @SuppressWarnings("unchecked")
    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (IMPORTER_CONFIGURATION_XP.equals(extensionPoint)) {

            ImporterConfigurationDescriptor descriptor = (ImporterConfigurationDescriptor) contribution;
            Class<? extends SourceNode> sourceNodeClass = (Class<? extends SourceNode>) descriptor.getSourceNodeClass();
            if (sourceNodeClass == null) {
                sourceNodeClass = FileSourceNode.class;
                log.info("No custom implementation defined for the SourceNode, using FileSourceNode");
            }
            importerService.setSourceNodeClass(sourceNodeClass);

            DocumentModelFactory docFactory = requireNonNullElseGet(descriptor.getDocumentModelFactory(),
                    DocumentModelFactory::new);
            Class<? extends DefaultDocumentModelFactory> docFactoryClass = docFactory.getDocumentModelFactoryClass();
            if (docFactoryClass == null) {
                docFactoryClass = DefaultDocumentModelFactory.class;
                log.info(
                        "No custom implementation provided for the documentModelFactory, using DefaultDocumentModelFactory");
            }
            importerService.setDocModelFactoryClass(docFactoryClass);

            String folderishType = docFactory.getFolderishType();
            if (folderishType == null) {
                folderishType = DEFAULT_FOLDERISH_DOC_TYPE;
                log.info("No folderish type defined, using Folder by default");
            }
            importerService.setFolderishDocType(folderishType);

            String leafType = docFactory.getLeafType();
            if (leafType == null) {
                leafType = DEFAULT_LEAF_DOC_TYPE;
                log.info("No leaf type doc defined, using File by deafult");
            }
            importerService.setLeafDocType(leafType);

            Class<? extends ImporterLogger> logClass = descriptor.getImporterLog();
            if (logClass == null) {
                log.info("No specific ImporterLogger configured for this importer");
            } else {
                try {
                    importerService.setImporterLogger(logClass.getDeclaredConstructor().newInstance());
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException(e);
                }
            }

            if (descriptor.getRepository() != null) {
                importerService.setRepository(descriptor.getRepository());
            }

            if (descriptor.getBulkMode() != null) {
                importerService.setBulkMode(descriptor.getBulkMode().booleanValue());
            }

            if (descriptor.getEnablePerfLogging() != null) {
                importerService.setEnablePerfLogging(descriptor.getEnablePerfLogging().booleanValue());
            }
        }
    }

    @Override
    public void activate(ComponentContext context) {
        importerService = new DefaultImporterServiceImpl();
    }

    @Override
    public void deactivate(ComponentContext context) {
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
