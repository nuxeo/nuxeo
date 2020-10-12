/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.platform.filemanager.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.pathsegment.PathSegmentService;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.filemanager.api.FileImporterContext;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.ecm.platform.filemanager.service.extension.CreationContainerListProvider;
import org.nuxeo.ecm.platform.filemanager.service.extension.CreationContainerListProviderDescriptor;
import org.nuxeo.ecm.platform.filemanager.service.extension.FileImporter;
import org.nuxeo.ecm.platform.filemanager.service.extension.FileImporterDescriptor;
import org.nuxeo.ecm.platform.filemanager.service.extension.FolderImporter;
import org.nuxeo.ecm.platform.filemanager.service.extension.FolderImporterDescriptor;
import org.nuxeo.ecm.platform.filemanager.service.extension.UnicityExtension;
import org.nuxeo.ecm.platform.filemanager.service.extension.VersioningDescriptor;
import org.nuxeo.ecm.platform.filemanager.utils.FileManagerUtils;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.runtime.RuntimeMessage.Level;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.logging.DeprecationLogger;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * FileManager registry service.
 * <p>
 * This is the component to request to perform transformations. See API.
 *
 * @author <a href="mailto:andreas.kalogeropoulos@nuxeo.com">Andreas Kalogeropoulos</a>
 */
public class FileManagerService extends DefaultComponent implements FileManager {

    public static final ComponentName NAME = new ComponentName(
            "org.nuxeo.ecm.platform.filemanager.service.FileManagerService");

    public static final String DEFAULT_FOLDER_TYPE_NAME = "Folder";

    // TODO: OG: we should use an overridable query model instead of hardcoding
    // the NXQL query
    public static final String QUERY = "SELECT * FROM Document WHERE file:content/digest = '%s'";

    public static final int MAX = 15;

    /** @since 11.1 */
    public static final String PLUGINS_EP = "plugins";

    /** @since 11.1 */
    public static final String UNICITY_EP = "unicity";

    /** @since 11.1 */
    public static final String VERSIONING_EP = "versioning";

    private static final Logger log = LogManager.getLogger(FileManagerService.class);

    private Map<String, FileImporter> fileImporters;

    private List<FolderImporter> folderImporters;

    private List<CreationContainerListProvider> creationContainerListProviders;

    private List<String> fieldsXPath = new ArrayList<>();

    private boolean unicityEnabled = false;

    private String digestAlgorithm = "sha-256";

    private boolean computeDigest = false;

    public static final VersioningOption DEF_VERSIONING_OPTION = VersioningOption.MINOR;

    public static final boolean DEF_VERSIONING_AFTER_ADD = false;

    /**
     * @since 5.7
     * @deprecated since 9.1 automatic versioning is now handled at versioning service level, remove versioning
     *             behaviors from importers
     */
    @Deprecated(since = "9.1")
    private VersioningOption defaultVersioningOption = DEF_VERSIONING_OPTION;

    /**
     * @since 5.7
     * @deprecated since 9.1 automatic versioning is now handled at versioning service level, remove versioning
     *             behaviors from importers
     */
    @Deprecated(since = "9.1")
    private boolean versioningAfterAdd = DEF_VERSIONING_AFTER_ADD;

    @Override
    public void registerContribution(Object contribution, String xp, ComponentInstance component) {
        if (PLUGINS_EP.equals(xp)) {
            xp = computePluginsExtensionPoint(contribution.getClass());
        }
        super.registerContribution(contribution, xp, component);
    }

    @Override
    public void unregisterContribution(Object contribution, String xp, ComponentInstance component) {
        if (PLUGINS_EP.equals(xp)) {
            xp = computePluginsExtensionPoint(contribution.getClass());
        }
        super.unregisterContribution(contribution, xp, component);
    }

    @Override
    public void start(ComponentContext context) {
        super.start(context);

        registerFileImporters();
        registerFolderImporters();
        registerCreationContainerListProviders();
        registerUnicity();
        registerVersioning();
    }

    protected void registerFileImporters() {
        String xp = computePluginsExtensionPoint(FileImporterDescriptor.class);
        fileImporters = getDescriptors(xp).stream()
                                          .map(FileImporterDescriptor.class::cast)
                                          .map(FileImporterDescriptor::newInstance)
                                          .collect(Collectors.toMap(FileImporter::getName, Function.identity()));
    }

    protected void registerFolderImporters() {
        String xp = computePluginsExtensionPoint(FolderImporterDescriptor.class);
        folderImporters = getDescriptors(xp).stream()
                                            .map(FolderImporterDescriptor.class::cast)
                                            .map(FolderImporterDescriptor::newInstance)
                                            .collect(Collectors.toList());
    }

    protected void registerCreationContainerListProviders() {
        String xp = computePluginsExtensionPoint(CreationContainerListProviderDescriptor.class);
        creationContainerListProviders = getDescriptors(xp).stream()
                                                           .map(CreationContainerListProviderDescriptor.class::cast)
                                                           .map(CreationContainerListProviderDescriptor::newInstance)
                                                           .collect(Collectors.toList());
    }

    protected void registerUnicity() {
        getDescriptors(UNICITY_EP).stream().map(UnicityExtension.class::cast).forEach(unicityExtension -> {
            if (unicityExtension.getAlgo() != null) {
                digestAlgorithm = unicityExtension.getAlgo();
            }
            if (unicityExtension.getEnabled() != null) {
                unicityEnabled = unicityExtension.getEnabled();
            }
            if (unicityExtension.getFields() != null) {
                fieldsXPath = unicityExtension.getFields();
            } else {
                fieldsXPath.add("file:content");
            }
            if (unicityExtension.getComputeDigest() != null) {
                computeDigest = unicityExtension.getComputeDigest();
            }
        });
    }

    /**
     * @deprecated since 9.1
     */
    @Deprecated(since = "9.1")
    protected void registerVersioning() {
        getDescriptors(VERSIONING_EP).stream().map(VersioningDescriptor.class::cast).forEach(versioningDescriptor -> {
            String message = "Extension point 'versioning' has been deprecated and corresponding behavior removed from "
                    + "Nuxeo Platform. Please use versioning policy instead.";
            DeprecationLogger.log(message, "9.1");
            addRuntimeMessage(Level.WARNING, message);

            String defver = versioningDescriptor.defaultVersioningOption;
            if (!StringUtils.isBlank(defver)) {
                try {
                    defaultVersioningOption = VersioningOption.valueOf(defver.toUpperCase(Locale.ENGLISH));
                } catch (IllegalArgumentException e) {
                    log.warn("Illegal versioning option: {}, using {} instead", defver, DEF_VERSIONING_OPTION);
                    defaultVersioningOption = DEF_VERSIONING_OPTION;
                }
            }
            if (versioningDescriptor.versionAfterAdd != null) {
                versioningAfterAdd = versioningDescriptor.versionAfterAdd;
            }
        });
    }

    protected String computePluginsExtensionPoint(Class<?> klass) {
        return String.format("%s-%s", PLUGINS_EP, klass.getSimpleName());
    }

    private Blob checkMimeType(Blob blob, String fullname) {
        String filename = FileManagerUtils.fetchFileName(fullname);
        blob = Framework.getService(MimetypeRegistry.class).updateMimetype(blob, filename, true);
        return blob;
    }

    @Override
    public DocumentModel createFolder(CoreSession documentManager, String fullname, String path, boolean overwrite)
            throws IOException {

        if (folderImporters.isEmpty()) {
            return defaultCreateFolder(documentManager, fullname, path, overwrite);
        } else {
            // use the last registered folder importer
            FolderImporter folderImporter = folderImporters.get(folderImporters.size() - 1);
            return folderImporter.create(documentManager, fullname, path, overwrite,
                    Framework.getService(TypeManager.class));
        }
    }

    /**
     * @deprecated since 9.1, use {@link #defaultCreateFolder(CoreSession, String, String, boolean)} instead
     */
    @Deprecated(since = "9.1")
    public DocumentModel defaultCreateFolder(CoreSession documentManager, String fullname, String path) {
        return defaultCreateFolder(documentManager, fullname, path, true);
    }

    /**
     * @since 9.1
     */
    public DocumentModel defaultCreateFolder(CoreSession documentManager, String fullname, String path,
            boolean overwrite) {
        return defaultCreateFolder(documentManager, fullname, path, DEFAULT_FOLDER_TYPE_NAME, true, overwrite);
    }

    /**
     * @deprecated since 9.1, use {@link #defaultCreateFolder(CoreSession, String, String, String, boolean, boolean)}
     *             instead
     */
    @Deprecated(since = "9.1")
    public DocumentModel defaultCreateFolder(CoreSession documentManager, String fullname, String path,
            String containerTypeName, boolean checkAllowedSubTypes) {
        return defaultCreateFolder(documentManager, fullname, path, containerTypeName, checkAllowedSubTypes, true);
    }

    /**
     * @since 9.1
     */
    public DocumentModel defaultCreateFolder(CoreSession documentManager, String fullname, String path,
            String containerTypeName, boolean checkAllowedSubTypes, boolean overwrite) {

        // Fetching filename
        String title = FileManagerUtils.fetchFileName(fullname);

        if (overwrite) {
            // Looking if an existing Folder with the same filename exists.
            DocumentModel docModel = FileManagerUtils.getExistingDocByTitle(documentManager, path, title);
            if (docModel != null) {
                return docModel;
            }
        }

        // check permissions
        PathRef containerRef = new PathRef(path);
        if (!documentManager.hasPermission(containerRef, SecurityConstants.READ_PROPERTIES)
                || !documentManager.hasPermission(containerRef, SecurityConstants.ADD_CHILDREN)) {
            throw new DocumentSecurityException("Not enough rights to create folder");
        }

        // check allowed sub types
        DocumentModel container = documentManager.getDocument(containerRef);
        if (checkAllowedSubTypes && !Framework.getService(TypeManager.class)
                                              .isAllowedSubType(containerTypeName, container.getType(), container)) {
            // cannot create document file here
            // TODO: we should better raise a dedicated exception to be
            // catched by the FileManageActionsBean instead of returning
            // null
            return null;
        }

        PathSegmentService pss = Framework.getService(PathSegmentService.class);
        DocumentModel docModel = documentManager.createDocumentModel(containerTypeName);
        docModel.setProperty("dublincore", "title", title);

        // writing changes
        docModel.setPathInfo(path, pss.generatePathSegment(docModel));
        docModel = documentManager.createDocument(docModel);
        documentManager.save();

        log.debug("Created container: {} with type {}", docModel::getName, () -> containerTypeName);
        return docModel;
    }

    @Override
    public DocumentModel createDocumentFromBlob(CoreSession documentManager, Blob input, String path, boolean overwrite,
            String fullName) throws IOException {
        return createDocumentFromBlob(documentManager, input, path, overwrite, fullName, false);
    }

    @Override
    public DocumentModel createDocumentFromBlob(CoreSession documentManager, Blob input, String path, boolean overwrite,
            String fullName, boolean noMimeTypeCheck) throws IOException {
        FileImporterContext context = FileImporterContext.builder(documentManager, input, path)
                                                         .overwrite(overwrite)
                                                         .fileName(fullName)
                                                         .mimeTypeCheck(!noMimeTypeCheck)
                                                         .build();
        return createOrUpdateDocument(context);
    }

    @Override
    public DocumentModel createOrUpdateDocument(FileImporterContext context) throws IOException {
        Blob blob = context.getBlob();

        // check mime type to be able to select the best importer plugin
        if (context.isMimeTypeCheck()) {
            blob = checkMimeType(blob, context.getFileName());
        }

        List<FileImporter> importers = new ArrayList<>(fileImporters.values());
        Collections.sort(importers);
        String mimeType = blob.getMimeType();
        String normalizedMimeType = Framework.getService(MimetypeRegistry.class)
                                             .getMimetypeEntryByMimeType(mimeType)
                                             .getNormalized();
        for (FileImporter importer : importers) {
            if (isImporterAvailable(importer, normalizedMimeType, mimeType, context.isExcludeOneToMany())) {
                DocumentModel doc = importer.createOrUpdate(context);
                if (doc != null) {
                    return doc;
                }
            }
        }
        return null;
    }

    protected boolean isImporterAvailable(FileImporter importer, String normalizedMimeType, String mimeType,
            boolean excludeOneToMany) {
        return importer.isEnabled() && !(importer.isOneToMany() && excludeOneToMany)
                && (importer.matches(normalizedMimeType) || importer.matches(mimeType));
    }

    @Override
    public DocumentModel updateDocumentFromBlob(CoreSession documentManager, Blob input, String path, String fullName) {
        String filename = FileManagerUtils.fetchFileName(fullName);
        DocumentModel doc = FileManagerUtils.getExistingDocByFileName(documentManager, path, filename);
        if (doc != null) {
            doc.setProperty("file", "content", input);

            documentManager.saveDocument(doc);
            documentManager.save();

            log.debug("Updated the document: {}", doc::getName);
        }
        return doc;
    }

    public FileImporter getPluginByName(String name) {
        return fileImporters.get(name);
    }

    @Override
    public List<DocumentLocation> findExistingDocumentWithFile(CoreSession documentManager, String path, String digest,
            NuxeoPrincipal principal) {
        String nxql = String.format(QUERY, digest);
        DocumentModelList documentModelList = documentManager.query(nxql, MAX);
        List<DocumentLocation> docLocationList = new ArrayList<>(documentModelList.size());
        for (DocumentModel documentModel : documentModelList) {
            docLocationList.add(new DocumentLocationImpl(documentModel));
        }
        return docLocationList;
    }

    @Override
    public boolean isUnicityEnabled() {
        return unicityEnabled;
    }

    @Override
    public boolean isDigestComputingEnabled() {
        return computeDigest;
    }

    @Override
    public List<String> getFields() {
        return fieldsXPath;
    }

    @Override
    public DocumentModelList getCreationContainers(NuxeoPrincipal principal, String docType) {
        DocumentModelList containers = new DocumentModelListImpl();
        RepositoryManager repositoryManager = Framework.getService(RepositoryManager.class);
        for (String repositoryName : repositoryManager.getRepositoryNames()) {
            CoreSession session = CoreInstance.getCoreSession(repositoryName, principal);
            DocumentModelList docs = getCreationContainers(session, docType);
            docs.forEach(doc -> doc.detach(true));
            containers.addAll(docs);
        }
        return containers;
    }

    @Override
    public DocumentModelList getCreationContainers(CoreSession documentManager, String docType) {
        for (CreationContainerListProvider provider : creationContainerListProviders) {
            if (provider.accept(docType)) {
                return provider.getCreationContainerList(documentManager, docType);
            }
        }
        return new DocumentModelListImpl();
    }

    @Override
    public String getDigestAlgorithm() {
        return digestAlgorithm;
    }

    /**
     * @deprecated since 9.1 automatic versioning is now handled at versioning service level, remove versioning
     *             behaviors from importers
     */
    @Override
    @Deprecated(since = "9.1")
    public VersioningOption getVersioningOption() {
        return defaultVersioningOption;
    }

    /**
     * @deprecated since 9.1 automatic versioning is now handled at versioning service level, remove versioning
     *             behaviors from importers
     */
    @Override
    @Deprecated(since = "9.1")
    public boolean doVersioningAfterAdd() {
        return versioningAfterAdd;
    }

}
