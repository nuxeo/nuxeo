/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thomas Roger
 */

package org.nuxeo.dam.importer.core;

import java.io.File;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.common.utils.Path;
import org.nuxeo.dam.Constants;
import org.nuxeo.dam.importer.core.helper.UnrestrictedSessionRunnerHelper;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.platform.importer.base.GenericMultiThreadedImporter;
import org.nuxeo.ecm.platform.importer.base.ImporterRunnerConfiguration;
import org.nuxeo.ecm.platform.importer.properties.MetadataCollector;
import org.nuxeo.ecm.platform.importer.source.FileSourceNode;
import org.nuxeo.ecm.platform.importer.source.FileWithMetadataSourceNode;
import org.nuxeo.ecm.platform.importer.source.SourceNode;
import org.nuxeo.runtime.transaction.TransactionHelper;

import static org.nuxeo.dam.Constants.IMPORT_FOLDER_TYPE;
import static org.nuxeo.dam.Constants.IMPORT_SET_TYPE;

/**
 * Default Importer for DAM.
 * <p>
 * All the imported files will be stored in a new {@code ImportSet}, created in
 * the given {@code ImportFolder}. If no ImportFolder is given, use the default
 * 'automatic-import' {@code ImportFolder}.
 * </p>
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class DamMultiThreadedImporter extends GenericMultiThreadedImporter {

    protected String importFolderPath;

    protected String importFolderTitle;

    protected String importSetTitle;

    protected boolean removeImportedFolder = false;

    public static DamMultiThreadedImporter createWithImportFolderPath(
            ImporterRunnerConfiguration configuration, String importFolderPath,
            String importSetTitle) throws Exception {
        return new DamMultiThreadedImporter(configuration, importFolderPath,
                null, importSetTitle, false);
    }

    public static DamMultiThreadedImporter createWithImportFolderPath(
            ImporterRunnerConfiguration configuration, String importFolderPath,
            String importSetTitle, boolean removeImportedFolder)
            throws Exception {
        return new DamMultiThreadedImporter(configuration, importFolderPath,
                null, importSetTitle, removeImportedFolder);
    }

    public static DamMultiThreadedImporter createWithImportFolderTitle(
            ImporterRunnerConfiguration configuration,
            String importFolderTitle, String importSetTitle) throws Exception {
        return new DamMultiThreadedImporter(configuration, null,
                importFolderTitle, importSetTitle, false);
    }

    public static DamMultiThreadedImporter createWithImportFolderTitle(
            ImporterRunnerConfiguration configuration,
            String importFolderTitle, String importSetTitle,
            boolean removeImportedFolder) throws Exception {
        return new DamMultiThreadedImporter(configuration, null,
                importFolderTitle, importSetTitle, removeImportedFolder);
    }

    protected DamMultiThreadedImporter(
            ImporterRunnerConfiguration configuration, String importFolderPath,
            String importFolderTitle, String importSetTitle,
            boolean removeImportedFolder) throws Exception {
        super(configuration);
        this.importFolderPath = importFolderPath;
        this.importFolderTitle = importFolderTitle;
        this.importSetTitle = importSetTitle;
        this.removeImportedFolder = removeImportedFolder;
    }

    @Override
    protected DocumentModel createTargetContainer() throws Exception {
        final TargetContainerCreator tcc = new TargetContainerCreator(
                getCoreSession().getRepositoryName(), importFolderPath,
                importFolderTitle, importSetTitle, importWritePath,
                importSource);
        UnrestrictedSessionRunnerHelper.runInNewThread(tcc);
        return getCoreSession().getDocument(tcc.targetContainer.getRef());
    }

    @Override
    protected void doRun() throws Exception {
        super.doRun();

        // remove the imported folder
        removeImportedFolder();
    }

    protected void removeImportedFolder() {
        if (removeImportedFolder) {
            if (importSource instanceof FileSourceNode) {
                FileSourceNode sourceNode = (FileSourceNode) importSource;
                FileUtils.deleteTree(sourceNode.getFile());
            }
        }
    }

}

/**
 * Create the {@code ImportSet} document where the assets will be imported in a
 * Unrestricted Session. Create, or get, the {@ImportFolder}
 * where the {@code ImportSet} wil lbe created.
 */
class TargetContainerCreator extends UnrestrictedSessionRunner {

    public static final String DEFAULT_IMPORT_FOLDER_TITLE = "Automatic Import";

    private static final Log log = LogFactory.getLog(TargetContainerCreator.class);

    public DocumentModel targetContainer;

    protected String importFolderPath;

    protected String importFolderTitle;

    protected String importSetTitle;

    protected String importWritePath;

    protected SourceNode importSource;

    public TargetContainerCreator(String repositoryName,
            String importFolderPath, String importFolderTitle,
            String importSetTitle, String importWritePath,
            SourceNode importSource) {
        super(repositoryName);
        this.importFolderPath = importFolderPath;
        this.importFolderTitle = importFolderTitle;
        this.importSetTitle = importSetTitle;
        this.importWritePath = importWritePath;
        this.importSource = importSource;
    }

    @Override
    public void run() throws ClientException {
        try {
            DocumentModel importFolder = getOrCreateImportFolder();
            targetContainer = createImportSet(importFolder);
            session.save();
        } catch (Exception e) {
            log.error(e, e);
        }
    }

    /**
     * Use the {@code importFolderPath} if not null to get or create the
     * ImportFolder, otherwise use the {@code importFolderTitle}.
     *
     * @return the existing or created ImportFolder
     */
    protected DocumentModel getOrCreateImportFolder() throws ClientException {
        DocumentRef importFolderRef;
        String importFolderName;
        if (importFolderPath != null && importFolderPath.trim().length() > 0) {
            importFolderRef = new PathRef(importFolderPath);
            Path p = new Path(importFolderPath);
            importFolderName = p.segment(p.segmentCount() - 1);
        } else {
            if (importFolderTitle == null
                    || importFolderTitle.trim().length() == 0) {
                importFolderTitle = DEFAULT_IMPORT_FOLDER_TITLE;
            }
            importFolderName = IdUtils.generateId(importFolderTitle);
            importFolderRef = new PathRef(importWritePath, importFolderName);
        }
        return getOrCreateImportFolder(importFolderRef, importFolderName);
    }

    protected DocumentModel getOrCreateImportFolder(
            DocumentRef importFolderRef, String importFolderName)
            throws ClientException {
        DocumentModel importFolder;
        if (!session.exists(importFolderRef)) {
            importFolder = session.createDocumentModel(importWritePath,
                    importFolderName, IMPORT_FOLDER_TYPE);
            importFolder.setPropertyValue(Constants.DUBLINCORE_TITLE_PROPERTY,
                    importFolderTitle);
            importFolder = session.createDocument(importFolder);
        } else {
            importFolder = session.getDocument(importFolderRef);
        }
        return importFolder;
    }

    protected DocumentModel createImportSet(DocumentModel importFolder)
            throws Exception {
        String title = importSetTitle != null
                && importSetTitle.trim().length() != 0 ? importSetTitle
                : generateImportSetName();
        DocumentModel importSet = session.createDocumentModel(
                importFolder.getPathAsString(), IdUtils.generateId(title),
                IMPORT_SET_TYPE);
        importSet.setPropertyValue(Constants.DUBLINCORE_TITLE_PROPERTY, title);

        importSet = setImportSetProperties(importSet);

        importSet = session.createDocument(importSet);
        return importSet;
    }

    protected String generateImportSetName() {
        Calendar calendar = Calendar.getInstance();
        // not thread-safe so don't use a static instance
        return new SimpleDateFormat("yyyyMMdd HH:mm").format(calendar.getTime());
    }

    protected DocumentModel setImportSetProperties(DocumentModel importSet)
            throws Exception {
        String sourcePath = importSource.getSourcePath();
        String metadataFilePath = sourcePath.endsWith(File.separator) ? sourcePath
                : sourcePath + File.separator;
        metadataFilePath += FileWithMetadataSourceNode.METADATA_FILENAME;
        File metadataFile = new File(metadataFilePath);
        if (metadataFile.exists()) {
            MetadataCollector metadataCollector = new MetadataCollector();
            metadataCollector.addPropertyFile(metadataFile);

            String contextPath = metadataFile.getAbsoluteFile().getParent();
            for (Map.Entry<String, Serializable> entry : metadataCollector.getProperties(
                    contextPath).entrySet()) {
                try {
                    importSet.setPropertyValue(entry.getKey(), entry.getValue());
                } catch (PropertyNotFoundException e) {
                    String message = String.format(
                            "Property '%s' not found on document type: %s. Skipping it.",
                            entry.getKey(), importSet.getType());
                    log.debug(message);
                }
            }
        }
        return importSet;
    }

}
