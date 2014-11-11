/*
 * (C) Copyright 2002 - 2006 Nuxeo SARL <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 *
 * $Id: Registry.java 2531 2006-09-04 23:01:57Z janguenot $
 */

package org.nuxeo.ecm.platform.filemanager.service;

import java.io.IOException;
import java.io.Serializable;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Base64;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.pathsegment.PathSegmentService;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.ecm.platform.filemanager.service.extension.CreationContainerListProvider;
import org.nuxeo.ecm.platform.filemanager.service.extension.CreationContainerListProviderDescriptor;
import org.nuxeo.ecm.platform.filemanager.service.extension.FileImporter;
import org.nuxeo.ecm.platform.filemanager.service.extension.FileImporterDescriptor;
import org.nuxeo.ecm.platform.filemanager.service.extension.FolderImporter;
import org.nuxeo.ecm.platform.filemanager.service.extension.FolderImporterDescriptor;
import org.nuxeo.ecm.platform.filemanager.service.extension.UnicityExtension;
import org.nuxeo.ecm.platform.filemanager.utils.FileManagerUtils;
import org.nuxeo.ecm.platform.mimetype.MimetypeDetectionException;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;

/**
 * FileManager registry service.
 * <p>
 * This is the component to request to perform transformations. See API.
 *
 * @author <a href="mailto:andreas.kalogeropoulos@nuxeo.com">Andreas
 *         Kalogeropoulos</a>
 */
public class FileManagerService extends DefaultComponent implements FileManager {

    public static final ComponentName NAME = new ComponentName(
            "org.nuxeo.ecm.platform.filemanager.service.FileManagerService");

    public static final String DEFAULT_FOLDER_TYPE_NAME = "Folder";

    // TODO: OG: we should use an overridable query model instead of hardcoding
    // the NXQL query
    public static final String QUERY = "SELECT * FROM Document WHERE file:content:digest = '%s'";

    public static final int MAX = 15;

    private static final Log log = LogFactory.getLog(FileManagerService.class);

    private final Map<String, FileImporter> fileImporters;

    private final List<FolderImporter> folderImporters;

    private final List<CreationContainerListProvider> creationContainerListProviders;

    private List<String> fieldsXPath = new ArrayList<String>();

    private MimetypeRegistry mimeService;

    private boolean unicityEnabled = false;

    private String digestAlgorithm = "sha-256";

    private boolean computeDigest = false;

    private TypeManager typeService;

    private RepositoryManager repositoryManager;

    public FileManagerService() {
        fileImporters = new HashMap<String, FileImporter>();
        folderImporters = new LinkedList<FolderImporter>();
        creationContainerListProviders = new LinkedList<CreationContainerListProvider>();
    }

    private MimetypeRegistry getMimeService() throws ClientException {
        if (mimeService == null) {
            try {
                mimeService = Framework.getService(MimetypeRegistry.class);
            } catch (Exception e) {
                throw new ClientException(e);
            }
        }
        return mimeService;
    }

    private TypeManager getTypeService() throws ClientException {
        if (typeService == null) {
            try {
                typeService = Framework.getService(TypeManager.class);
            } catch (Exception e) {
                throw new ClientException(e);
            }
        }
        return typeService;
    }

    private RepositoryManager getRepositoryManager() throws ClientException {
        if (repositoryManager == null) {
            try {
                repositoryManager = Framework.getService(RepositoryManager.class);
            } catch (Exception e) {
                throw new ClientException(e);
            }
        }
        return repositoryManager;
    }

    private Blob checkMimeType(Blob blob, String fullname)
            throws ClientException {
        final String mimeType = blob.getMimeType();
        if (mimeType != null && !mimeType.equals("application/octet-stream")) {
            return blob;
        }
        String filename = FileManagerUtils.fetchFileName(fullname);
        try {
            blob = getMimeService().updateMimetype(blob, filename);
        } catch (MimetypeDetectionException e) {
            throw new ClientException(e);
        }
        return blob;
    }

    public DocumentModel createFolder(CoreSession documentManager,
            String fullname, String path) throws ClientException, IOException {

        if (folderImporters.isEmpty()) {
            return defaultCreateFolder(documentManager, fullname, path);
        } else {
            // use the last registered folder importer
            FolderImporter folderImporter = folderImporters.get(folderImporters.size() - 1);
            return folderImporter.create(documentManager, fullname, path, true,
                    getTypeService());
        }
    }

    public DocumentModel defaultCreateFolder(CoreSession documentManager,
            String fullname, String path) throws ClientException {
        return defaultCreateFolder(documentManager, fullname, path,
                DEFAULT_FOLDER_TYPE_NAME, true);
    }

    public DocumentModel defaultCreateFolder(CoreSession documentManager,
            String fullname, String path, String containerTypeName,
            boolean checkAllowedSubTypes) throws ClientException {

        // Fetching filename
        String title = FileManagerUtils.fetchFileName(fullname);

        // Looking if an existing Folder with the same filename exists.
        DocumentModel docModel = FileManagerUtils.getExistingDocByTitle(
                documentManager, path, title);

        if (docModel == null) {
            // check permissions
            PathRef containerRef = new PathRef(path);
            if (!documentManager.hasPermission(containerRef,
                    SecurityConstants.READ_PROPERTIES)
                    || !documentManager.hasPermission(containerRef,
                            SecurityConstants.ADD_CHILDREN)) {
                throw new DocumentSecurityException(
                        "Not enough rights to create folder");
            }

            // check allowed sub types
            DocumentModel container = documentManager.getDocument(containerRef);
            if (checkAllowedSubTypes
                    && !getTypeService().isAllowedSubType(containerTypeName,
                            container.getType(), container)) {
                // cannot create document file here
                // TODO: we should better raise a dedicated exception to be
                // catched by the FileManageActionsBean instead of returning
                // null
                return null;
            }

            PathSegmentService pss;
            try {
                pss = Framework.getService(PathSegmentService.class);
            } catch (Exception e) {
                throw new ClientException(e);
            }
            docModel = documentManager.createDocumentModel(containerTypeName);
            docModel.setProperty("dublincore", "title", title);

            // writing changes
            docModel.setPathInfo(path, pss.generatePathSegment(docModel));
            docModel = documentManager.createDocument(docModel);
            documentManager.save();

            log.debug("Created container: " + docModel.getName()
                    + " with type " + containerTypeName);
        }
        return docModel;
    }

    public DocumentModel createDocumentFromBlob(CoreSession documentManager,
            Blob input, String path, boolean overwrite, String fullName)
            throws IOException, ClientException {

        // check mime type to be able to select the best importer plugin
        input = checkMimeType(input, fullName);

        List<FileImporter> importers = new ArrayList<FileImporter>(
                fileImporters.values());
        Collections.sort(importers);
        for (FileImporter importer : importers) {
            if (importer.isEnabled() && importer.matches(input.getMimeType())) {
                DocumentModel doc = importer.create(documentManager, input,
                        path, overwrite, fullName, getTypeService());
                if (doc != null) {
                    return doc;
                }
            }
        }
        return null;
    }

    public DocumentModel updateDocumentFromBlob(CoreSession documentManager,
            Blob input, String path, String fullName) throws ClientException {
        String filename = FileManagerUtils.fetchFileName(fullName);
        DocumentModel doc = FileManagerUtils.getExistingDocByFileName(
                documentManager, path, filename);
        if (doc != null) {
            doc.setProperty("file", "content", input);

            documentManager.saveDocument(doc);
            documentManager.save();

            log.debug("Updated the document: " + doc.getName());
        }
        return doc;
    }

    public FileImporter getPluginByName(String name) {
        return fileImporters.get(name);
    }

    @Override
    public void registerExtension(Extension extension) throws Exception {
        if (extension.getExtensionPoint().equals("plugins")) {
            Object[] contribs = extension.getContributions();
            for (Object contrib : contribs) {
                if (contrib instanceof FileImporterDescriptor) {
                    registerFileImporter((FileImporterDescriptor) contrib,
                            extension);
                } else if (contrib instanceof FolderImporterDescriptor) {
                    registerFolderImporter((FolderImporterDescriptor) contrib,
                            extension);
                } else if (contrib instanceof CreationContainerListProviderDescriptor) {
                    registerCreationContainerListProvider(
                            (CreationContainerListProviderDescriptor) contrib,
                            extension);
                }
            }
        } else if (extension.getExtensionPoint().equals("unicity")) {
            Object[] contribs = extension.getContributions();
            for (Object contrib : contribs) {
                if (contrib instanceof UnicityExtension) {
                    registerUnicityOptions((UnicityExtension) contrib,
                            extension);
                }
            }

        } else {
            log.warn(String.format("Unknown contribution %s: ignored",
                    extension.getExtensionPoint()));
        }
    }

    @Override
    public void unregisterExtension(Extension extension) throws Exception {
        if (extension.getExtensionPoint().equals("plugins")) {
            Object[] contribs = extension.getContributions();

            for (Object contrib : contribs) {
                if (contrib instanceof FileImporterDescriptor) {
                    unregisterFileImporter((FileImporterDescriptor) contrib);
                } else if (contrib instanceof FolderImporterDescriptor) {
                    unregisterFolderImporter((FolderImporterDescriptor) contrib);
                } else if (contrib instanceof CreationContainerListProviderDescriptor) {
                    unregisterCreationContainerListProvider((CreationContainerListProviderDescriptor) contrib);
                }
            }
        } else if (extension.getExtensionPoint().equals("unicity")) {

        } else {
            log.warn(String.format("Unknown contribution %s: ignored",
                    extension.getExtensionPoint()));
        }
    }

    private void registerUnicityOptions(UnicityExtension unicityExtension,
            Extension extension) {
        if (unicityExtension.getAlgo() != null) {
            digestAlgorithm = unicityExtension.getAlgo();
        }
        if (unicityExtension.getEnabled() != null) {
            unicityEnabled = unicityExtension.getEnabled().booleanValue();
        }
        if (unicityExtension.getFields() != null) {
            fieldsXPath = unicityExtension.getFields();
        } else {
            fieldsXPath.add("file:content");
        }
        if (unicityExtension.getComputeDigest() != null) {
            computeDigest = unicityExtension.getComputeDigest().booleanValue();
        }
    }

    private void registerFileImporter(FileImporterDescriptor pluginExtension,
            Extension extension) throws Exception {
        String name = pluginExtension.getName();
        String className = pluginExtension.getClassName();

        FileImporter plugin;
        if (fileImporters.containsKey(name)) {
            log.info("Overriding file importer plugin " + name);
            if (className != null) {
                plugin = (FileImporter) extension.getContext().loadClass(
                        className).newInstance();
                fileImporters.put(name, plugin);
            } else {
                plugin = fileImporters.get(name);
            }
        } else {
            plugin = (FileImporter) extension.getContext().loadClass(className).newInstance();
        }

        List<String> filters = pluginExtension.getFilters();
        if (filters != null && !filters.isEmpty()) {
            plugin.setFilters(filters);
        }
        plugin.setName(name);
        plugin.setFileManagerService(this);
        plugin.setEnabled(pluginExtension.isEnabled());
        plugin.setOrder(pluginExtension.getOrder());
        fileImporters.put(name, plugin);
        log.info("Registered file importer " + name);
    }

    private void unregisterFileImporter(FileImporterDescriptor pluginExtension) {
        String name = pluginExtension.getName();
        fileImporters.remove(name);
        log.info("unregistered file importer: " + name);
    }

    private void registerFolderImporter(
            FolderImporterDescriptor folderImporterDescriptor,
            Extension extension) throws Exception {

        String name = folderImporterDescriptor.getName();
        String className = folderImporterDescriptor.getClassName();

        FolderImporter folderImporter = (FolderImporter) extension.getContext().loadClass(
                className).newInstance();
        folderImporter.setName(name);
        folderImporter.setFileManagerService(this);
        folderImporters.add(folderImporter);
        log.info("registered folder importer: " + name);
    }

    private void unregisterFolderImporter(
            FolderImporterDescriptor folderImporterDescriptor) {
        String name = folderImporterDescriptor.getName();
        FolderImporter folderImporterToRemove = null;
        for (FolderImporter folderImporter : folderImporters) {
            if (name.equals(folderImporter.getName())) {
                folderImporterToRemove = folderImporter;
            }
        }
        if (folderImporterToRemove != null) {
            folderImporters.remove(folderImporterToRemove);
        }
        log.info("unregistered folder importer: " + name);
    }

    private void registerCreationContainerListProvider(
            CreationContainerListProviderDescriptor ccListProviderDescriptor,
            Extension extension) throws Exception {

        String name = ccListProviderDescriptor.getName();
        String[] docTypes = ccListProviderDescriptor.getDocTypes();
        String className = ccListProviderDescriptor.getClassName();

        CreationContainerListProvider provider = (CreationContainerListProvider) extension.getContext().loadClass(
                className).newInstance();
        provider.setName(name);
        provider.setDocTypes(docTypes);
        if (creationContainerListProviders.contains(provider)) {
            // equality and containment tests are based on unique names
            creationContainerListProviders.remove(provider);
        }
        // add the new provider at the beginning of the list
        creationContainerListProviders.add(0, provider);
        log.info("registered creationContaineterList provider: " + name);
    }

    private void unregisterCreationContainerListProvider(
            CreationContainerListProviderDescriptor ccListProviderDescriptor) {
        String name = ccListProviderDescriptor.getName();
        CreationContainerListProvider providerToRemove = null;
        for (CreationContainerListProvider provider : creationContainerListProviders) {
            if (name.equals(provider.getName())) {
                providerToRemove = provider;
                break;
            }
        }
        if (providerToRemove != null) {
            creationContainerListProviders.remove(providerToRemove);
        }
        log.info("unregistered creationContaineterList provider: " + name);
    }

    // XXX: bad exception management here: unexpected Exceptions should not be
    // logged but be un-catched so that the caller can handle them properly
    public String computeDigest(Blob blob) throws NoSuchAlgorithmException,
            IOException {

        MessageDigest md = MessageDigest.getInstance(digestAlgorithm);

        DigestInputStream dis = new DigestInputStream(blob.getStream(), md);
        while (dis.available() > 0) {
            dis.read();
        }
        byte[] b = md.digest();
        String base64Digest = Base64.encodeBytes(b);
        return base64Digest;
    }

    public List<DocumentLocation> findExistingDocumentWithFile(
            CoreSession documentManager, String path, String digest,
            Principal principal) throws ClientException {
        String nxql = String.format(QUERY, digest);
        DocumentModelList documentModelList = documentManager.query(nxql, MAX);
        List<DocumentLocation> docLocationList = new ArrayList<DocumentLocation>(
                documentModelList.size());
        for (DocumentModel documentModel : documentModelList) {
            docLocationList.add(new DocumentLocationImpl(documentModel));
        }
        return docLocationList;
    }

    public boolean isUnicityEnabled() {
        return unicityEnabled;
    }

    public boolean isDigestComputingEnabled() {
        return computeDigest;
    }

    public List<String> getFields() {
        return fieldsXPath;
    }

    public DocumentModelList getCreationContainers(Principal principal,
            String docType) throws Exception {
        DocumentModelList containers = new DocumentModelListImpl();
        for (Repository repo : getRepositoryManager().getRepositories()) {
            CoreSession session = null;
            try {
                Map<String, Serializable> context = new HashMap<String, Serializable>();
                context.put("principal", (Serializable) principal);
                session = repo.open(context);
                containers.addAll(getCreationContainers(session, docType));
            } finally {
                if (session != null) {
                    CoreInstance.getInstance().close(session);
                }
            }
        }
        return containers;
    }

    public DocumentModelList getCreationContainers(CoreSession documentManager,
            String docType) throws Exception {
        for (CreationContainerListProvider provider : creationContainerListProviders) {
            if (provider.accept(docType)) {
                return provider.getCreationContainerList(documentManager,
                        docType);
            }
        }
        return new DocumentModelListImpl();
    }

    public String getDigestAlgorithm() {
        return digestAlgorithm;
    }

}
