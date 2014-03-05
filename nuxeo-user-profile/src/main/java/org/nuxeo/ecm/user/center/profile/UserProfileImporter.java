/*
 * (C) Copyright 2012-2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 */
package org.nuxeo.ecm.user.center.profile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.SimpleTypeImpl;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.BooleanType;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;
import org.nuxeo.ecm.core.schema.types.primitives.DoubleType;
import org.nuxeo.ecm.core.schema.types.primitives.IntegerType;
import org.nuxeo.ecm.core.schema.types.primitives.LongType;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

import au.com.bytecode.opencsv.CSVReader;

/**
 * @since 5.9.3
 */
public class UserProfileImporter {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(UserProfileImporter.class);

    public static final String CONTENT_FILED_TYPE_NAME = "content";

    public static final String USER_PROFILE_IMPORTER_USERNAME_COL = "username";

    protected ImporterConfig config;

    protected String dataFileName;

    protected transient DateFormat dateformat;

    protected Date startDate;

    public UserProfileImporter() {
        startDate = new Date();
    }

    public void doImport(CoreSession session) {
        UserProfileService ups = Framework.getLocalService(UserProfileService.class);

        config = ups.getImporterConfig();
        if (config == null) {
            logError(0, "No importer configuration could be found");
            return;
        }

        dataFileName = config.getDataFileName();
        if (dataFileName == null) {
            logError(0, "No importer dataFileName was supplied");
            return;
        }

        InputStream is = getResourceAsStream(dataFileName);
        if (is == null) {
            logError(0, "Error locating CSV data file: %s:", dataFileName);
            return;
        }

        CSVReader csvReader = null;
        try {
            csvReader = new CSVReader(new InputStreamReader(is));
            doImport(session, csvReader, ups);
        } catch (IOException e) {
            logError(0, "Error while importing CSV data file: %s", dataFileName,
                    e.getMessage());
            log.debug(e, e);
        } finally {
            if (csvReader != null) {
                try {
                    csvReader.close();
                } catch (IOException e) {
                    log.debug(e, e);
                }
            }
        }

    }

    protected InputStream getResourceAsStream(String resource) {
        InputStream is = getClass().getClassLoader().getResourceAsStream(resource);
        if (is == null) {
            is = Framework.getResourceLoader().getResourceAsStream(resource);
            if (is == null) {
                return null;
            }
        }
        return is;
    }

    public void doImport(CoreSession session, CSVReader csvReader,
            UserProfileService userProfileService) throws IOException {
        log.info(String.format("Importing CSV file: %s", dataFileName));

        DocumentType docType = Framework.getLocalService(SchemaManager.class).getDocumentType(
                UserProfileConstants.USER_PROFILE_DOCTYPE);
        if (docType == null) {
            logError(0, "The type '%s' does not exist",
                    UserProfileConstants.USER_PROFILE_DOCTYPE);
            return;
        }

        String[] header = csvReader.readNext();
        if (header == null) {
            // empty file?
            logError(0, "No header line, empty file?");
            return;
        }

        // find the index for the required name and type values
        int nameIndex = -1;
        for (int col = 0; col < header.length; col++) {
            if (UserProfileImporter.USER_PROFILE_IMPORTER_USERNAME_COL.equals(header[col])) {
                nameIndex = col;
                break;
            }
        }
        if (nameIndex == -1) {
            logError(0, "Missing 'username' column");
            return;
        }

        long docsUpdatedCount = 0;
        try {
            int batchSize = config.getBatchSize();
            long lineNumber = 0;
            for (;;) {
                lineNumber++;
                String[] line = csvReader.readNext();
                if (line == null) {
                    break; // no more line
                }

                if (line.length == 0) {
                    // empty line
                    logInfo(lineNumber, "Empty line");
                    continue;
                }

                try {
                    if (importLine(line, lineNumber, nameIndex, docType,
                            session, userProfileService, header)) {
                        docsUpdatedCount++;
                        if (docsUpdatedCount % batchSize == 0) {
                            commitOrRollbackTransaction();
                            startTransaction();
                        }
                    }
                } catch (ClientException e) {
                    // try next line
                    Throwable unwrappedException = unwrapException(e);
                    logError(lineNumber, "Error while importing line: %s",
                            unwrappedException.getMessage());
                    log.debug(unwrappedException, unwrappedException);
                }
            }
            try {
                session.save();
            } catch (ClientException e) {
                Throwable ue = unwrapException(e);
                logError(lineNumber, "Unable to save: %s", ue.getMessage());
                log.debug(ue, ue);
            }
        } finally {
            commitOrRollbackTransaction();
            startTransaction();
        }
        log.info(String.format("Done importing %s entries from CSV file: %s",
                docsUpdatedCount, dataFileName));
    }

    /**
     * Import a line from the CSV file.
     * @param userProfileService
     * @param docType
     * @param session
     *
     * @return {@code true} if a document has been created or updated,
     *         {@code false} otherwise.
     */
    protected boolean importLine(String[] line, final long lineNumber, int nameIndex,
            DocumentType docType, CoreSession session,
            UserProfileService userProfileService, String[] headerValues)
            throws ClientException {
        final String name = line[nameIndex];
        if (StringUtils.isBlank(name)) {
            logError(lineNumber, "Missing 'name' value",
                    "label.csv.importer.missingNameValue");
            return false;
        }

        Map<String, Serializable> values = computePropertiesMap(lineNumber, docType,
                headerValues, line);
        if (values == null) {
            // skip this line
            return false;
        }

        return updateDocument(lineNumber, name, docType,
                session, userProfileService, values);
    }

    protected Map<String, Serializable> computePropertiesMap(long lineNumber,
            DocumentType docType, String[] headerValues, String[] line) {

        Map<String, Serializable> values = new HashMap<String, Serializable>();
        for (int col = 0; col < headerValues.length; col++) {
            String headerValue = headerValues[col];
            String lineValue = line[col];
            lineValue = lineValue.trim();

            String fieldName = headerValue;
            if (!UserProfileImporter.USER_PROFILE_IMPORTER_USERNAME_COL.equals(headerValue)) {
                if (!docType.hasField(fieldName)) {
                    fieldName = fieldName.split(":")[1];
                }
                if (docType.hasField(fieldName)
                        && !StringUtils.isBlank(lineValue)) {
                    Serializable convertedValue = convertValue(docType,
                            fieldName, headerValue, lineValue, lineNumber);
                    if (convertedValue == null) {
                        return null;
                    }
                    values.put(headerValue, convertedValue);
                }
            }
        }
        return values;
    }

    protected Serializable convertValue(DocumentType docType, String fieldName,
            String headerValue, String stringValue, long lineNumber) {
        if (docType.hasField(fieldName)) {
            Field field = docType.getField(fieldName);
            if (field != null) {
                try {
                    Serializable fieldValue = null;
                    Type fieldType = field.getType();
                    if (fieldType.isComplexType()) {
                        if (fieldType.getName().equals(CONTENT_FILED_TYPE_NAME)) {
                            String blobsFolderPath = Framework.getProperty("nuxeo.userprofile.blobs.folder");
                            String path = FilenameUtils.normalize(blobsFolderPath
                                    + "/" + stringValue);
                            File file = new File(path);
                            if (file.exists()) {
                                FileBlob blob = new FileBlob(file);
                                blob.setFilename(file.getName());
                                fieldValue = blob;
                            } else {
                                logError(lineNumber,
                                        "The file '%s' does not exist", stringValue);
                                return null;
                            }
                        }
                        // other types not supported
                    } else {
                        if (fieldType.isListType()) {
                            Type listFieldType = ((ListType) fieldType).getFieldType();
                            if (listFieldType.isSimpleType()) {
                                /*
                                 * Array.
                                 */
                                fieldValue = stringValue.split(config.getListSeparatorRegex());
                            } else {
                                /*
                                 * Complex list.
                                 */
                                fieldValue = (Serializable) Arrays.asList(stringValue.split(config.getListSeparatorRegex()));
                            }
                        } else {
                            /*
                             * Primitive type.
                             */
                            Type type = field.getType();
                            if (type instanceof SimpleTypeImpl) {
                                type = type.getSuperType();
                            }
                            if (type.isSimpleType()) {
                                if (type instanceof StringType) {
                                    fieldValue = stringValue;
                                } else if (type instanceof IntegerType) {
                                    fieldValue = Integer.valueOf(stringValue);
                                } else if (type instanceof LongType) {
                                    fieldValue = Long.valueOf(stringValue);
                                } else if (type instanceof DoubleType) {
                                    fieldValue = Double.valueOf(stringValue);
                                } else if (type instanceof BooleanType) {
                                    fieldValue = Boolean.valueOf(stringValue);
                                } else if (type instanceof DateType) {
                                    fieldValue = getDateFormat().parse(
                                            stringValue);
                                }
                            }
                        }
                    }
                    return fieldValue;
                } catch (ParseException pe) {
                    logError(lineNumber,
                            "Unable to convert field '%s' with value '%s'",
                            headerValue, stringValue);
                    log.debug(pe, pe);
                } catch (NumberFormatException nfe) {
                    logError(lineNumber,
                            "Unable to convert field '%s' with value '%s'",
                            headerValue, stringValue);
                    log.debug(nfe, nfe);
                }
            }
        } else {
            logError(lineNumber, "Field '%s' does not exist on type '%s'",
                    headerValue, docType.getName());
        }
        return null;
    }

    protected DateFormat getDateFormat() {
        // transient field so may become null
        if (dateformat == null) {
            dateformat = new SimpleDateFormat(config.getDateFormat());
        }
        return dateformat;
    }

    protected boolean updateDocument(long lineNumber,
            String name, DocumentType docType, CoreSession session,
            UserProfileService userProfileService,
            Map<String, Serializable> properties) throws ClientException {

        DocumentModel doc = userProfileService.getUserProfileDocument(name, session);
        Calendar createdDate = (Calendar) doc.getPropertyValue("dc:created");
        boolean isCreated = (createdDate.getTime().after(startDate));
        if (!isCreated && !config.isUpdateExisting()) {
            logInfo(lineNumber, "Document already exists for user: %s", name);
            return false;
        }

        for (Map.Entry<String, Serializable> entry : properties.entrySet()) {
            doc.setPropertyValue(entry.getKey(), entry.getValue());
        }

        try {
            session.saveDocument(doc);
        } catch (ClientException e) {
            Throwable unwrappedException = unwrapException(e);
            logError(lineNumber, "Unable to update document for user: %s: %s",
                    name, unwrappedException.getMessage());
            log.debug(unwrappedException, unwrappedException);
            return false;
        }
        return true;
    }

    /**
     * Releases the transaction resources by committing the existing transaction
     * (if any). This is recommended before running a long process.
     */
    protected void commitOrRollbackTransaction() {
        if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            TransactionHelper.commitOrRollbackTransaction();
        }
    }

    /**
     * Starts a new transaction.
     * <p>
     * Usually called after {@code commitOrRollbackTransaction()}, for instance
     * for saving back the results of a long process.
     *
     * @return true if a new transaction was started
     */
    protected boolean startTransaction() {
        return TransactionHelper.startTransaction();
    }


    protected void logError(long lineNumber, String message,
            String... params) {
        String lineMessage = String.format("Line %d", lineNumber);
        String errorMessage = String.format(message, (Object[]) params);
        log.error(String.format("%s: %s", lineMessage, errorMessage));
    }

    protected void logInfo(long lineNumber, String message,
            String... params) {
        String lineMessage = String.format("Line %d", lineNumber);
        String infoMessage = String.format(message, (Object[]) params);
        log.info(String.format("%s: %s", lineMessage, infoMessage));
    }

    public static Throwable unwrapException(Throwable t) {
        Throwable cause = null;

        if (t instanceof ClientException || t instanceof Exception) {
            cause = t.getCause();
        }

        if (cause == null) {
            return t;
        } else {
            return unwrapException(cause);
        }
    }

}
