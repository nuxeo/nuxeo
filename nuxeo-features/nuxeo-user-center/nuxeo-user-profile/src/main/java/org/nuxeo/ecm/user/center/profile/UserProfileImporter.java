/*
 * (C) Copyright 2012-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.user.center.profile;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.annotation.Experimental;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
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

/**
 *
 * @since 7.2
 */
@Experimental(comment="https://jira.nuxeo.com/browse/NXP-12200")
public class UserProfileImporter {

    private static final Log log = LogFactory.getLog(UserProfileImporter.class);

    public static final String CONTENT_FILED_TYPE_NAME = "content";

    public static final String USER_PROFILE_IMPORTER_USERNAME_COL = "username";

    protected Character escapeCharacter = '\\';

    protected ImporterConfig config;

    protected String dataFileName;

    protected transient DateFormat dateformat;

    protected final Date startDate;

    protected long totalRecords = 0;

    protected long currentRecord = 0;

    public static final String BLOB_FOLDER_PROPERTY = "nuxeo.csv.blobs.folder";

    public UserProfileImporter() {
        startDate = new Date();
    }

    public void doImport(CoreSession session) {
        UserProfileService ups = Framework.getService(UserProfileService.class);

        config = ups.getImporterConfig();
        if (config == null) {
            log.error("No importer configuration could be found");
            return;
        }

        dataFileName = config.getDataFileName();
        if (dataFileName == null) {
            log.error("No importer dataFileName was supplied");
            return;
        }

        InputStream is = getResourceAsStream(dataFileName);
        if (is == null) {
            log.error("Error locating CSV data file: " + dataFileName);
            return;
        }

        Reader in = new BufferedReader(new InputStreamReader(is));
        CSVParser parser = null;

        try {
            parser = CSVFormat.DEFAULT.withEscape(escapeCharacter).withHeader().parse(in);
            doImport(session, parser, ups);
        } catch (IOException e) {
            log.error("Unable to read CSV file", e);
        } finally {
            if (parser != null) {
                try {
                    parser.close();
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

    public void doImport(CoreSession session, CSVParser parser, UserProfileService userProfileService)
            throws IOException {
        log.info(String.format("Importing CSV file: %s", dataFileName));

        DocumentType docType = Framework.getService(SchemaManager.class).getDocumentType(
                UserProfileConstants.USER_PROFILE_DOCTYPE);
        if (docType == null) {
            log.error("The type " + UserProfileConstants.USER_PROFILE_DOCTYPE + " does not exist");
            return;
        }

        Map<String, Integer> header = parser.getHeaderMap();

        if (header == null) {
            // empty file?
            log.error("No header line, empty file?");
            return;
        }

        // find the index for the required name and type values
        Integer nameIndex = header.get(UserProfileImporter.USER_PROFILE_IMPORTER_USERNAME_COL);
        if (nameIndex == null) {
            log.error("Missing 'username' column");
            return;
        }

        long docsUpdatedCount = 0;
        totalRecords = parser.getRecordNumber();
        try {
            int batchSize = config.getBatchSize();
            long lineNumber = 0;

            for (CSVRecord record : parser.getRecords()) {
                lineNumber++;
                currentRecord = lineNumber;

                try {
                    if (importLine(record, lineNumber, nameIndex, docType, session, userProfileService, header)) {
                        docsUpdatedCount++;
                        if (docsUpdatedCount % batchSize == 0) {
                            commitOrRollbackTransaction();
                            startTransaction();
                        }
                    }
                } catch (NuxeoException e) {
                    // try next line
                    Throwable unwrappedException = unwrapException(e);
                    logImportError(lineNumber, "Error while importing line: %s", unwrappedException.getMessage());
                    log.debug(unwrappedException, unwrappedException);
                }
            }

            session.save();
        } finally {
            commitOrRollbackTransaction();
            startTransaction();
        }
        log.info(String.format("Done importing %s entries from CSV file: %s", docsUpdatedCount, dataFileName));
    }

    /**
     * Import a line from the CSV file.
     *
     * @param userProfileService
     * @param docType
     * @param session
     * @return {@code true} if a document has been created or updated, {@code false} otherwise.
     */
    protected boolean importLine(CSVRecord record, final long lineNumber, Integer nameIndex, DocumentType docType,
            CoreSession session, UserProfileService userProfileService, Map<String, Integer> headerValues)
            {
        final String name = record.get(nameIndex);
        if (StringUtils.isBlank(name)) {
            logImportError(lineNumber, "Missing 'name' value", "label.csv.importer.missingNameValue");
            return false;
        }

        Map<String, Serializable> values = computePropertiesMap(lineNumber, docType, headerValues, record);
        if (values == null) {
            // skip this line
            return false;
        }

        return updateDocument(lineNumber, name, docType, session, userProfileService, values);
    }

    protected Map<String, Serializable> computePropertiesMap(long lineNumber, DocumentType docType,
            Map<String, Integer> headerValues, CSVRecord record) {

        Map<String, Serializable> values = new HashMap<String, Serializable>();
        for (String headerValue : headerValues.keySet()) {
            String lineValue = record.get(headerValue);
            lineValue = lineValue.trim();
            String fieldName = headerValue;
            if (!UserProfileImporter.USER_PROFILE_IMPORTER_USERNAME_COL.equals(headerValue)) {
                if (!docType.hasField(fieldName)) {
                    fieldName = fieldName.split(":")[1];
                }
                if (docType.hasField(fieldName) && !StringUtils.isBlank(lineValue)) {
                    Serializable convertedValue = convertValue(docType, fieldName, headerValue, lineValue, lineNumber);
                    if (convertedValue == null) {
                        return null;
                    }
                    values.put(headerValue, convertedValue);
                }
            }
        }
        return values;
    }

    protected Serializable convertValue(DocumentType docType, String fieldName, String headerValue, String stringValue,
            long lineNumber) {
        if (docType.hasField(fieldName)) {
            Field field = docType.getField(fieldName);
            if (field != null) {
                try {
                    Serializable fieldValue = null;
                    Type fieldType = field.getType();
                    if (fieldType.isComplexType()) {
                        if (fieldType.getName().equals(CONTENT_FILED_TYPE_NAME)) {
                            String blobsFolderPath = Framework.getProperty(BLOB_FOLDER_PROPERTY);
                            String path = FilenameUtils.normalize(blobsFolderPath + "/" + stringValue);
                            File file = new File(path);
                            if (file.exists()) {
                                FileBlob blob = new FileBlob(file);
                                blob.setFilename(file.getName());
                                fieldValue = blob;
                            } else {
                                logImportError(lineNumber, "The file '%s' does not exist", stringValue);
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
                                    fieldValue = getDateFormat().parse(stringValue);
                                }
                            }
                        }
                    }
                    return fieldValue;
                } catch (ParseException pe) {
                    logImportError(lineNumber, "Unable to convert field '%s' with value '%s'", headerValue, stringValue);
                    log.debug(pe, pe);
                } catch (NumberFormatException nfe) {
                    logImportError(lineNumber, "Unable to convert field '%s' with value '%s'", headerValue, stringValue);
                    log.debug(nfe, nfe);
                }
            }
        } else {
            logImportError(lineNumber, "Field '%s' does not exist on type '%s'", headerValue, docType.getName());
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

    protected boolean updateDocument(long lineNumber, String name, DocumentType docType, CoreSession session,
            UserProfileService userProfileService, Map<String, Serializable> properties) {

        DocumentModel doc = userProfileService.getUserProfileDocument(name, session);
        Calendar createdDate = (Calendar) doc.getPropertyValue("dc:created");
        boolean isCreated = (createdDate.getTime().after(startDate));
        if (!isCreated && !config.isUpdateExisting()) {
            logImportInfo(lineNumber, "Document already exists for user: %s", name);
            return false;
        }

        for (Map.Entry<String, Serializable> entry : properties.entrySet()) {
            doc.setPropertyValue(entry.getKey(), entry.getValue());
        }

        try {
            session.saveDocument(doc);
        } catch (NuxeoException e) {
            Throwable unwrappedException = unwrapException(e);
            logImportError(lineNumber, "Unable to update document for user: %s: %s", name,
                    unwrappedException.getMessage());
            log.debug(unwrappedException, unwrappedException);
            return false;
        }
        return true;
    }

    /**
     * Releases the transaction resources by committing the existing transaction (if any). This is recommended before
     * running a long process.
     */
    protected void commitOrRollbackTransaction() {
        if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            TransactionHelper.commitOrRollbackTransaction();
        }
    }

    /**
     * Starts a new transaction.
     * <p>
     * Usually called after {@code commitOrRollbackTransaction()}, for instance for saving back the results of a long
     * process.
     *
     * @return true if a new transaction was started
     */
    protected boolean startTransaction() {
        return TransactionHelper.startTransaction();
    }

    protected void logImportError(long lineNumber, String message, String... params) {
        String lineMessage = String.format("Line %d", lineNumber);
        String errorMessage = String.format(message, (Object[]) params);
        log.error(String.format("%s: %s", lineMessage, errorMessage));
    }

    protected void logImportInfo(long lineNumber, String message, String... params) {
        String lineMessage = String.format("Line %d", lineNumber);
        String infoMessage = String.format(message, (Object[]) params);
        log.info(String.format("%s: %s", lineMessage, infoMessage));
    }

    public static Throwable unwrapException(Throwable t) {
        Throwable cause = null;

        if (t != null) {
            cause = t.getCause();
        }

        if (cause == null) {
            return t;
        } else {
            return unwrapException(cause);
        }
    }

    public long getTotalRecords() {
        return totalRecords;
    }

    public long getCurrentRecord() {
        return currentRecord;
    }

}
