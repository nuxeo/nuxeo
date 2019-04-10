/*
 * (C) Copyright 2012-2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thomas Roger
 *     Florent Guillaume
 *     Julien Carsique
 */
package org.nuxeo.ecm.csv;

import static org.nuxeo.ecm.csv.CSVImportLog.Status.ERROR;
import static org.nuxeo.ecm.csv.Constants.CSV_NAME_COL;
import static org.nuxeo.ecm.csv.Constants.CSV_TYPE_COL;
import static org.nuxeo.ecm.csv.Constants.CSV_UID_COL;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.ExceptionUtils;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.operations.notification.MailTemplateHelper;
import org.nuxeo.ecm.automation.core.operations.notification.SendMail;
import org.nuxeo.ecm.automation.core.scripting.Expression;
import org.nuxeo.ecm.automation.core.scripting.Scripting;
import org.nuxeo.ecm.automation.core.util.ComplexTypeJSONDecoder;
import org.nuxeo.ecm.automation.core.util.StringList;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.ComplexType;
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
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.ecm.csv.CSVImportLog.Status;
import org.nuxeo.ecm.platform.ec.notification.service.NotificationService;
import org.nuxeo.ecm.platform.ec.notification.service.NotificationServiceHelper;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.ecm.platform.ui.web.rest.api.URLPolicyService;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Work task to import form a CSV file. Because the file is read from the local filesystem, this must be executed in a
 * local queue. Since NXP-15252 the CSV reader manages "records", not "lines".
 *
 * @since 5.7
 */
public class CSVImporterWork extends AbstractWork {

    public static final String NUXEO_CSV_MAIL_TO = "nuxeo.csv.mail.to";

    public static final String LABEL_CSV_IMPORTER_NOT_EXISTING_FIELD = "label.csv.importer.notExistingField";

    public static final String LABEL_CSV_IMPORTER_CANNOT_CONVERT_FIELD_VALUE = "label.csv.importer.cannotConvertFieldValue";

    public static final String LABEL_CSV_IMPORTER_NOT_EXISTING_FILE = "label.csv.importer.notExistingFile";

    public static final String NUXEO_CSV_BLOBS_FOLDER = "nuxeo.csv.blobs.folder";

    public static final String LABEL_CSV_IMPORTER_DOCUMENT_ALREADY_EXISTS = "label.csv.importer.documentAlreadyExists";

    public static final String LABEL_CSV_IMPORTER_UNABLE_TO_UPDATE = "label.csv.importer.unableToUpdate";

    public static final String LABEL_CSV_IMPORTER_DOCUMENT_UPDATED = "label.csv.importer.documentUpdated";

    public static final String LABEL_CSV_IMPORTER_UNABLE_TO_CREATE = "label.csv.importer.unableToCreate";

    public static final String LABEL_CSV_IMPORTER_PARENT_DOES_NOT_EXIST = "label.csv.importer.parentDoesNotExist";

    public static final String LABEL_CSV_IMPORTER_DOCUMENT_CREATED = "label.csv.importer.documentCreated";

    public static final String LABEL_CSV_IMPORTER_NOT_ALLOWED_SUB_TYPE = "label.csv.importer.notAllowedSubType";

    public static final String LABEL_CSV_IMPORTER_UNABLE_TO_SAVE = "label.csv.importer.unableToSave";

    public static final String LABEL_CSV_IMPORTER_ERROR_IMPORTING_LINE = "label.csv.importer.errorImportingLine";

    public static final String LABEL_CSV_IMPORTER_NOT_EXISTING_TYPE = "label.csv.importer.notExistingType";

    public static final String LABEL_CSV_IMPORTER_MISSING_TYPE_VALUE = "label.csv.importer.missingTypeValue";

    public static final String LABEL_CSV_IMPORTER_MISSING_NAME_VALUE = "label.csv.importer.missingNameValue";

    public static final String LABEL_CSV_IMPORTER_MISSING_NAME_OR_TYPE_COLUMN = "label.csv.importer.missingNameOrTypeColumn";

    public static final String LABEL_CSV_IMPORTER_EMPTY_FILE = "label.csv.importer.emptyFile";

    public static final String LABEL_CSV_IMPORTER_ERROR_DURING_IMPORT = "label.csv.importer.errorDuringImport";

    public static final String LABEL_CSV_IMPORTER_EMPTY_LINE = "label.csv.importer.emptyLine";

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(CSVImporterWork.class);

    private static final String TEMPLATE_IMPORT_RESULT = "templates/csvImportResult.ftl";

    public static final String CATEGORY_CSV_IMPORTER = "csvImporter";

    public static final String CONTENT_FILED_TYPE_NAME = "content";

    /**
     * CSV headers that won't be checked if the field exists on the document type.
     *
     * @since 7.3
     */
    public static List<String> AUTHORIZED_HEADERS = Arrays.asList(NXQL.ECM_LIFECYCLESTATE);

    protected String parentPath;

    protected String username;

    protected File csvFile;

    protected String csvFileName;

    protected CSVImporterOptions options;

    protected transient DateFormat dateformat;

    protected Date startDate;

    protected List<CSVImportLog> importLogs = new ArrayList<>();

    public CSVImporterWork(String id) {
        super(id);
    }

    public CSVImporterWork(String repositoryName, String parentPath, String username, File csvFile, String csvFileName,
            CSVImporterOptions options) {
        super(CSVImportId.create(repositoryName, parentPath, csvFile));
        setDocument(repositoryName, null);
        setOriginatingUsername(username);
        this.parentPath = parentPath;
        this.username = username;
        this.csvFile = csvFile;
        this.csvFileName = csvFileName;
        this.options = options;
        startDate = new Date();
    }

    @Override
    public String getCategory() {
        return CATEGORY_CSV_IMPORTER;
    }

    @Override
    public String getTitle() {
        return String.format("CSV import in '%s'", parentPath);
    }

    public List<CSVImportLog> getImportLogs() {
        return new ArrayList<>(importLogs);
    }

    @Override
    public void work() {
        setStatus("Importing");
        openUserSession();
        try (Reader in = newReader(csvFile);
                CSVParser parser = CSVFormat.DEFAULT.withEscape(options.getEscapeCharacter()).withHeader().parse(in)) {
            doImport(parser);
        } catch (IOException e) {
            logError(0, "Error while doing the import: %s", LABEL_CSV_IMPORTER_ERROR_DURING_IMPORT, e.getMessage());
            log.debug(e, e);
        }
        if (options.sendEmail()) {
            setStatus("Sending email");
            sendMail();
        }
        setStatus(null);
    }

    /**
     * @since 7.3
     */
    protected BufferedReader newReader(File file) throws FileNotFoundException {
        return new BufferedReader(new InputStreamReader(new BOMInputStream(new FileInputStream(file))));
    }

    protected void doImport(CSVParser parser) {
        log.info(String.format("Importing CSV file: %s", csvFileName));
        Map<String, Integer> header = parser.getHeaderMap();
        if (header == null) {
            logError(0, "No header line, empty file?", LABEL_CSV_IMPORTER_EMPTY_FILE);
            return;
        }
        if ( (!header.containsKey(CSV_NAME_COL) || !header.containsKey(CSV_TYPE_COL)) && !header.containsKey(CSV_UID_COL) ) {
            logError(0, "Missing 'name' or 'type' column", LABEL_CSV_IMPORTER_MISSING_NAME_OR_TYPE_COLUMN);
            return;
        }

        try {
            int batchSize = options.getBatchSize();
            long docsCreatedCount = 0;
            for (CSVRecord record : parser) {
                if (record.size() == 0) {
                    // empty record
                    importLogs.add(new CSVImportLog(record.getRecordNumber(), Status.SKIPPED, "Empty record",
                            LABEL_CSV_IMPORTER_EMPTY_LINE));
                    continue;
                }
                try {
                    if (importRecord(record, header)) {
                        docsCreatedCount++;
                        if (docsCreatedCount % batchSize == 0) {
                            commitOrRollbackTransaction();
                            startTransaction();
                        }
                    }
                } catch (NuxeoException e) {
                    // try next line
                    Throwable unwrappedException = unwrapException(e);
                    logError(parser.getRecordNumber(), "Error while importing line: %s",
                            LABEL_CSV_IMPORTER_ERROR_IMPORTING_LINE, unwrappedException.getMessage());
                    log.debug(unwrappedException, unwrappedException);
                }
            }

            try {
                session.save();
            } catch (NuxeoException e) {
                Throwable ue = unwrapException(e);
                logError(parser.getRecordNumber(), "Unable to save: %s", LABEL_CSV_IMPORTER_UNABLE_TO_SAVE,
                        ue.getMessage());
                log.debug(ue, ue);
            }
        } finally {
            commitOrRollbackTransaction();
            startTransaction();
        }
        log.info(String.format("Done importing CSV file: %s", csvFileName));
    }

    /**
     * Import a line from the CSV file.
     *
     * @return {@code true} if a document has been created or updated, {@code false} otherwise.
     * @since 6.0
     */
    protected boolean importRecord(CSVRecord record, Map<String, Integer> header) {
        if (record.isSet(CSV_UID_COL) && !StringUtils.isBlank(record.get(CSV_UID_COL))) {

            DocumentType docType = Framework.getLocalService(SchemaManager.class).getDocumentType(record.get(CSV_TYPE_COL));
            if (docType == null) {
                logError(record.getRecordNumber(), "The type '%s' does not exist", LABEL_CSV_IMPORTER_NOT_EXISTING_TYPE, record.get(CSV_TYPE_COL));
                return false;
            }
            Map<String, Serializable> values = computePropertiesMap(record, docType, header);
            if (values == null) {
                // skip this line
                return false;
            }
            return updateDocumentByUID(record.getRecordNumber(), record.get(CSV_UID_COL), docType, values);
        }

        final String name = record.get(CSV_NAME_COL);
        final String type = record.get(CSV_TYPE_COL);

        if (StringUtils.isBlank(name) ) {
            //log.debug("record.isSet=" + record.isSet(CSV_NAME_COL));
            logError(record.getRecordNumber(), "Missing 'name' or 'uid' value", LABEL_CSV_IMPORTER_MISSING_NAME_VALUE);
            return false;
        }
        if (StringUtils.isBlank(type)) {
            log.debug("record.isSet=" + record.isSet(CSV_TYPE_COL));
            logError(record.getRecordNumber(), "Missing 'type' value", LABEL_CSV_IMPORTER_MISSING_TYPE_VALUE);
            return false;
        }
        DocumentType docType = Framework.getLocalService(SchemaManager.class).getDocumentType(type);
        if (docType == null) {
            logError(record.getRecordNumber(), "The type '%s' does not exist", LABEL_CSV_IMPORTER_NOT_EXISTING_TYPE,
                    type);
            return false;
        }
        Map<String, Serializable> values = computePropertiesMap(record, docType, header);
        if (values == null) {
            // skip this line
            return false;
        }

        return createOrUpdateDocument(record.getRecordNumber(), name, type, values);
    }


    /**
     * @since 6.0
     */
    protected Map<String, Serializable> computePropertiesMap(CSVRecord record, DocumentType docType,
            Map<String, Integer> header) {
        Map<String, Serializable> values = new HashMap<>();
        for (String headerValue : header.keySet()) {
            String lineValue = record.get(headerValue);
            lineValue = lineValue.trim();
            String fieldName = headerValue;
            if (!CSV_NAME_COL.equals(headerValue) && !CSV_TYPE_COL.equals(headerValue) && !CSV_UID_COL.equals(headerValue)) {
                if (AUTHORIZED_HEADERS.contains(headerValue) && !StringUtils.isBlank(lineValue)) {
                    values.put(headerValue, lineValue);
                } else {
                    if (!docType.hasField(fieldName)) {
                        fieldName = fieldName.split(":")[1];
                    }
                    if (docType.hasField(fieldName) && !StringUtils.isBlank(lineValue)) {
                        Serializable convertedValue = convertValue(docType, fieldName, headerValue, lineValue,
                                record.getRecordNumber());
                        if (convertedValue == null) {
                            return null;
                        }
                        values.put(headerValue, convertedValue);
                    }
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
                            String blobsFolderPath = Framework.getProperty(NUXEO_CSV_BLOBS_FOLDER);
                            String path = FilenameUtils.normalize(blobsFolderPath + "/" + stringValue);
                            File file = new File(path);
                            if (file.exists()) {
                                fieldValue = (Serializable) Blobs.createBlob(file);
                            } else {
                                logError(lineNumber, "The file '%s' does not exist",
                                        LABEL_CSV_IMPORTER_NOT_EXISTING_FILE, stringValue);
                                return null;
                            }
                        } else {
                            fieldValue = (Serializable) ComplexTypeJSONDecoder.decode((ComplexType) fieldType,
                                    stringValue);
                        }
                    } else {
                        if (fieldType.isListType()) {
                            Type listFieldType = ((ListType) fieldType).getFieldType();
                            if (listFieldType.isSimpleType()) {
                                /*
                                 * Array.
                                 */
                                fieldValue = stringValue.split(options.getListSeparatorRegex());
                            } else {
                                /*
                                 * Complex list.
                                 */
                                fieldValue = (Serializable) ComplexTypeJSONDecoder.decodeList((ListType) fieldType,
                                        stringValue);
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
                } catch (ParseException | NumberFormatException | IOException e) {
                    logError(lineNumber, "Unable to convert field '%s' with value '%s'",
                            LABEL_CSV_IMPORTER_CANNOT_CONVERT_FIELD_VALUE, headerValue, stringValue);
                    log.debug(e, e);
                }
            }
        } else {
            logError(lineNumber, "Field '%s' does not exist on type '%s'", LABEL_CSV_IMPORTER_NOT_EXISTING_FIELD,
                    headerValue, docType.getName());
        }
        return null;
    }

    protected DateFormat getDateFormat() {
        // transient field so may become null
        if (dateformat == null) {
            dateformat = new SimpleDateFormat(options.getDateFormat());
        }
        return dateformat;
    }

    protected boolean createOrUpdateDocument(long lineNumber, String name, String type,
            Map<String, Serializable> properties) {
        Path targetPath = new Path(parentPath).append(name);
        name = targetPath.lastSegment();
        String newParentPath = targetPath.removeLastSegments(1).toString();
        DocumentRef docRef = new PathRef(targetPath.toString());
        if (options.getCSVImporterDocumentFactory().exists(session, newParentPath, name, type, properties)) {
            return updateDocument(lineNumber, docRef, properties);
        } else {
            return createDocument(lineNumber, newParentPath, name, type, properties);
        }
    }

    protected boolean createDocument(long lineNumber, String newParentPath, String name, String type,
            Map<String, Serializable> properties) {
        try {
            DocumentRef parentRef = new PathRef(newParentPath);
            if (session.exists(parentRef)) {
                DocumentModel parent = session.getDocument(parentRef);

                TypeManager typeManager = Framework.getLocalService(TypeManager.class);
                if (options.checkAllowedSubTypes() && !typeManager.isAllowedSubType(type, parent.getType())) {
                    logError(lineNumber, "'%s' type is not allowed in '%s'", LABEL_CSV_IMPORTER_NOT_ALLOWED_SUB_TYPE,
                            type, parent.getType());
                } else {
                    options.getCSVImporterDocumentFactory().createDocument(session, newParentPath, name, type,
                            properties);
                    importLogs.add(new CSVImportLog(lineNumber, Status.SUCCESS, "Document created",
                            LABEL_CSV_IMPORTER_DOCUMENT_CREATED));
                    return true;
                }
            } else {
                logError(lineNumber, "Parent document '%s' does not exist", LABEL_CSV_IMPORTER_PARENT_DOES_NOT_EXIST,
                        newParentPath);
            }
        } catch (RuntimeException e) {
            Throwable unwrappedException = unwrapException(e);
            logError(lineNumber, "Unable to create document: %s", LABEL_CSV_IMPORTER_UNABLE_TO_CREATE,
                    unwrappedException.getMessage());
            log.debug(unwrappedException, unwrappedException);
        }
        return false;
    }

    protected boolean updateDocument(long lineNumber, DocumentRef docRef, Map<String, Serializable> properties) {
        if (options.updateExisting()) {
            try {
                options.getCSVImporterDocumentFactory().updateDocument(session, docRef, properties);
                importLogs.add(new CSVImportLog(lineNumber, Status.SUCCESS, "Document updated",
                        LABEL_CSV_IMPORTER_DOCUMENT_UPDATED));
                return true;
            } catch (RuntimeException e) {
                Throwable unwrappedException = unwrapException(e);
                logError(lineNumber, "Unable to update document: %s", LABEL_CSV_IMPORTER_UNABLE_TO_UPDATE,
                        unwrappedException.getMessage());
                log.debug(unwrappedException, unwrappedException);
            }
        } else {
            importLogs.add(new CSVImportLog(lineNumber, Status.SKIPPED, "Document already exists",
                    LABEL_CSV_IMPORTER_DOCUMENT_ALREADY_EXISTS));
        }
        return false;
    }

    protected boolean updateDocumentByUID(long recordNumber, String uid, DocumentType docType, Map<String, Serializable> values) {
        DocumentRef docRef = new IdRef(uid);
        if (session.exists(docRef)) {
            return updateDocument(recordNumber, docRef, values);
        } else {
            logError(recordNumber, "Document with UID '%s' does not exist", LABEL_CSV_IMPORTER_NOT_EXISTING_FILE, uid);
            return false;
        }
    }

    protected void logError(long lineNumber, String message, String localizedMessage, String... params) {
        importLogs.add(new CSVImportLog(lineNumber, ERROR, String.format(message, (Object[]) params), localizedMessage,
                params));
        String lineMessage = String.format("Line %d", lineNumber);
        String errorMessage = String.format(message, (Object[]) params);
        log.error(String.format("%s: %s", lineMessage, errorMessage));
    }

    protected void sendMail() {
        UserManager userManager = Framework.getLocalService(UserManager.class);
        NuxeoPrincipal principal = userManager.getPrincipal(username);
        String email = principal.getEmail();
        if (email == null) {
            log.info(String.format("Not sending import result email to '%s', no email configured", username));
            return;
        }

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(session.getRootDocument());

        CSVImporter csvImporter = Framework.getLocalService(CSVImporter.class);
        List<CSVImportLog> importerLogs = csvImporter.getImportLogs(getId());
        CSVImportResult importResult = CSVImportResult.fromImportLogs(importerLogs);
        List<CSVImportLog> skippedAndErrorImportLogs = csvImporter.getImportLogs(getId(), Status.SKIPPED, Status.ERROR);
        ctx.put("importResult", importResult);
        ctx.put("skippedAndErrorImportLogs", skippedAndErrorImportLogs);
        ctx.put("csvFilename", csvFileName);
        ctx.put("startDate", DateFormat.getInstance().format(startDate));
        ctx.put("username", username);

        DocumentModel importFolder = session.getDocument(new PathRef(parentPath));
        String importFolderUrl = getDocumentUrl(importFolder);
        ctx.put("importFolderTitle", importFolder.getTitle());
        ctx.put("importFolderUrl", importFolderUrl);
        ctx.put("userUrl", getUserUrl());

        StringList to = buildRecipientsList(email);
        Expression from = Scripting.newExpression("Env[\"mail.from\"]");
        String subject = "CSV Import result of " + csvFileName;
        String message = loadTemplate(TEMPLATE_IMPORT_RESULT);

        try {
            OperationChain chain = new OperationChain("SendMail");
            chain.add(SendMail.ID)
                 .set("from", from)
                 .set("to", to)
                 .set("HTML", true)
                 .set("subject", subject)
                 .set("message", message);
            Framework.getLocalService(AutomationService.class).run(ctx, chain);
        } catch (Exception e) {
            ExceptionUtils.checkInterrupt(e);
            log.error(String.format("Unable to notify user '%s' for import result of '%s': %s", username, csvFileName,
                    e.getMessage()));
            log.debug(e, e);
            throw ExceptionUtils.runtimeException(e);
        }
    }

    protected String getDocumentUrl(DocumentModel doc) {
        return MailTemplateHelper.getDocumentUrl(doc, null);
    }

    protected String getUserUrl() {
        NotificationService notificationService = NotificationServiceHelper.getNotificationService();
        Map<String, String> params = new HashMap<>();
        params.put("username", username);
        DocumentView docView = new DocumentViewImpl(null, null, params);
        URLPolicyService urlPolicyService = Framework.getLocalService(URLPolicyService.class);
        return urlPolicyService.getUrlFromDocumentView("user", docView, notificationService.getServerUrlPrefix());
    }

    protected StringList buildRecipientsList(String userEmail) {
        String csvMailTo = Framework.getProperty(NUXEO_CSV_MAIL_TO);
        if (StringUtils.isBlank(csvMailTo)) {
            return new StringList(new String[] { userEmail });
        } else {
            return new StringList(new String[] { userEmail, csvMailTo });
        }
    }

    private static String loadTemplate(String key) {
        InputStream io = CSVImporterWork.class.getClassLoader().getResourceAsStream(key);
        if (io != null) {
            try {
                return IOUtils.toString(io, Charsets.UTF_8);
            } catch (IOException e) {
                // cannot happen
                throw new NuxeoException(e);
            } finally {
                try {
                    io.close();
                } catch (IOException e) {
                    // nothing to do
                }
            }
        }
        return null;
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

}
