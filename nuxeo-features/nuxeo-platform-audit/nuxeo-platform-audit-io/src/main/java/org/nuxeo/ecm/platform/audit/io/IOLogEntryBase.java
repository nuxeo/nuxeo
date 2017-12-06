/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 *
 * $Id: $
 */
package org.nuxeo.ecm.platform.audit.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.audit.api.AuditLogger;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.runtime.api.Framework;

/**
 * Audit log entry importer/exporter.
 * <p>
 * Could be overridden to externalize additional information of a redefined LogEntry.
 *
 * @author DM
 */
// FIXME: design issue - this is a utility class (only static methods) with no subclasses (misleading name).
public class IOLogEntryBase {

    private static final Log log = LogFactory.getLog(IOLogEntryBase.class);

    public static final String DOCUMENT_TAG = "documentLogs";

    public static final String LOGENTRY_TAG = "logEntry";

    public static void write(List<LogEntry> logEntries, OutputStream out) throws IOException {
        Document jdoc = writeDocument(logEntries);
        writeXML(jdoc, out);
    }

    private static Document writeDocument(List<LogEntry> logEntries) {
        Document document = DocumentFactory.getInstance().createDocument();
        document.setName("logEntries");

        Element rootElement = document.addElement(DOCUMENT_TAG);
        for (LogEntry logEntry : logEntries) {
            Element logEntryElement = rootElement.addElement(LOGENTRY_TAG);
            writeLogEntry(logEntryElement, logEntry);
        }

        return document;
    }

    /**
     * Could be overridden to put other (additional) data.
     */
    protected static void writeLogEntry(Element logEntryElement, LogEntry logEntry) {
        logEntryElement.addAttribute("category", logEntry.getCategory());
        logEntryElement.addAttribute("comment", logEntry.getComment());
        logEntryElement.addAttribute("docLifeCycle", logEntry.getDocLifeCycle());
        logEntryElement.addAttribute("docPath", logEntry.getDocPath());
        logEntryElement.addAttribute("docType", logEntry.getDocType());
        logEntryElement.addAttribute("docUUID", logEntry.getDocUUID());
        logEntryElement.addAttribute("repoId", logEntry.getRepositoryId());

        String creationDate = getDateFormat().format(logEntry.getEventDate());
        logEntryElement.addAttribute("creationDate", creationDate);
        logEntryElement.addAttribute("eventId", logEntry.getEventId());
        logEntryElement.addAttribute("principalName", logEntry.getPrincipalName());
    }

    public static List<LogEntry> read(InputStream in) throws IOException {
        Document jDoc = loadXML(in);
        return readDocument(jDoc);
    }

    /**
     * Will translate from a jdoc to a list of LogEntry objects.
     */
    @SuppressWarnings({ "unchecked" })
    protected static List<LogEntry> readDocument(Document doc) {
        List<LogEntry> logEntries = new ArrayList<LogEntry>();

        AuditLogger audit = Framework.getService(AuditLogger.class);

        Element rootElement = doc.getRootElement();
        Iterator<Element> it = rootElement.elementIterator();
        while (it.hasNext()) {
            Element logEntryElement = it.next();

            LogEntry logEntry = readLogEntry(audit, logEntryElement);
            logEntries.add(logEntry);
        }

        return logEntries;
    }

    /**
     * Could be overridden to get other (additional) data.
     *
     * @param logEntryElement
     */
    protected static LogEntry readLogEntry(AuditLogger audit, Element logEntryElement) {
        LogEntry logEntry = audit.newLogEntry();

        logEntry.setCategory(logEntryElement.attributeValue("category"));
        logEntry.setComment(logEntryElement.attributeValue("comment"));
        logEntry.setDocLifeCycle(logEntryElement.attributeValue("docLifeCycle"));
        logEntry.setDocPath(logEntryElement.attributeValue("docPath"));
        logEntry.setDocType(logEntryElement.attributeValue("docType"));
        logEntry.setDocUUID(logEntryElement.attributeValue("docUUID"));
        logEntry.setRepositoryId(logEntryElement.attributeValue("repoId"));

        try {
            Date creationDate = getDateFormat().parse(logEntryElement.attributeValue("creationDate"));
            logEntry.setEventDate(creationDate);
        } catch (ParseException e) {
            log.error(e, e);
        }
        logEntry.setEventId(logEntryElement.attributeValue("eventId"));
        logEntry.setPrincipalName(logEntryElement.attributeValue("principalName"));

        return logEntry;
    }

    /**
     * Specifies date-string conversion.
     */
    protected static DateFormat getDateFormat() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }

    protected static void writeXML(Document doc, OutputStream out) throws IOException {
        OutputFormat format = OutputFormat.createPrettyPrint();
        XMLWriter writer = new XMLWriter(out, format);
        writer.write(doc);
    }

    private static Document loadXML(InputStream in) throws IOException {
        try {
            // the SAXReader is closing the stream so that we need to copy the
            // content somewhere
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IOUtils.copy(in, baos);
            return new SAXReader().read(new ByteArrayInputStream(baos.toByteArray()));
        } catch (DocumentException e) {
            IOException ioe = new IOException("Failed to read log entry " + ": " + e.getMessage());
            ioe.setStackTrace(e.getStackTrace());
            throw ioe;
        }
    }

    public static List<LogEntry> translate(List<LogEntry> docLogs, DocumentRef newRef) {
        List<LogEntry> newList = new ArrayList<LogEntry>();
        for (LogEntry logEntry : docLogs) {
            LogEntry newLogEntry = translate(logEntry, newRef);
            newList.add(newLogEntry);
        }
        return newList;
    }

    /**
     * Should be overridden if log data structure is changed.
     */
    private static LogEntry translate(LogEntry logEntry, DocumentRef newRef) {
        LogEntry newLogEntry;
        try {
            newLogEntry = (LogEntry) BeanUtils.cloneBean(logEntry);
        } catch (ReflectiveOperationException e) {
            throw new NuxeoException("cannot clone bean " + logEntry, e);
        }
        newLogEntry.setDocUUID(newRef);
        return newLogEntry;
    }

}
