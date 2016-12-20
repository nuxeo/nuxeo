/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.mail.action;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;

/**
 * Transforms the message using the transformer and puts it in the context under transformed.
 *
 * @author Alexandre Russel
 */
public class TransformMessageAction implements MessageAction {

    private static final Log log = LogFactory.getLog(TransformMessageAction.class);

    protected final Map<String, Map<String, Object>> schemas = new HashMap<String, Map<String, Object>>();

    protected final Map<String, Object> mailSchema = new HashMap<String, Object>();

    protected final Map<String, Object> dcSchema = new HashMap<String, Object>();

    protected final Map<String, Object> filesSchema = new HashMap<String, Object>();

    protected final List<Map<String, Object>> files = new ArrayList<Map<String, Object>>();

    protected StringBuilder text = new StringBuilder();

    private final HashMap<String, List<Part>> messageBodyParts = new HashMap<String, List<Part>>();

    public TransformMessageAction() {
        messageBodyParts.put("text", new ArrayList<Part>());
        messageBodyParts.put("html", new ArrayList<Part>());
        schemas.put("mail", mailSchema);
        schemas.put("dublincore", dcSchema);
        filesSchema.put("files", files);
        schemas.put("files", filesSchema);
    }

    public boolean execute(ExecutionContext context) throws MessagingException {
        Message message = context.getMessage();
        if (log.isDebugEnabled()) {
            log.debug("Transforming message" + message.getSubject());
        }
        if (message.getFrom() != null && message.getFrom().length != 0) {
            List<String> contributors = new ArrayList<String>();
            for (Address ad : message.getFrom()) {
                contributors.add(safelyDecodeText(ad.toString()));
            }
            dcSchema.put("contributors", contributors);
            dcSchema.put("creator", contributors.get(0));
            dcSchema.put("created", message.getReceivedDate());
        }
        if (message.getAllRecipients() != null && message.getAllRecipients().length != 0) {
            List<String> recipients = new ArrayList<String>();
            for (Address address : message.getAllRecipients()) {
                recipients.add(safelyDecodeText(address.toString()));
            }
            mailSchema.put("recipients", recipients);
        }
        if (message instanceof MimeMessage) {
            try {
                processMimeMessage((MimeMessage) message);
            } catch (IOException e) {
                throw new MessagingException(e.getMessage(), e);
            }
        }
        mailSchema.put("text", text.toString());
        dcSchema.put("title", message.getSubject());
        context.put("transformed", schemas);
        return true;
    }

    private void processMimeMessage(MimeMessage message) throws MessagingException, IOException {
        Object object = message.getContent();
        if (object instanceof String) {
            addToTextMessage(message.getContent().toString(), true);
        } else if (object instanceof MimeMultipart) {
            processMultipartMessage((MimeMultipart) object);
            processSavedTextMessageBody();
        }
    }

    private void processMultipartMessage(MimeMultipart parts) throws MessagingException, IOException {
        log.debug("processing multipart message.");
        for (int i = 0; i < parts.getCount(); i++) {
            Part part = parts.getBodyPart(i);
            if (part.getDataHandler().getContent() instanceof MimeMultipart) {
                log.debug("** found embedded multipart message");
                processMultipartMessage((MimeMultipart) part.getDataHandler().getContent());
                continue;
            }
            log.debug("processing single part message: " + part.getClass());
            processSingleMessagePart(part);
        }
    }

    private void processSingleMessagePart(Part part) throws MessagingException, IOException {
        String partContentType = part.getContentType();
        String partFileName = getFileName(part);

        if (partFileName != null) {
            log.debug("Add named attachment: " + partFileName);
            setFile(partFileName, part.getInputStream());
            return;
        }

        if (!contentTypeIsReadableText(partContentType)) {
            log.debug("Add unnamed binary attachment.");
            setFile(null, part.getInputStream());
            return;
        }

        if (contentTypeIsPlainText(partContentType)) {
            log.debug("found plain text unnamed attachment [save for later processing]");
            messageBodyParts.get("text").add(part);
            return;
        }

        log.debug("found html unnamed attachment [save for later processing]");
        messageBodyParts.get("html").add(part);
    }

    private void processSavedTextMessageBody() throws MessagingException, IOException {
        if (messageBodyParts.get("text").isEmpty()) {
            log.debug("entering case 2: no plain text found -> html is the body of the message.");
            addPartsToTextMessage(messageBodyParts.get("html"));
        } else {
            log.debug("entering case 1: text is saved as message body and html as attachment.");
            addPartsToTextMessage(messageBodyParts.get("text"));
            addPartsAsAttachements(messageBodyParts.get("html"));
        }
    }

    private void addPartsToTextMessage(List<Part> someMessageParts) throws MessagingException, IOException {
        for (Part part : someMessageParts) {
            addToTextMessage(part.getContent().toString(), contentTypeIsPlainText(part.getContentType()));
        }
    }

    private void addPartsAsAttachements(List<Part> someMessageParts) throws MessagingException, IOException {
        for (Part part : someMessageParts) {
            setFile(getFileName(part), part.getInputStream());
        }
    }

    private static boolean contentTypeIsReadableText(String contentType) {
        boolean isText = contentTypeIsPlainText(contentType);
        boolean isHTML = contentTypeIsHtml(contentType);
        return isText || isHTML;
    }

    private static boolean contentTypeIsHtml(String contentType) {
        contentType = contentType.trim().toLowerCase();
        return contentType.startsWith("text/html");
    }

    private static boolean contentTypeIsPlainText(String contentType) {
        contentType = contentType.trim().toLowerCase();
        return contentType.startsWith("text/plain");
    }

    /**
     * "javax.mail.internet.MimeBodyPart" is decoding the file name (with special characters) if it has the
     * "mail.mime.decodefilename" sysstem property set but the "com.sun.mail.imap.IMAPBodyPart" subclass of MimeBodyPart
     * is overriding getFileName() and never deal with encoded file names. the filename is decoded with the utility
     * function: MimeUtility.decodeText(filename); so we force here a filename decode. MimeUtility.decodeText is doing
     * nothing if the text is not encoded
     */
    private static String getFileName(Part mailPart) throws MessagingException {
        String sysPropertyVal = System.getProperty("mail.mime.decodefilename");
        boolean decodeFileName = sysPropertyVal != null && !sysPropertyVal.equalsIgnoreCase("false");

        String encodedFilename = mailPart.getFileName();

        if (!decodeFileName || encodedFilename == null) {
            return encodedFilename;
        }

        try {
            return MimeUtility.decodeText(encodedFilename);
        } catch (UnsupportedEncodingException ex) {
            throw new MessagingException("Can't decode attachment filename.", ex);
        }
    }

    private static String safelyDecodeText(String textToDecode) {
        try {
            return MimeUtility.decodeText(textToDecode);
        } catch (UnsupportedEncodingException ex) {
            log.error("Can't decode text. Use undecoded!", ex);
            return textToDecode;
        }
    }

    private void setFile(String fileName, InputStream inputStream) throws IOException {
        log.debug("* adding attachment: " + fileName);
        Map<String, Object> map = new HashMap<String, Object>();
        Blob fileBlob = Blobs.createBlob(inputStream);
        fileBlob.setFilename(fileName);
        map.put("file", fileBlob);
        files.add(map);
    }

    private void addToTextMessage(String message, boolean isPlainText) {
        log.debug("* adding text to message body: " + message);
        // if(isPlainText){
        // message = "<pre>" + message + "</pre>";
        // }
        text.append(message);
    }

    public void reset(ExecutionContext context) {
        mailSchema.clear();
        dcSchema.clear();
        files.clear();
        text = new StringBuilder();

        messageBodyParts.get("text").clear();
        messageBodyParts.get("html").clear();
    }

}
