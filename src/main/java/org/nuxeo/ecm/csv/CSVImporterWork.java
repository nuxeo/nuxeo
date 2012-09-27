package org.nuxeo.ecm.csv;

import static org.nuxeo.ecm.csv.CSVImportLog.Status.ERROR;
import static org.nuxeo.ecm.csv.Constants.CSV_NAME_COL;
import static org.nuxeo.ecm.csv.Constants.CSV_TYPE_COL;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.operations.notification.SendMail;
import org.nuxeo.ecm.automation.core.scripting.Expression;
import org.nuxeo.ecm.automation.core.scripting.Scripting;
import org.nuxeo.ecm.automation.core.util.StringList;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.impl.SimpleDocumentModel;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.BooleanType;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;
import org.nuxeo.ecm.core.schema.types.primitives.DoubleType;
import org.nuxeo.ecm.core.schema.types.primitives.IntegerType;
import org.nuxeo.ecm.core.schema.types.primitives.LongType;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

import au.com.bytecode.opencsv.CSVReader;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.7
 */
public class CSVImporterWork extends AbstractWork {

    private static final Log log = LogFactory.getLog(CSVImporterWork.class);

    private static final String TEMPLATE_IMPORT_RESULT = "templates/csvImportResult.ftl";

    public static final String CATEGORY_CSV_IMPORTER = "csvImporter";

    protected final CSVImportId id;

    protected String repositoryName;

    protected String parentPath;

    protected String username;

    protected Blob csvBlob;

    protected CSVImporterOptions options;

    protected DateFormat dateformat;

    protected List<CSVImportLog> importLogs = new ArrayList<CSVImportLog>();

    public CSVImporterWork(CSVImportId id) {
        this.id = id;
    }

    public CSVImporterWork(String repositoryName, String parentPath,
            String username, Blob csvBlob, CSVImporterOptions options) {
        this.id = CSVImportId.create(repositoryName, parentPath, csvBlob);
        this.repositoryName = repositoryName;
        this.parentPath = parentPath;
        this.username = username;
        this.csvBlob = csvBlob;
        this.options = options;
        this.dateformat = new SimpleDateFormat(options.getDateFormat());
    }

    public CSVImportId getId() {
        return id;
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
        return new ArrayList<CSVImportLog>(importLogs);
    }

    @Override
    public void work() throws Exception {
        setStatus("Importing");
        new UnrestrictedSessionRunner(repositoryName, username) {
            @Override
            public void run() throws ClientException {
                try {
                    doImport(session);
                } catch (IOException e) {
                    logError(0, "Error while doing the import: %s",
                            "label.csv.importer.errorDuringImport",
                            e.getMessage());
                    log.debug(e, e);
                }
            }
        }.runUnrestricted();

        if (options.sendEmail()) {
            setStatus("Sending email");
            new UnrestrictedSessionRunner(repositoryName, username) {
                @Override
                public void run() throws ClientException {
                    sendMail(session);
                }
            }.runUnrestricted();
        }
        setStatus(null);
    }

    protected void doImport(CoreSession session) throws IOException {
        log.info(String.format("Importing CSV file: %s", csvBlob.getFilename()));
        CSVReader csvReader = new CSVReader(csvBlob.getReader());

        String[] header = csvReader.readNext();
        if (header == null) {
            // empty file?
            logError(0, "No header line, empty file?",
                    "label.csv.importer.emptyFile");
            return;
        }

        // find the index for the required name and type values
        int nameIndex = -1;
        int typeIndex = -1;
        for (int col = 0; col < header.length; col++) {
            if (CSV_NAME_COL.equals(header[col])) {
                nameIndex = col;
            } else if (CSV_TYPE_COL.equals(header[col])) {
                typeIndex = col;
            }
        }
        if (nameIndex == -1 || typeIndex == -1) {
            logError(0, "Missing 'name' or 'type' column",
                    "label.csv.importer.missingNameOrTypeColumn");
            return;
        }

        boolean transactionStarted = false;
        if (!TransactionHelper.isTransactionActive()) {
            TransactionHelper.startTransaction();
            transactionStarted = true;
        }

        try {
            int batchSize = options.getBatchSize();
            long docsCreatedCount = 0;
            long lineNumber = 0;
            while (true) {
                lineNumber++;
                try {
                    String[] line = csvReader.readNext();
                    if (line == null) {
                        break; // no more line
                    }

                    if (line.length == 0) {
                        // empty line
                        importLogs.add(new CSVImportLog(lineNumber,
                                CSVImportLog.Status.SKIPPED, "Empty line",
                                "label.csv.importer.emptyLine"));
                        continue;
                    }

                    try {
                        if (importLine(session, line, lineNumber, nameIndex,
                                typeIndex, header)) {
                            docsCreatedCount++;
                            if (docsCreatedCount % batchSize == 0) {
                                TransactionHelper.commitOrRollbackTransaction();
                                TransactionHelper.startTransaction();
                            }
                        }
                    } catch (ClientException e) {
                        // try next line
                        Throwable unwrappedException = unwrapException(e);
                        logError(lineNumber, "Error while importing line: %s",
                                "label.csv.importer.errorImportingLine",
                                unwrappedException.getMessage());
                        log.debug(unwrappedException, unwrappedException);
                    }
                } catch (Exception e) {
                    // try next line
                    Throwable unwrappedException = unwrapException(e);
                    logError(lineNumber, "Error while reading line: %s",
                            "label.csv.importer.errorReadingLine",
                            unwrappedException.getMessage());
                    log.debug(unwrappedException, unwrappedException);
                }
            }
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            if (!transactionStarted) {
                TransactionHelper.startTransaction();
            }
        }
        log.info(String.format("Done importing CSV file: %s",
                csvBlob.getFilename()));
    }

    /**
     * Import a line from the CSV file.
     *
     * @return {@code true} if a document has been created or updated,
     *         {@code false} otherwise.
     */
    protected boolean importLine(CoreSession session, String[] line,
            final long lineNumber, int nameIndex, int typeIndex,
            String[] headerValues) throws ClientException {
        final String name = line[nameIndex];
        final String type = line[typeIndex];
        if (StringUtils.isBlank(name)) {
            logError(lineNumber, "Missing 'name' value",
                    "label.csv.importer.missingNameValue");
            return false;
        }
        if (StringUtils.isBlank(type)) {
            logError(lineNumber, "Missing 'type' value",
                    "label.csv.importer.missingTypeValue");
            return false;
        }

        DocumentType docType = Framework.getLocalService(SchemaManager.class).getDocumentType(
                type);
        if (docType == null) {
            logError(lineNumber, "The type '%s' does not exist",
                    "label.csv.importer.notExistingType", type);
            return false;
        }

        Map<String, Serializable> values = computePropertiesMap(lineNumber,
                docType, headerValues, line);
        if (values == null) {
            // skip this line
            return false;
        }

        return createOrUpdateDocument(lineNumber, session, parentPath, name,
                type, values);
    }

    protected Map<String, Serializable> computePropertiesMap(long lineNumber,
            DocumentType docType, String[] headerValues, String[] line) {
        Map<String, Serializable> values = new HashMap<String, Serializable>();
        for (int col = 0; col < headerValues.length; col++) {
            String headerValue = headerValues[col];
            String lineValue = line[col];

            String fieldName = headerValue;
            if (!CSV_NAME_COL.equals(headerValue)
                    && !CSV_TYPE_COL.equals(headerValue)) {
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
                        // not supported
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
                                fieldValue = (Serializable) Arrays.asList(stringValue.split(options.getListSeparatorRegex()));
                            }
                        } else {
                            /*
                             * Primitive type.
                             */
                            if (field.getType().isSimpleType()) {
                                if (field.getType() instanceof StringType) {
                                    fieldValue = stringValue;
                                } else if (field.getType() instanceof IntegerType) {
                                    fieldValue = Integer.parseInt(stringValue);
                                } else if (field.getType() instanceof LongType) {
                                    fieldValue = Long.parseLong(stringValue);
                                } else if (field.getType() instanceof DoubleType) {
                                    fieldValue = Double.parseDouble(stringValue);
                                } else if (field.getType() instanceof BooleanType) {
                                    fieldValue = Boolean.valueOf(stringValue);
                                } else if (field.getType() instanceof DateType) {
                                    fieldValue = dateformat.parse(stringValue);
                                }
                            }
                        }
                    }
                    return fieldValue;
                } catch (Exception e) {
                    logError(lineNumber,
                            "Unable to convert field '%s' with value '%s'",
                            "label.csv.importer.cannotConvertFieldValue",
                            headerValue, stringValue);
                    log.debug(e, e);
                }
            }
        } else {
            logError(lineNumber, "Field '%s' does not exist on type '%s'",
                    "label.csv.importer.notExistingField", headerValue,
                    docType.getName());
        }
        return null;
    }

    protected boolean createOrUpdateDocument(long lineNumber,
            CoreSession session, String parentPath, String name, String type,
            Map<String, Serializable> properties) throws ClientException {
        String targetPath = new Path(parentPath).append(name).toString();
        DocumentRef docRef = new PathRef(targetPath);
        if (session.exists(docRef)) {
            return updateDocument(lineNumber, session, docRef, properties);
        } else {
            return createDocument(lineNumber, session, parentPath, name, type,
                    properties);
        }
    }

    protected boolean createDocument(long lineNumber, CoreSession session,
            String parentPath, String name, String type,
            Map<String, Serializable> properties) {
        try {
            options.getCSVImporterDocumentFactory().createDocument(session,
                    parentPath, name, type, properties);
            importLogs.add(new CSVImportLog(lineNumber,
                    CSVImportLog.Status.SUCCESS, "Document created",
                    "label.csv.importer.documentCreated"));
            return true;
        } catch (Exception e) {
            Throwable unwrappedException = unwrapException(e);
            logError(lineNumber, "Unable to create document: %s",
                    "label.csv.importer.unableToCreate",
                    unwrappedException.getMessage());
            log.debug(unwrappedException, unwrappedException);
        }
        return false;
    }

    protected boolean updateDocument(long lineNumber, CoreSession session,
            DocumentRef docRef, Map<String, Serializable> properties) {
        if (options.updateExisting()) {
            try {
                options.getCSVImporterDocumentFactory().updateDocument(session,
                        docRef, properties);
                importLogs.add(new CSVImportLog(lineNumber,
                        CSVImportLog.Status.SUCCESS, "Document updated",
                        "label.csv.importer.documentUpdated"));
                return true;
            } catch (Exception e) {
                Throwable unwrappedException = unwrapException(e);
                logError(lineNumber, "Unable to update document: %s",
                        "label.csv.importer.unableToUpdate",
                        unwrappedException.getMessage());
                log.debug(unwrappedException, unwrappedException);
            }
        } else {
            importLogs.add(new CSVImportLog(lineNumber,
                    CSVImportLog.Status.SKIPPED, "Document already exists",
                    "label.csv.importer.documentAlreadyExists"));
        }
        return false;
    }

    protected void logError(long lineNumber, String message,
            String localizedMessage, Object... params) {
        importLogs.add(new CSVImportLog(lineNumber, ERROR, String.format(
                message, params), localizedMessage, params));
        String lineMessage = String.format("Line %d", lineNumber);
        String errorMessage = String.format(message, params);
        log.error(String.format("%s: %s", lineMessage, errorMessage));
    }

    protected void sendMail(CoreSession session) throws ClientException {
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
        List<CSVImportLog> importLogs = csvImporter.getImportLogs(id,
                CSVImportLog.Status.SKIPPED, CSVImportLog.Status.ERROR);
        ctx.put("importResult", CSVImportResult.fromImportLogs(importLogs));
        ctx.put("importLogs", importLogs);
        ctx.put("csvFilename", csvBlob.getFilename());

        Expression from = Scripting.newExpression("Env[\"mail.from\"]");
        StringList to = new StringList(new String[] { email });

        String subject = "CSV Import result of " + csvBlob.getFilename();
        String message = loadTemplate(TEMPLATE_IMPORT_RESULT);

        try {
            OperationChain chain = new OperationChain("SendMail");
            chain.add(SendMail.ID).set("from", from).set("to", to).set("HTML",
                    true).set("subject", subject).set("message", message);
            Framework.getLocalService(AutomationService.class).run(ctx, chain);
        } catch (Exception e) {
            log.error(String.format(
                    "Unable to notify user '%s' for import result of '%s': %s",
                    username, csvBlob.getFilename(), e.getMessage()));
            log.debug(e, e);
        }
    }

    private static String loadTemplate(String key) {
        InputStream io = CSVImporterWork.class.getClassLoader().getResourceAsStream(
                key);
        if (io != null) {
            try {
                return FileUtils.read(io);
            } catch (IOException e) {
                throw new ClientRuntimeException(e);
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

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof CSVImporterWork)) {
            return false;
        }
        return id.equals(((CSVImporterWork) other).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public static Throwable unwrapException(Throwable t) {
        Throwable cause = null;

        if (t instanceof ClientException) {
            cause = t.getCause();
        } else if (t instanceof Exception) {
            cause = t.getCause();
        }

        if (cause == null) {
            return t;
        } else {
            return unwrapException(cause);
        }
    }

}
