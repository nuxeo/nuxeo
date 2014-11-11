/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.api.LogEntryBase;
import org.nuxeo.ecm.platform.audit.ejb.LogEntryImpl;

/**
 * Audit log entry importer/exporter.
 * <p>
 * Could be overriden to externalize additional information of a redefined
 * LogEntry.
 *
 * @author DM
 */
public class IOLogEntryBase {

    public final static String DOCUMENT_TAG = "documentLogs";

    public final static String LOGENTRY_TAG = "logEntry";


    public static void write(List<LogEntry> logEntries, OutputStream out)
            throws IOException {
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
     * Could be overriden to put other (additional) data.
     *
     * @param logEntryElement
     * @param logEntry
     */
    protected static void writeLogEntry(Element logEntryElement, LogEntry logEntry) {
        logEntryElement.addAttribute("category", logEntry.getCategory());
        logEntryElement.addAttribute("comment", logEntry.getComment());
        logEntryElement.addAttribute("docLifeCycle", logEntry.getDocLifeCycle());
        logEntryElement.addAttribute("docPath", logEntry.getDocPath());
        logEntryElement.addAttribute("docType", logEntry.getDocType());
        logEntryElement.addAttribute("docUUID", logEntry.getDocUUID());

        String creationDate = getDateFormat().format(logEntry.getEventDate());
        logEntryElement.addAttribute("creationDate", creationDate);
        logEntryElement.addAttribute("eventId", logEntry.getEventId());
        logEntryElement.addAttribute("principalName",
                logEntry.getPrincipalName());
    }

    public static List<LogEntry> read(InputStream in) throws IOException {
        Document jDoc = loadXML(in);
        return readDocument(jDoc);
    }

    /**
     * Will translate from a jdoc to a list of LogEntry objects.
     *
     * @param doc
     */
    @SuppressWarnings({"unchecked"})
    protected static List<LogEntry> readDocument(Document doc) {
        List<LogEntry> logEntries = new ArrayList<LogEntry>();

        Element rootElement = doc.getRootElement();
        Iterator<Element> it = rootElement.elementIterator();
        while (it.hasNext()) {
            Element logEntryElement = it.next();

            LogEntry logEntry = readLogEntry(logEntryElement);
            logEntries.add(logEntry);
        }

        return logEntries;
    }

    /**
     * Could be overriden to get other (additional) data.
     *
     * @param logEntryElement
     */
    protected static LogEntry readLogEntry(Element logEntryElement) {
        LogEntryBase logEntry = new LogEntryBase();

        logEntry.setCategory(logEntryElement.attributeValue("category"));
        logEntry.setComment(logEntryElement.attributeValue("comment"));
        logEntry.setDocLifeCycle(logEntryElement.attributeValue("docLifeCycle"));
        logEntry.setDocPath(logEntryElement.attributeValue("docPath"));
        logEntry.setDocType(logEntryElement.attributeValue("docType"));
        logEntry.setDocUUID(logEntryElement.attributeValue("docUUID"));

        try {
            Date creationDate = getDateFormat().parse(
                    logEntryElement.attributeValue("creationDate"));
            logEntry.setEventDate(creationDate);
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        logEntry.setEventId(logEntryElement.attributeValue("eventId"));
        logEntry.setPrincipalName(logEntryElement.attributeValue("principalName"));

        return logEntry;
    }

    /**
     * Specifies date-string conversion.
     *
     * @return
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
            FileUtils.copy(in, baos);
            return new SAXReader().read(new ByteArrayInputStream(
                    baos.toByteArray()));
        } catch (DocumentException e) {
            IOException ioe = new IOException("Failed to read log entry "
                    + ": " + e.getMessage());
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
     *
     * @param logEntry
     * @param newRef
     * @return
     */
    private static LogEntry translate(LogEntry logEntry, DocumentRef newRef) {
        LogEntryBase newLogEntry = new LogEntryImpl();

        newLogEntry.setCategory(logEntry.getCategory());
        newLogEntry.setComment(logEntry.getComment());
        newLogEntry.setDocLifeCycle(logEntry.getDocLifeCycle());
        // XXX ??? also the docPath?
        newLogEntry.setDocPath(logEntry.getDocPath());
        newLogEntry.setDocType(logEntry.getDocType());

        // changed that
        newLogEntry.setDocUUID(newRef.toString());

        newLogEntry.setEventDate(logEntry.getEventDate());
        newLogEntry.setEventId(logEntry.getEventId());
        newLogEntry.setPrincipalName(logEntry.getPrincipalName());

        return newLogEntry;
    }
}
