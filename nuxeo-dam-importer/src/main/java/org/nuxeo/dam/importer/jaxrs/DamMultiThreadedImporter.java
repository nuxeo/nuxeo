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

package org.nuxeo.dam.importer.jaxrs;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.platform.importer.base.GenericMultiThreadedImporter;
import org.nuxeo.ecm.platform.importer.log.ImporterLogger;
import org.nuxeo.ecm.platform.importer.source.SourceNode;

import static org.nuxeo.dam.core.Constants.IMPORT_FOLDER_TYPE;
import static org.nuxeo.dam.core.Constants.IMPORT_SET_TYPE;

/**
 * Default Importer for DAM.
 * <p>All the imported files will be stored in a new
 * {@code ImportSet}, created in the given {@code ImportFolder}. If no
 * ImportFolder is given, use the default 'automatic-import' {@code ImportFolder}.</p>
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class DamMultiThreadedImporter extends GenericMultiThreadedImporter {

    public static final String DEFAULT_IMPORT_FOLDER_NAME = "automatic-import";

    public static final String DUBLINCORE_TITLE_PROPERTY = "dc:title";

    public static DateFormat IMPORT_SET_NAME_FORMAT = new SimpleDateFormat(
            "yyyyMMdd HH:mm");

    protected String importFolderTitle;

    protected String importSetName;

    public DamMultiThreadedImporter(SourceNode sourceNode,
            String importWritePath, String importFolderTitle,
            String importSetName, Integer batchSize, Integer nbThreads,
            ImporterLogger log) throws Exception {
        super(sourceNode, importWritePath, batchSize, nbThreads, log);
        this.importFolderTitle = importFolderTitle;
        this.importSetName = importSetName;
    }

    public DamMultiThreadedImporter(SourceNode sourceNode,
            String importWritePath, String importFolderTitle,
            String importSetName, Integer batchSize, Integer nbThreads,
            String jobName, ImporterLogger log) throws Exception {
        super(sourceNode, importWritePath, batchSize, nbThreads, jobName, log);
        this.importFolderTitle = importFolderTitle;
        this.importSetName = importSetName;
    }

    @Override
    protected DocumentModel createTargetContainer() throws Exception {
        DocumentModel importFolder = getOrCreateImportFolder();
        return createImportSet(importFolder);
    }

    protected DocumentModel getOrCreateImportFolder() throws Exception {
        String importFolderName;
        if (importFolderTitle == null) {
            importFolderName = DEFAULT_IMPORT_FOLDER_NAME;
            importFolderTitle = DEFAULT_IMPORT_FOLDER_NAME;
        } else {
            importFolderName = IdUtils.generateId(importFolderTitle);
        }

        DocumentRef importFolderRef = new PathRef(importWritePath,
                importFolderName);
        CoreSession session = getCoreSession();
        DocumentModel importFolder;
        if (!session.exists(importFolderRef)) {
            importFolder = session.createDocumentModel(importWritePath,
                    importFolderTitle, IMPORT_FOLDER_TYPE);
            importFolder = session.createDocument(importFolder);
            importFolder.setPropertyValue(DUBLINCORE_TITLE_PROPERTY, importFolderTitle);
        } else {
            importFolder = session.getDocument(importFolderRef);
        }
        return importFolder;
    }

    protected DocumentModel createImportSet(DocumentModel importFolder)
            throws Exception {
        String title = importSetName != null ? importSetName
                : generateImportSetName();
        CoreSession session = getCoreSession();
        DocumentModel importSet = session.createDocumentModel(
                importFolder.getPathAsString(), IdUtils.generateId(title),
                IMPORT_SET_TYPE);
        importSet.setPropertyValue(DUBLINCORE_TITLE_PROPERTY, title);
        importSet = session.createDocument(importSet);
        return importSet;
    }

    protected String generateImportSetName() {
        Calendar calendar = Calendar.getInstance();
        return IMPORT_SET_NAME_FORMAT.format(calendar.getTime());
    }

}
