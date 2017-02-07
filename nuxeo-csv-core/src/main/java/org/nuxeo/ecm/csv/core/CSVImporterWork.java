/*
 * (C) Copyright 2012-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 *     Florent Guillaume
 *     Julien Carsique
 */
package org.nuxeo.ecm.csv.core;

import static org.nuxeo.ecm.csv.core.CSVImportLog.Status.ERROR;
import static org.nuxeo.ecm.csv.core.Constants.CSV_NAME_COL;
import static org.nuxeo.ecm.csv.core.Constants.CSV_TYPE_COL;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.CompositeType;
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
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.ecm.core.transientstore.work.TransientStoreWork;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.csv.core.CSVImportLog.Status;
import org.nuxeo.ecm.platform.ec.notification.NotificationEventListener;
import org.nuxeo.ecm.platform.ec.notification.service.NotificationServiceHelper;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.ecm.platform.url.api.DocumentViewCodecManager;
import org.nuxeo.ecm.platform.url.codec.api.DocumentViewCodec;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

;

/**
 * Work task to import form a CSV file. Because the file is read from the local filesystem, this must be executed in a
 * local queue. Since NXP-15252 the CSV reader manages "records", not "lines".
 *
 * @since 5.7
 */
public class CSVImporterWork extends TransientStoreWork {

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

    public static final String LABEL_CSV_IMPORTER_MISSING_NAME_COLUMN = "label.csv.importer.missingNameColumn";

    public static final String LABEL_CSV_IMPORTER_EMPTY_FILE = "label.csv.importer.emptyFile";

    public static final String LABEL_CSV_IMPORTER_ERROR_DURING_IMPORT = "label.csv.importer.errorDuringImport";

    public static final String LABEL_CSV_IMPORTER_EMPTY_LINE = "label.csv.importer.emptyLine";

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(CSVImporterWork.class);

    private static final String TEMPLATE_IMPORT_RESULT = "templates/csvImportResult.ftl";

    public static final String CATEGORY_CSV_IMPORTER = "csvImporter";

    public static final String CONTENT_FILED_TYPE_NAME = "content";

    private static final long COMPUTE_TOTAL_THRESHOLD_KB = 1000;

    /**
     * CSV headers that won't be checked if the field exists on the document type.
     *
     * @since 7.3
     */
    public static List<String> AUTHORIZED_HEADERS = Arrays.asList(NXQL.ECM_LIFECYCLESTATE, NXQL.ECM_UUID);

    protected String parentPath;

    protected String username;

    protected CSVImporterOptions options;

    protected transient DateFormat dateformat;

    protected boolean hasTypeColumn;

    protected Date startDate;

    protected ArrayList<CSVImportLog> importLogs = new ArrayList<>();

    protected boolean computeTotal = false;

    protected long total = -1L;

    protected long docsCreatedCount;

    public CSVImporterWork(String id) {
        super(id);
    }

    public CSVImporterWork(String repositoryName, String parentPath, String username, Blob csvBlob,
            CSVImporterOptions options) {
        super(CSVImportId.create(repositoryName, parentPath, csvBlob));
        getStore().putBlobs(id, Collections.singletonList(csvBlob));
        setDocument(repositoryName, null);
        setOriginatingUsername(username);
        this.parentPath = parentPath;
        this.username = username;
        if (csvBlob.getLength() >= 0 && csvBlob.getLength() / 1024 < COMPUTE_TOTAL_THRESHOLD_KB) {
            this.computeTotal = true;
        }
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
        TransientStore store = getStore();
        setStatus("Importing");
        openUserSession();
        CSVFormat csvFormat = CSVFormat.DEFAULT.withHeader().withEscape(options.getEscapeCharacter()).withCommentMarker(
                options.getCommentMarker());
        try (Reader in = newReader(getBlob()); CSVParser parser = csvFormat.parse(in)) {
            doImport(parser);
        } catch (IOException e) {
            logError(0, "Error while doing the import: %s", LABEL_CSV_IMPORTER_ERROR_DURING_IMPORT, e.getMessage());
            log.debug(e, e);
        }
        store.putParameter(id, "logs", importLogs);
        if (options.sendEmail()) {
            setStatus("Sending email");
            sendMail();
        }
        setStatus(null);
    }

    @Override
    public void cleanUp(boolean ok, Exception e) {
        try {
            super.cleanUp(ok, e);
        } finally {
            getStore().putParameter(id, "status", new CSVImportStatus(CSVImportStatus.State.COMPLETED, total, total));
        }
    }

    static final Serializable EMPTY_LOGS = new ArrayList<CSVImportLog>();

    String launch() {
        WorkManager works = Framework.getLocalService(WorkManager.class);

        TransientStore store = getStore();
        store.putParameter(id, "logs", EMPTY_LOGS);
        store.putParameter(id, "status", new CSVImportStatus(CSVImportStatus.State.SCHEDULED));
        works.schedule(this, WorkManager.Scheduling.IF_NOT_RUNNING_OR_SCHEDULED);
        return id;
    }

    static CSVImportStatus getStatus(String id) {
        TransientStore store = getStore();
        if (!store.exists(id)) {
            return null;
        }
        return (CSVImportStatus) store.getParameter(id, "status");
    }

    @SuppressWarnings("unchecked")
    static List<CSVImportLog> getLastImportLogs(String id) {
        TransientStore store = getStore();
        if (!store.exists(id)) {
            return Collections.emptyList();
        }
        return (ArrayList<CSVImportLog>) store.getParameter(id, "logs");
    }

    /**
     * @throws IOException
     * @since 7.3
     */
    protected BufferedReader newReader(Blob blob) throws IOException {
        return new BufferedReader(new InputStreamReader(new BOMInputStream(blob.getStream())));
    }

    protected void doImport(CSVParser parser) {
        log.info(String.format("Importing CSV file: %s", getBlob().getFilename()));
        Map<String, Integer> header = parser.getHeaderMap();
        if (header == null) {
            logError(0, "No header line, empty file?", LABEL_CSV_IMPORTER_EMPTY_FILE);
            return;
        }
        if (!header.containsKey(CSV_NAME_COL)) {
            logError(0, "Missing 'name' column", LABEL_CSV_IMPORTER_MISSING_NAME_COLUMN);
            return;
        }
        hasTypeColumn = header.containsKey(CSV_TYPE_COL);

        try {
            int batchSize = options.getBatchSize();
            Iterable<CSVRecord> it = parser;
            if (computeTotal) {
                try {
                    List<CSVRecord> l = parser.getRecords();
                    total = l.size();
                    it = l;
                } catch (IOException e) {
                    log.warn("Could not compute total number of document to be imported");
                }
            }
            for (CSVRecord record : it) {
                if (record.size() == 0) {
                    // empty record
                    importLogs.add(new CSVImportLog(record.getRecordNumber(), Status.SKIPPED, "Empty record",
                            LABEL_CSV_IMPORTER_EMPTY_LINE));
                    continue;
                }
                try {
                    if (importRecord(record, header)) {
                        docsCreatedCount++;
                        getStore().putParameter(id, "status", new CSVImportStatus(CSVImportStatus.State.RUNNING,
                                docsCreatedCount, total));
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
        log.info(String.format("Done importing CSV file: %s", getBlob().getFilename()));
    }

    /**
     * Import a line from the CSV file.
     *
     * @return {@code true} if a document has been created or updated, {@code false} otherwise.
     * @since 6.0
     */
    protected boolean importRecord(CSVRecord record, Map<String, Integer> header) {
        String name = record.get(CSV_NAME_COL);
        if (StringUtils.isBlank(name)) {
            log.debug("record.isSet=" + record.isSet(CSV_NAME_COL));
            logError(record.getRecordNumber(), "Missing 'name' value", LABEL_CSV_IMPORTER_MISSING_NAME_VALUE);
            return false;
        }

        Path targetPath = new Path(parentPath).append(name);
        name = targetPath.lastSegment();
        String newParentPath = targetPath.removeLastSegments(1).toString();
        boolean exists = options.getCSVImporterDocumentFactory().exists(session, newParentPath, name, null);

        DocumentRef docRef = null;
        String type = null;
        if (exists) {
            docRef = new PathRef(targetPath.toString());
            type = session.getDocument(docRef).getType();
        } else {
            if (hasTypeColumn) {
                type = record.get(CSV_TYPE_COL);
            }
            if (StringUtils.isBlank(type)) {
                log.debug("record.isSet=" + record.isSet(CSV_TYPE_COL));
                logError(record.getRecordNumber(), "Missing 'type' value", LABEL_CSV_IMPORTER_MISSING_TYPE_VALUE);
                return false;
            }
        }

        DocumentType docType = Framework.getLocalService(SchemaManager.class).getDocumentType(type);
        if (docType == null) {
            logError(record.getRecordNumber(), "The type '%s' does not exist", LABEL_CSV_IMPORTER_NOT_EXISTING_TYPE,
                    type);
            return false;
        }
        Map<String, Serializable> properties = computePropertiesMap(record, docType, header);
        if (properties == null) {
            // skip this line
            return false;
        }

        long lineNumber = record.getRecordNumber();
        if (exists) {
            return updateDocument(lineNumber, docRef, properties);
        } else {
            return createDocument(lineNumber, newParentPath, name, type, properties);
        }
    }

    /**
     * @since 6.0
     */
    protected Map<String, Serializable> computePropertiesMap(CSVRecord record, CompositeType compositeType,
            Map<String, Integer> header) {
        Map<String, Serializable> values = new HashMap<>();
        for (String headerValue : header.keySet()) {
            String lineValue = record.get(headerValue);
            lineValue = lineValue.trim();
            String fieldName = headerValue;
            if (!CSV_NAME_COL.equals(headerValue) && !CSV_TYPE_COL.equals(headerValue)) {
                if (AUTHORIZED_HEADERS.contains(headerValue) && !StringUtils.isBlank(lineValue)) {
                    values.put(headerValue, lineValue);
                } else {
                    if (!compositeType.hasField(fieldName)) {
                        fieldName = fieldName.split(":")[1];
                    }
                    if (compositeType.hasField(fieldName) && !StringUtils.isBlank(lineValue)) {
                        Serializable convertedValue = convertValue(compositeType, fieldName, headerValue, lineValue,
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

    protected Serializable convertValue(CompositeType compositeType, String fieldName, String headerValue,
            String stringValue, long lineNumber) {
        if (compositeType.hasField(fieldName)) {
            Field field = compositeType.getField(fieldName);
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
                    headerValue, compositeType.getName());
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

    protected void logError(long lineNumber, String message, String localizedMessage, String... params) {
        importLogs.add(new CSVImportLog(lineNumber, ERROR, String.format(message, (Object[]) params), localizedMessage,
                params));
        String lineMessage = String.format("Line %d", lineNumber);
        String errorMessage = String.format(message, (Object[]) params);
        log.error(String.format("%s: %s", lineMessage, errorMessage));
        getStore().putParameter(id, "status",
                new CSVImportStatus(CSVImportStatus.State.ERROR, docsCreatedCount, docsCreatedCount));
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
        ctx.put("csvFilename", getBlob().getFilename());
        ctx.put("startDate", DateFormat.getInstance().format(startDate));
        ctx.put("username", username);

        DocumentModel importFolder = session.getDocument(new PathRef(parentPath));
        String importFolderUrl = getDocumentUrl(importFolder);
        ctx.put("importFolderTitle", importFolder.getTitle());
        ctx.put("importFolderUrl", importFolderUrl);
        ctx.put("userUrl", getUserUrl());

        StringList to = buildRecipientsList(email);
        Expression from = Scripting.newExpression("Env[\"mail.from\"]");
        String subject = "CSV Import result of " + getBlob().getFilename();
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
            log.error(String.format("Unable to notify user '%s' for import result of '%s': %s", username,
                    getBlob().getFilename(),
                    e.getMessage()));
            log.debug(e, e);
            throw ExceptionUtils.runtimeException(e);
        }
    }

    /**
     * @since 9.1
     */
    private Blob getBlob() {
        return getStore().getBlobs(id).get(0);
    }

    protected String getDocumentUrl(DocumentModel doc) {
        return MailTemplateHelper.getDocumentUrl(doc, null);
    }

    protected String getUserUrl() {
        DocumentViewCodecManager codecService = Framework.getService(DocumentViewCodecManager.class);
        DocumentViewCodec codec = codecService.getCodec(NotificationEventListener.NOTIFICATION_DOCUMENT_ID_CODEC_NAME);
        boolean isNotificationCodec = codec != null;
        boolean isJSFUI = isNotificationCodec
                && NotificationEventListener.JSF_NOTIFICATION_DOCUMENT_ID_CODEC_PREFIX.equals(codec.getPrefix());
        StringBuilder userUrl = new StringBuilder();
        if (isNotificationCodec) {

            userUrl.append(NotificationServiceHelper.getNotificationService().getServerUrlPrefix());
            if (!isJSFUI) {
                userUrl.append("ui/");
                userUrl.append("#!/");
            }
            userUrl.append("user/").append(username);
        }
        return userUrl.toString();
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
