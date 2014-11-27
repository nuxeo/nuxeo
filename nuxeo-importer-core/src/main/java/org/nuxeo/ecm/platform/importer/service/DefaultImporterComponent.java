/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *    Mariana Cedica
 */
package org.nuxeo.ecm.platform.importer.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.importer.factories.DefaultDocumentModelFactory;
import org.nuxeo.ecm.platform.importer.log.ImporterLogger;
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
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        if (IMPORTER_CONFIGURATION_XP.equals(extensionPoint)) {

            ImporterConfigurationDescriptor descriptor = (ImporterConfigurationDescriptor) contribution;
            Class<? extends SourceNode> sourceNodeClass = (Class<? extends SourceNode>) descriptor.getSourceNodeClass();
            if (sourceNodeClass == null) {
                sourceNodeClass = FileSourceNode.class;
                log.info("No custom implementation defined for the SourceNode, using FileSourceNode");
            }
            importerService.setSourceNodeClass(sourceNodeClass);

            Class<? extends DefaultDocumentModelFactory> docFactoryClass = descriptor.getDocumentModelFactory().getDocumentModelFactoryClass();
            if (docFactoryClass == null) {
                docFactoryClass = DefaultDocumentModelFactory.class;
                log.info("No custom implementation provided for the documentModelFactory, using DefaultDocumentModelFactory");
            }
            importerService.setDocModelFactoryClass(docFactoryClass);

            String folderishType = descriptor.getDocumentModelFactory().getFolderishType();
            if (folderishType == null) {
                folderishType = DEFAULT_FOLDERISH_DOC_TYPE;
                log.info("No folderish type defined, using Folder by default");
            }
            importerService.setFolderishDocType(folderishType);

            String leafType = descriptor.getDocumentModelFactory().getLeafType();
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
                    importerService.setImporterLogger(logClass.newInstance());
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException(e);
                }
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
