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

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.platform.importer.base.GenericMultiThreadedImporter;
import org.nuxeo.ecm.platform.importer.base.ImporterRunnerConfiguration;
import org.nuxeo.ecm.platform.importer.base.TxHelper;
import org.nuxeo.ecm.platform.importer.properties.MetadataCollector;
import org.nuxeo.ecm.platform.importer.source.FileSourceNode;
import org.nuxeo.ecm.platform.importer.source.FileWithMetadataSourceNode;

import static org.nuxeo.dam.core.Constants.IMPORT_FOLDER_TYPE;
import static org.nuxeo.dam.core.Constants.IMPORT_SET_TYPE;

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

    public static final String DEFAULT_IMPORT_FOLDER_TITLE = "Automatic Import";

    public static final String DUBLINCORE_TITLE_PROPERTY = "dc:title";

    public static DateFormat IMPORT_SET_NAME_FORMAT = new SimpleDateFormat(
            "yyyyMMdd HH:mm");

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
        TxHelper txHelper = new TxHelper();
        //txHelper.beginNewTransaction(600);
        txHelper.grabCurrentTransaction(600);
        DocumentModel importFolder = getOrCreateImportFolder();
        DocumentModel importset = createImportSet(importFolder);
        getCoreSession().save();
        txHelper.commitOrRollbackTransaction();
        return importset;
    }

    /**
     * Use the {@code importFolderPath} if not null to get or create the
     * ImportFolder, otherwise use the {@code importFolderTitle}.
     *
     * @return the existing or created ImportFolder
     */
    protected DocumentModel getOrCreateImportFolder() throws Exception {
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
            throws Exception {
        CoreSession session = getCoreSession();
        DocumentModel importFolder;
        if (!session.exists(importFolderRef)) {
            importFolder = session.createDocumentModel(importWritePath,
                    importFolderName, IMPORT_FOLDER_TYPE);
            importFolder.setPropertyValue(DUBLINCORE_TITLE_PROPERTY,
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
        CoreSession session = getCoreSession();
        DocumentModel importSet = session.createDocumentModel(
                importFolder.getPathAsString(), IdUtils.generateId(title),
                IMPORT_SET_TYPE);
        importSet.setPropertyValue(DUBLINCORE_TITLE_PROPERTY, title);

        importSet = setImportSetProperties(importSet);

        importSet = session.createDocument(importSet);
        return importSet;
    }

    protected String generateImportSetName() {
        Calendar calendar = Calendar.getInstance();
        return IMPORT_SET_NAME_FORMAT.format(calendar.getTime());
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

            String contextPath = new Path(metadataFile.getAbsolutePath()).removeLastSegments(
                    1).toString();
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
