/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.service.impl;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.service.FileSystemItemAdapterService;
import org.nuxeo.drive.service.FileSystemItemFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Default implementation of the {@link FileSystemItemAdapterService}.
 *
 * @author Antoine Taillefer
 */
public class FileSystemItemAdapterServiceImpl extends DefaultComponent
        implements FileSystemItemAdapterService {

    private static final Log log = LogFactory.getLog(FileSystemItemAdapterServiceImpl.class);

    public static final String FILE_SYSTEM_ITEM_FACTORY_EP = "fileSystemItemFactory";

    protected final Set<FileSystemItemFactoryDescriptor> factoryDescriptors = new TreeSet<FileSystemItemFactoryDescriptor>();

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {

        if (FILE_SYSTEM_ITEM_FACTORY_EP.equals(extensionPoint)) {
            FileSystemItemFactoryDescriptor desc = (FileSystemItemFactoryDescriptor) contribution;
            // Override
            if (factoryDescriptors.contains(desc)) {
                if (desc.isEnabled()) {
                    // No merge
                    factoryDescriptors.add(desc);
                } else {
                    factoryDescriptors.remove(desc);
                }
            }
            // New factory
            else {
                if (desc.isEnabled()) {
                    factoryDescriptors.add(desc);
                }
            }
        } else {
            log.error("Unknown extension point " + extensionPoint);
        }
    }

    /**
     * Iterates on the ordered contributed factories looking for the first one
     * that matches, in this order of precedence:
     * <ul>
     * <li>The factory contribution has no docType and no facet (this is the
     * case for the default factory {@code defaultFileSystemItemFactory} bound
     * to {@link DefaultFileSystemItemFactory})</li>
     * <li>The factory contribution has a matching docType</li>
     * <li>The factory contribution has a matching facet</li>
     * </ul>
     */
    @Override
    public FileSystemItem getFileSystemItemAdapter(DocumentModel doc)
            throws ClientException {

        FileSystemItemFactoryDescriptor matchingFactoryDesc = null;
        Iterator<FileSystemItemFactoryDescriptor> factoryDescriptorsIt = factoryDescriptors.iterator();
        while (factoryDescriptorsIt.hasNext()) {
            FileSystemItemFactoryDescriptor factoryDesc = factoryDescriptorsIt.next();
            if (StringUtils.isEmpty(factoryDesc.getDocType())
                    && StringUtils.isEmpty(factoryDesc.getFacet())) {
                matchingFactoryDesc = factoryDesc;
                break;
            }
            if (!StringUtils.isEmpty(factoryDesc.getDocType())
                    && factoryDesc.getDocType().equals(doc.getType())) {
                matchingFactoryDesc = factoryDesc;
                break;
            }
            if (!StringUtils.isEmpty(factoryDesc.getFacet())) {
                for (String docFacet : doc.getFacets()) {
                    if (factoryDesc.getFacet().equals(docFacet)) {
                        matchingFactoryDesc = factoryDesc;
                        break;
                    }
                }
                if (matchingFactoryDesc != null) {
                    break;
                }
            }
        }
        if (matchingFactoryDesc == null) {
            log.debug(String.format(
                    "No fileSystemItemFactory found for document %s, it cannot be part of the synchronized items. Please check the contributions to the following extension point: <extension target=\"org.nuxeo.drive.service.FileSystemItemAdapterService\" point=\"fileSystemItemFactory\">",
                    doc.getId()));
            return null;
        }

        FileSystemItemFactory factory = null;
        try {
            factory = matchingFactoryDesc.getFactory();
        } catch (Exception e) {
            throw new ClientException(
                    String.format(
                            "Error while trying to instantiate the class %s for the <fileSystemItemFactory> contribution named %s.",
                            matchingFactoryDesc.factoryClass.getName(),
                            matchingFactoryDesc.getName()), e);
        }
        return factory.getFileSystemItem(doc);
    }

    public Set<FileSystemItemFactoryDescriptor> getFactoryDescriptors() {
        return factoryDescriptors;
    }

}
