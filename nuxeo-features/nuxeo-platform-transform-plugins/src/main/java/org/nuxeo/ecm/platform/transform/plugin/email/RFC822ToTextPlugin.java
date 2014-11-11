/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.transform.plugin.email;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.mail.Address;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Message.RecipientType;
import javax.mail.internet.ContentType;
import javax.mail.internet.MimeMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.transform.document.TransformDocumentImpl;
import org.nuxeo.ecm.platform.transform.interfaces.Plugin;
import org.nuxeo.ecm.platform.transform.interfaces.TransformDocument;
import org.nuxeo.ecm.platform.transform.interfaces.TransformServiceCommon;
import org.nuxeo.ecm.platform.transform.plugin.AbstractPlugin;
import org.nuxeo.ecm.platform.transform.timer.SimpleTimer;
import org.nuxeo.runtime.api.Framework;

/**
 * RFC822 to text plugin
 * <p>
 * Extracts text from message parts, considering only one alternative when
 * dealing with alternative subparts.
 * <p>
 * Also contains usual message part headers such as subject, sender, recipients,
 * parts filename and description.
 *
 * @author Anahide Tchertchian
 *
 */
public class RFC822ToTextPlugin extends AbstractPlugin {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(RFC822ToTextPlugin.class);

    private static final String MESSAGE_RFC822_MIMETYPE = "message/rfc822";

    private static final String TXT_MT = "text/plain";

    @Override
    public List<TransformDocument> transform(Map<String, Serializable> options,
            TransformDocument... sources) throws Exception {
        List<TransformDocument> trs = new ArrayList<TransformDocument>();
        if (sources.length < 0 || sources[0] == null) {
            return trs;
        }

        SimpleTimer timer = new SimpleTimer();
        try {
            timer.start();
            trs = super.transform(options, sources);

            for (TransformDocument td : sources) {
                Blob outblob = extractTextFromMessage(td.getBlob());
                trs.add(new TransformDocumentImpl(outblob));
            }
        } finally {
            timer.stop();
            log.debug("Transformation terminated." + timer);
        }

        return trs;
    }

    protected Blob extractTextFromMessage(Blob blob) throws IOException {
        if (blob != null) {
            File f = null;
            OutputStream fo = null;
            try {
                TransformServiceCommon service = Framework.getService(TransformServiceCommon.class);
                if (service == null) {
                    log.error("Could not retrieve transform service");
                    return null;
                }
                MimeMessage msg = new MimeMessage((Session) null,
                        blob.getStream());
                f = File.createTempFile("rfc822totext", ".txt");
                fo = new FileOutputStream(f);
                List<Part> parts = getAttachmentParts(msg);
                writeInfo(fo, msg.getSubject());
                writeInfo(fo, msg.getFrom());
                writeInfo(fo, msg.getRecipients(RecipientType.TO));
                writeInfo(fo, msg.getRecipients(RecipientType.CC));
                for (Part part : parts) {
                    writeInfo(fo, part.getFileName());
                    writeInfo(fo, part.getDescription());
                    byte[] extracted = extractTextFromMessagePart(service, part);
                    if (extracted != null) {
                        writeInfo(fo, extracted);
                    }
                }
                Blob outblob = new FileBlob(new FileInputStream(f));
                outblob.setMimeType(getDestinationMimeType());
                return outblob;
            } catch (Exception e) {
                log.error(e);
            } finally {
                if (fo != null) {
                    try {
                        fo.close();
                    } catch (IOException e) {
                        log.error(e);
                    }
                }
                if (f != null) {
                    f.delete();
                }
            }
        }
        return null;
    }

    protected void writeInfo(OutputStream stream, Address address) {
        if (address != null) {
            try {
                stream.write(address.toString().getBytes());
                stream.write(" ".getBytes());
            } catch (Exception e) {
                log.error(e);
            }
        }
    }

    protected void writeInfo(OutputStream stream, Address[] addresses) {
        if (addresses != null) {
            for (Address address : addresses) {
                writeInfo(stream, address);
            }
        }
    }

    protected void writeInfo(OutputStream stream, String info) {
        if (info != null) {
            try {
                stream.write(info.getBytes());
                stream.write(" ".getBytes());
            } catch (Exception e) {
                log.error(e);
            }
        }
    }

    protected void writeInfo(OutputStream stream, byte[] info) {
        if (info != null) {
            try {
                stream.write(info);
                stream.write(" ".getBytes());
            } catch (Exception e) {
                log.error(e);
            }
        }
    }

    protected byte[] extractTextFromMessagePart(TransformServiceCommon service,
            Part p) throws Exception {
        ContentType contentType = new ContentType(p.getContentType());
        String baseType = contentType.getBaseType();
        if (TXT_MT.equals(baseType)) {
            Object content = p.getContent();
            if (content instanceof String) {
                return ((String) content).getBytes();
            } else {
                return null;
            }
        }
        Plugin plugin = service.getPluginByMimeTypes(baseType, TXT_MT);
        if (plugin != null) {
            List<TransformDocument> docs = plugin.transform(null, new FileBlob(
                    p.getInputStream()));
            if (docs != null && !docs.isEmpty()) {
                return docs.get(0).getBlob().getByteArray();
            }
        }
        return null;
    }

    protected List<Part> getAttachmentParts(Part p) throws Exception {
        List<Part> res = new ArrayList<Part>();
        if (p.isMimeType(MESSAGE_RFC822_MIMETYPE)) {
            res.addAll(getAttachmentParts((Part) p.getContent()));
        } else if (p.isMimeType("multipart/alternative")) {
            // only return one of the text alternatives
            Multipart mp = (Multipart) p.getContent();
            int count = mp.getCount();
            Part alternativePart = null;
            for (int i = 0; i < count; i++) {
                Part subPart = mp.getBodyPart(i);
                if (subPart.isMimeType(TXT_MT)) {
                    alternativePart = subPart;
                    break;
                } else if (subPart.isMimeType("text/*")) {
                    alternativePart = subPart;
                } else {
                    res.addAll(getAttachmentParts(subPart));
                }
            }
            if (alternativePart != null) {
                res.add(alternativePart);
            }
        } else if (p.isMimeType("multipart/*")) {
            Multipart mp = (Multipart) p.getContent();
            int count = mp.getCount();
            for (int i = 0; i < count; i++) {
                res.addAll(getAttachmentParts(mp.getBodyPart(i)));
            }
        } else {
            res.add(p);
        }
        return res;
    }
}
