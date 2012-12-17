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
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.service.FileSystemItemAdapterService;
import org.nuxeo.drive.service.FileSystemItemFactory;
import org.nuxeo.drive.service.TopLevelFolderItemFactory;
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

    public static final String TOP_LEVEL_FOLDER_ITEM_FACTORY_EP = "topLevelFolderItemFactory";

    protected final FileSystemItemFactoryRegistry fileSystemItemFactoryRegistry = new FileSystemItemFactoryRegistry();

    protected final TopLevelFolderItemFactoryRegistry topLevelFolderItemFactoryRegistry = new TopLevelFolderItemFactoryRegistry();

    protected List<FileSystemItemFactoryWrapper> fileSystemItemFactories;

    /*------------------------ DefaultComponent -----------------------------*/
    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (FILE_SYSTEM_ITEM_FACTORY_EP.equals(extensionPoint)) {
            fileSystemItemFactoryRegistry.addContribution((FileSystemItemFactoryDescriptor) contribution);
        } else if (TOP_LEVEL_FOLDER_ITEM_FACTORY_EP.equals(extensionPoint)) {
            topLevelFolderItemFactoryRegistry.addContribution((TopLevelFolderItemFactoryDescriptor) contribution);
        } else {
            log.error("Unknown extension point " + extensionPoint);
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (FILE_SYSTEM_ITEM_FACTORY_EP.equals(extensionPoint)) {
            fileSystemItemFactoryRegistry.removeContribution((FileSystemItemFactoryDescriptor) contribution);
        } else if (TOP_LEVEL_FOLDER_ITEM_FACTORY_EP.equals(extensionPoint)) {
            topLevelFolderItemFactoryRegistry.removeContribution((TopLevelFolderItemFactoryDescriptor) contribution);
        } else {
            log.error("Unknown extension point " + extensionPoint);
        }
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        fileSystemItemFactories = null;
        super.deactivate(context);
    }

    /**
     * Sorts the contributed factories according to their order.
     */
    @Override
    public void applicationStarted(ComponentContext context) throws Exception {
        sortFileSystemItemFactories();
    }

    /*------------------------ FileSystemItemAdapterService -----------------------*/
    /**
     * Iterates on the ordered contributed file system item factories until it
     * finds one that matches and retrieves a non null {@link FileSystemItem}
     * for the given doc. A factory matches if:
     * <ul>
     * <li>It is not bound to any docType nor facet (this is the case for the
     * default factory contribution {@code defaultFileSystemItemFactory} bound
     * to {@link DefaultFileSystemItemFactory})</li>
     * <li>It is bound to a docType that matches the given doc's type</li>
     * <li>It is bound to a facet that matches one of the given doc's facets</li>
     * </ul>
     */
    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc)
            throws ClientException {
        return getFileSystemItem(doc, false, null);
    }

    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc, String parentId)
            throws ClientException {
        return getFileSystemItem(doc, true, parentId);
    }

    /**
     * Iterates on the ordered contributed file system item factories until if
     * finds one that can handle the given {@link FileSystemItem} id.
     */
    @Override
    public FileSystemItemFactory getFileSystemItemFactoryForId(String id)
            throws ClientException {
        Iterator<FileSystemItemFactoryWrapper> factoriesIt = fileSystemItemFactories.iterator();
        while (factoriesIt.hasNext()) {
            FileSystemItemFactoryWrapper factoryWrapper = factoriesIt.next();
            FileSystemItemFactory factory = factoryWrapper.getFactory();
            if (factory.canHandleFileSystemItemId(id)) {
                return factory;
            }
        }
        throw new ClientException(
                String.format(
                        "No fileSystemItemFactory found for FileSystemItem with id %s. Please check the contributions to the following extension point: <extension target=\"org.nuxeo.drive.service.FileSystemItemAdapterService\" point=\"fileSystemItemFactory\"> and make sure there is at least one defining a FileSystemItemFactory class for which the #canHandleFileSystemItemId(String id) method returns true.",
                        id));
    }

    @Override
    public TopLevelFolderItemFactory getTopLevelFolderItemFactory() {
        return topLevelFolderItemFactoryRegistry.factory;
    }

    /*------------------------- For test purpose ----------------------------------*/
    public Map<String, FileSystemItemFactoryDescriptor> getFileSystemItemFactoryDescriptors() {
        return fileSystemItemFactoryRegistry.factoryDescriptors;
    }

    public List<FileSystemItemFactoryWrapper> getFileSystemItemFactories() {
        return fileSystemItemFactories;
    }

    /*--------------------------- Protected ---------------------------------------*/
    protected void sortFileSystemItemFactories() throws Exception {
        fileSystemItemFactories = fileSystemItemFactoryRegistry.getOrderedFactories();
    }

    protected FileSystemItem getFileSystemItem(DocumentModel doc,
            boolean forceParentId, String parentId) throws ClientException {

        FileSystemItem fileSystemItem = null;
        FileSystemItemFactoryWrapper matchingFactory = null;
        Iterator<FileSystemItemFactoryWrapper> factoriesIt = fileSystemItemFactories.iterator();
        while (factoriesIt.hasNext()) {
            FileSystemItemFactoryWrapper factory = factoriesIt.next();
            if (generalFactoryMatches(factory)
                    || docTypeFactoryMatches(factory, doc)
                    || facetFactoryMatches(factory, doc)) {
                matchingFactory = factory;
                if (forceParentId) {
                    fileSystemItem = factory.getFactory().getFileSystemItem(
                            doc, parentId);
                } else {
                    fileSystemItem = factory.getFactory().getFileSystemItem(doc);
                }
                if (fileSystemItem != null) {
                    return fileSystemItem;
                }
            }
        }
        if (matchingFactory == null) {
            log.debug(String.format(
                    "No fileSystemItemFactory found matching with document %s => returning null. Please check the contributions to the following extension point: <extension target=\"org.nuxeo.drive.service.FileSystemItemAdapterService\" point=\"fileSystemItemFactory\">.",
                    doc.getId()));
        } else {
            log.debug(String.format(
                    "None of the fileSystemItemFactories were able to get a FileSystemItem adapter for document %s => returning null.",
                    doc.getId()));
        }
        return fileSystemItem;
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
            DocumentModel doc) throws ClientException {
        if (!StringUtils.isEmpty(factory.getFacet())) {
            for (String docFacet : doc.getFacets()) {
                if (factory.getFacet().equals(docFacet)) {
                    // Handle synchronization root case
                    if (NuxeoDriveManagerImpl.NUXEO_DRIVE_FACET.equals(docFacet)) {
                        return syncRootFactoryMatches(doc);
                    } else {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    protected boolean syncRootFactoryMatches(DocumentModel doc)
            throws ClientException {
        String userName = doc.getCoreSession().getPrincipal().getName();
        List<Map<String, Object>> subscriptions = (List<Map<String, Object>>) doc.getPropertyValue(NuxeoDriveManagerImpl.DRIVE_SUBSCRIPTIONS_PROPERTY);
        for (Map<String, Object> subscription : subscriptions) {
            if (userName.equals(subscription.get("username"))) {
                if (Boolean.TRUE.equals(subscription.get("enabled"))) {
                    return true;
                }
            }
        }
        return false;
    }

}
