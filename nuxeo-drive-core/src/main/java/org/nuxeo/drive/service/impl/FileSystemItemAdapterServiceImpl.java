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

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.service.FileSystemItemAdapterService;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.model.ComponentContext;
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

    protected final Map<String, FileSystemItemFactoryDescriptor> factoryDescriptors = new HashMap<String, FileSystemItemFactoryDescriptor>();

    protected final List<FileSystemItemFactoryWrapper> factories = new ArrayList<FileSystemItemFactoryWrapper>();

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {

        if (FILE_SYSTEM_ITEM_FACTORY_EP.equals(extensionPoint)) {
            FileSystemItemFactoryDescriptor desc = (FileSystemItemFactoryDescriptor) contribution;
            String descName = desc.getName();
            // Override
            if (factoryDescriptors.containsKey(descName)) {
                if (desc.isEnabled()) {
                    // No merge
                    factoryDescriptors.put(descName, desc);
                } else {
                    factoryDescriptors.remove(descName);
                }
            }
            // New factory
            else {
                if (desc.isEnabled()) {
                    factoryDescriptors.put(descName, desc);
                }
            }
        } else {
            log.error("Unknown extension point " + extensionPoint);
        }
    }

    /**
     * Sorts the contributed factories according to their order.
     */
    @Override
    public void applicationStarted(ComponentContext context) throws Exception {
        sortFactories();
    }

    /**
     * Iterates on the ordered contributed factories until it finds one that
     * matches and retrieves a non null {@link FileSystemItem} adapter for the
     * given doc. A factory matches if:
     * <ul>
     * <li>It is not bound to any docType nor facet (this is the case for the
     * default factory contribution {@code defaultFileSystemItemFactory} bound
     * to {@link DefaultFileSystemItemFactory})</li>
     * <li>It is bound to a docType that matches the given doc's type</li>
     * <li>It is bound to a facet that matches one of the given doc's facets</li>
     * </ul>
     */
    @Override
    public FileSystemItem getFileSystemItemAdapter(DocumentModel doc)
            throws ClientException {

        FileSystemItemFactoryWrapper matchingFactory = null;
        Iterator<FileSystemItemFactoryWrapper> factoriesIt = factories.iterator();
        while (factoriesIt.hasNext()) {
            FileSystemItemFactoryWrapper factory = factoriesIt.next();
            if (generalFactoryMatches(factory)
                    || docTypeFactoryMatches(factory, doc)
                    || facetFactoryMatches(factory, doc)) {
                matchingFactory = factory;
                FileSystemItem fileSystemItem = factory.getFactory().getFileSystemItem(
                        doc);
                if (fileSystemItem != null) {
                    return fileSystemItem;
                }
            }
        }
        if (matchingFactory == null) {
            log.debug(String.format(
                    "No fileSystemItemFactory found matching with document %s => returning null. Please check the contributions to the following extension point: <extension target=\"org.nuxeo.drive.service.FileSystemItemAdapterService\" point=\"fileSystemItemFactory\">",
                    doc.getId()));
        } else {
            log.debug(String.format(
                    "None of the fileSystemItemFactories were able to get a FileSystemItem adapter for document %s => returning null.",
                    doc.getId()));
        }
        return null;
    }

    /**
     * Iterates on the ordered contributed factories until if finds one that
     * retrieves a non null {@link FileSystemItem} adapter for the given id and
     * principal.
     */
    @Override
    public FileSystemItem getFileSystemItemById(String id, Principal principal)
            throws ClientException {
        Iterator<FileSystemItemFactoryWrapper> factoriesIt = factories.iterator();
        while (factoriesIt.hasNext()) {
            FileSystemItemFactoryWrapper factory = factoriesIt.next();
            FileSystemItem fileSystemItem = factory.getFactory().getFileSystemItemById(
                    id, principal);
            if (fileSystemItem != null) {
                return fileSystemItem;
            }
        }
        return null;
    }

    /*------------------------- For test purpose ----------------------------------*/
    public Map<String, FileSystemItemFactoryDescriptor> getFactoryDescriptors() {
        return factoryDescriptors;
    }

    public List<FileSystemItemFactoryWrapper> getFactories() {
        return factories;
    }

    // TODO: remove when better solution found for hot deploy
    public void recomputeFactories() throws Exception {
        factories.clear();
        sortFactories();
    }

    /*--------------------------- Protected ----------------------------------------*/
    protected void sortFactories() throws Exception {
        List<FileSystemItemFactoryDescriptor> orderedFactoryDescriptors = new ArrayList<FileSystemItemFactoryDescriptor>(
                factoryDescriptors.values());
        Collections.sort(orderedFactoryDescriptors);
        for (FileSystemItemFactoryDescriptor factoryDesc : orderedFactoryDescriptors) {
            FileSystemItemFactoryWrapper factoryWrapper = new FileSystemItemFactoryWrapper(
                    factoryDesc.getDocType(), factoryDesc.getFacet(),
                    factoryDesc.getFactory());
            factories.add(factoryWrapper);
        }
    }

    protected boolean generalFactoryMatches(FileSystemItemFactoryWrapper factory) {
        return StringUtils.isEmpty(factory.getDocType())
                && StringUtils.isEmpty(factory.getFacet());
    }

    protected boolean docTypeFactoryMatches(
            FileSystemItemFactoryWrapper factory, DocumentModel doc) {
        return !StringUtils.isEmpty(factory.getDocType())
                && factory.getDocType().equals(doc.getType());
    }

    protected boolean facetFactoryMatches(FileSystemItemFactoryWrapper factory,
            DocumentModel doc) {
        if (!StringUtils.isEmpty(factory.getFacet())) {
            for (String docFacet : doc.getFacets()) {
                if (factory.getFacet().equals(docFacet)) {
                    return true;
                }
            }
        }
        return false;
    }

}
