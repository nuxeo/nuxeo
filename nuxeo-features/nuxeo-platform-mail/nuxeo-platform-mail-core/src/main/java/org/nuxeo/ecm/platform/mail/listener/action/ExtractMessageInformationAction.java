/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.mail.listener.action;

import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.ATTACHMENTS_KEY;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.CC_RECIPIENTS_KEY;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.CONTENT_KEY;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.MIMETYPE_SERVICE_KEY;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.RECIPIENTS_KEY;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.SENDER_EMAIL_KEY;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.SENDER_KEY;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.SENDING_DATE_KEY;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.SUBJECT_KEY;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.TEXT_KEY;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.AddressException;
import javax.mail.internet.ContentType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimePart;
import javax.mail.internet.MimeUtility;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.platform.mail.action.ExecutionContext;
import org.nuxeo.ecm.platform.mail.utils.MailCoreConstants;
import org.nuxeo.ecm.platform.mimetype.MimetypeDetectionException;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.runtime.api.Framework;

/**
 * Puts on the pipe execution context the values retrieved from the new messages found in the INBOX. These values are
 * used later when new MailMessage documents are created based on them.
 *
 * @author Catalin Baican
 */
public class ExtractMessageInformationAction extends AbstractMailAction {

    private static final Log log = LogFactory.getLog(ExtractMessageInformationAction.class);

    public static final String DEFAULT_BINARY_MIMETYPE = "application/octet-stream*";

    public static final String MESSAGE_RFC822_MIMETYPE = "message/rfc822";

    private String bodyContent;

    public static final String COPY_MESSAGE = "org.nuxeo.mail.imap.copy";

    @Override
    public boolean execute(ExecutionContext context) {
        bodyContent = "";

        boolean copyMessage = Boolean.parseBoolean(Framework.getProperty(COPY_MESSAGE, "false"));

        try {
            Message originalMessage = context.getMessage();
            if (log.isDebugEnabled()) {
                log.debug("Transforming message, original subject: " + originalMessage.getSubject());
            }

            // fully load the message before trying to parse to
            // override most of server bugs, see
            // http://java.sun.com/products/javamail/FAQ.html#imapserverbug
            Message message;
            if (originalMessage instanceof MimeMessage && copyMessage) {
                message = new MimeMessage((MimeMessage) originalMessage);
                if (log.isDebugEnabled()) {
                    log.debug("Transforming message after full load: " + message.getSubject());
                }
            } else {
                // stuck with the original one
                message = originalMessage;
            }

            // Subject
            String subject = message.getSubject();
            if (subject != null) {
                subject = subject.trim();
            }
            if (subject == null || "".equals(subject)) {
                subject = "<Unknown>";
            }
            context.put(SUBJECT_KEY, subject);

            // Sender
            try {
                Address[] from = message.getFrom();
                String sender = null;
                String senderEmail = null;
                if (from != null) {
                    Address addr = from[0];
                    if (addr instanceof InternetAddress) {
                        InternetAddress iAddr = (InternetAddress) addr;
                        senderEmail = iAddr.getAddress();
                        sender = iAddr.getPersonal() + " <" + senderEmail + ">";
                    } else {
                        sender += addr.toString();
                        senderEmail = sender;
                    }
                }
                context.put(SENDER_KEY, sender);
                context.put(SENDER_EMAIL_KEY, senderEmail);
            } catch (AddressException ae) {
                // try to parse sender from header instead
                String[] values = message.getHeader("From");
                if (values != null) {
                    context.put(SENDER_KEY, values[0]);
                }
            }
            // Sending date
            context.put(SENDING_DATE_KEY, message.getSentDate());

            // Recipients
            try {
                Address[] to = message.getRecipients(Message.RecipientType.TO);
                Collection<String> recipients = new ArrayList<>();
                if (to != null) {
                    for (Address addr : to) {
                        if (addr instanceof InternetAddress) {
                            InternetAddress iAddr = (InternetAddress) addr;
                            if (iAddr.getPersonal() != null) {
                                recipients.add(iAddr.getPersonal() + " <" + iAddr.getAddress() + ">");
                            } else {
                                recipients.add(iAddr.getAddress());
                            }
                        } else {
                            recipients.add(addr.toString());
                        }
                    }
                }
                context.put(RECIPIENTS_KEY, recipients);
            } catch (AddressException ae) {
                // try to parse recipient from header instead
                Collection<String> recipients = getHeaderValues(message, Message.RecipientType.TO.toString());
                context.put(RECIPIENTS_KEY, recipients);
            }

            // CC recipients

            try {
                Address[] toCC = message.getRecipients(Message.RecipientType.CC);
                Collection<String> ccRecipients = new ArrayList<>();
                if (toCC != null) {
                    for (Address addr : toCC) {
                        if (addr instanceof InternetAddress) {
                            InternetAddress iAddr = (InternetAddress) addr;
                            ccRecipients.add(iAddr.getPersonal() + " " + iAddr.getAddress());
                        } else {
                            ccRecipients.add(addr.toString());
                        }
                    }
                }
                context.put(CC_RECIPIENTS_KEY, ccRecipients);

            } catch (AddressException ae) {
                // try to parse ccRecipient from header instead
                Collection<String> ccRecipients = getHeaderValues(message, Message.RecipientType.CC.toString());
                context.put(CC_RECIPIENTS_KEY, ccRecipients);
            }
            String[] messageIdHeader = message.getHeader("Message-ID");
            if (messageIdHeader != null) {
                context.put(MailCoreConstants.MESSAGE_ID_KEY, messageIdHeader[0]);
            }

            MimetypeRegistry mimeService = (MimetypeRegistry) context.getInitialContext().get(MIMETYPE_SERVICE_KEY);

            List<Blob> blobs = new ArrayList<>();
            context.put(ATTACHMENTS_KEY, blobs);
            context.put(CONTENT_KEY, new HashMap<String, String>());

            // String[] cte = message.getHeader("Content-Transfer-Encoding");

            // process content
            getAttachmentParts(message, subject, mimeService, context);

            context.put(TEXT_KEY, bodyContent);

            return true;
        } catch (MessagingException | IOException e) {
            log.error(e, e);
        }
        return false;
    }

    protected static String getFilename(Part p, String defaultFileName) throws MessagingException {
        String originalFilename = p.getFileName();
        if (originalFilename == null || originalFilename.trim().length() == 0) {
            String filename = defaultFileName;
            // using default filename => add extension for this type
            if (p.isMimeType("text/plain")) {
                filename += ".txt";
            } else if (p.isMimeType("text/html")) {
                filename += ".html";
            }
            return filename;
        } else {
            try {
                return MimeUtility.decodeText(originalFilename.trim());
            } catch (UnsupportedEncodingException e) {
                return originalFilename.trim();
            }
        }
    }

    protected void getAttachmentParts(Part part, String defaultFilename, MimetypeRegistry mimeService,
            ExecutionContext context) throws MessagingException, IOException {
        String filename = getFilename(part, defaultFilename);
        List<Blob> blobs = (List<Blob>) context.get(ATTACHMENTS_KEY);
        Map<String, String> contentKeys = (Map<String, String>) context.get(CONTENT_KEY);

        if (part.isMimeType("multipart/alternative")) {
            bodyContent += getText(part, defaultFilename, mimeService, context);
        } else {
            if (!part.isMimeType("multipart/*")) {
                String disp = part.getDisposition();
                // no disposition => mail body, which can be also blob (image for
                // instance)
                if (disp == null && // convert only text
                        part.getContentType().toLowerCase().startsWith("text/")) {
                    bodyContent += decodeMailBody(part);
                } else {
                    Blob blob;
                    try (InputStream in = part.getInputStream()) {
                        blob = Blobs.createBlob(in);
                    }
                    String mime = DEFAULT_BINARY_MIMETYPE;
                    try {
                        if (mimeService != null) {
                            ContentType contentType = new ContentType(part.getContentType());
                            mime = mimeService.getMimetypeFromFilenameAndBlobWithDefault(filename, blob,
                                    contentType.getBaseType());
                        }
                    } catch (MessagingException | MimetypeDetectionException e) {
                        log.error(e);
                    }
                    blob.setMimeType(mime);

                    blob.setFilename(filename);
                    if (part instanceof MimePart) {
                        String contentId = ((MimePart) part).getContentID();
                        if (contentId != null) {
                            contentKeys.put(filename, contentId.replace("<", "").replace(">", ""));
                        }
                    }
                    blobs.add(blob);
                }
            }

            if (part.isMimeType("multipart/*")) {
                // This is a Multipart
                Multipart mp = (Multipart) part.getContent();

                int count = mp.getCount();
                for (int i = 0; i < count; i++) {
                    getAttachmentParts(mp.getBodyPart(i), defaultFilename, mimeService, context);
                }
            } else if (part.isMimeType(MESSAGE_RFC822_MIMETYPE)) {
                // This is a Nested Message
                getAttachmentParts((Part) part.getContent(), defaultFilename, mimeService, context);
            }
        }
    }

    /**
     * Return the primary text content of the message.
     */
    private String getText(Part p, String defaultFilename, MimetypeRegistry mimeService, ExecutionContext context)
            throws MessagingException, IOException {
        if (p.isMimeType("text/*")) {
            return decodeMailBody(p);
        }

        if (p.isMimeType("multipart/alternative")) {
            // prefer html text over plain text
            Multipart mp = (Multipart) p.getContent();
            String text = null;
            for (int i = 0; i < mp.getCount(); i++) {
                Part bp = mp.getBodyPart(i);
                if (bp.isMimeType("text/plain")) {
                    if (text == null) {
                        text = getText(bp, defaultFilename, mimeService, context);
                    }
                    continue;
                } else if (bp.isMimeType("text/html")) {
                    String s = getText(bp, defaultFilename, mimeService, context);
                    if (s != null) {
                        return s;
                    }
                } else {
                    return getText(bp, defaultFilename, mimeService, context);
                }
            }
            return text;
        } else if (p.isMimeType("multipart/*")) {
            Multipart mp = (Multipart) p.getContent();
            String s = null;
            for (int i = 0; i < mp.getCount(); i++) {
                BodyPart bodyPart = mp.getBodyPart(i);
                if (Part.INLINE.equals(bodyPart.getDisposition())) {
                    getAttachmentParts(bodyPart, defaultFilename, mimeService, context);
                } else {
                    s = getText(bodyPart, defaultFilename, mimeService, context);
                }
            }
            if (s != null) {
                return s;
            }
        }

        return null;
    }

    /**
     * Interprets the body accordingly to the charset used. It relies on the content type being
     * ****;charset={charset};******
     *
     * @return the decoded String
     */
    protected static String decodeMailBody(Part part) throws MessagingException, IOException {

        String encoding = null;

        // try to get encoding from header rather than from Stream !
        // unfortunately, this does not seem to be reliable ...
        /*
         * String[] cteHeader = part.getHeader("Content-Transfer-Encoding"); if (cteHeader!=null && cteHeader.length>0)
         * { encoding = cteHeader[0].toLowerCase(); }
         */

        // fall back to default sniffing
        // that will actually read the stream from server
        if (encoding == null) {
            encoding = MimeUtility.getEncoding(part.getDataHandler());
        }

        String contType = part.getContentType();
        final String charsetIdentifier = "charset=";
        final String ISO88591 = "iso-8859-1";
        final String WINDOWS1252 = "windows-1252";
        int offset = contType.indexOf(charsetIdentifier);
        String charset = "";
        if (offset >= 0) {
            charset = contType.substring(offset + charsetIdentifier.length());
            offset = charset.indexOf(";");
            if (offset > 0) {
                charset = charset.substring(0, offset);
            }
        }
        // Charset could be like "utf-8" or utf-8
        if (!"".equals(charset)) {
            charset = charset.replaceAll("\"", "");
        }
        log.debug("Content type: " + contType + "; charset: " + charset);
        if (charset.equalsIgnoreCase(ISO88591)) {
            // see
            // http://www.whatwg.org/specs/web-apps/current-work/multipage/parsing.html#character1
            // for more details see http://en.wikipedia.org/wiki/ISO_8859-1
            // section "ISO-8859-1 and Windows-1252 confusion"
            charset = WINDOWS1252;
            log.debug("Using replacing charset: " + charset);
        }

        try (InputStream is = MimeUtility.decode(part.getInputStream(), encoding)) {
            String ret;
            byte[] streamContent = IOUtils.toByteArray(is);
            if ("".equals(charset)) {
                ret = new String(streamContent);
            } else {
                try {
                    ret = new String(streamContent, charset);
                } catch (UnsupportedEncodingException e) {
                    // try without encoding
                    ret = new String(streamContent);
                }
            }
            return ret;
        } catch (IOException ex) {
            log.error("Unable to read content", ex);
            return "";
        }
    }

    public Collection<String> getHeaderValues(Message message, String headerName) throws MessagingException {
        Collection<String> valuesList = new ArrayList<>();
        String[] values = message.getHeader(headerName);
        if (values != null) {
            for (String value : values) {
                valuesList.add(value);
            }
        }
        return valuesList;
    }

}
