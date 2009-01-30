/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.convert.plugins.text.extractors;

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
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.convert.cache.SimpleCachableBlobHolder;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;
import org.nuxeo.runtime.api.Framework;

public class RFC822ToTextConverter implements Converter {

    private static final Log log = LogFactory.getLog(RFC822ToTextConverter.class);

    private static final String MESSAGE_RFC822_MIMETYPE = "message/rfc822";
    private static final String TXT_MT = "text/plain";

    protected ConverterDescriptor descriptor;

    protected Blob extractTextFromMessage(Blob blob) {
        if (blob == null) {
            return null;
        }
        File f = null;
        OutputStream fo = null;
        try {
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
                byte[] extracted = extractTextFromMessagePart(part);
                if (extracted != null) {
                    writeInfo(fo, extracted);
                }
            }
            Blob outblob = new FileBlob(new FileInputStream(f));
            outblob.setMimeType(descriptor.getDestinationMimeType());
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
        return null;
    }

    protected static void writeInfo(OutputStream stream, Address address) {
        if (address != null) {
            try {
                stream.write(address.toString().getBytes());
                stream.write(" ".getBytes());
            } catch (Exception e) {
                log.error(e);
            }
        }
    }

    protected static void writeInfo(OutputStream stream, Address[] addresses) {
        if (addresses != null) {
            for (Address address : addresses) {
                writeInfo(stream, address);
            }
        }
    }

    protected static void writeInfo(OutputStream stream, String info) {
        if (info != null) {
            try {
                stream.write(info.getBytes());
                stream.write(" ".getBytes());
            } catch (Exception e) {
                log.error(e);
            }
        }
    }

    protected static void writeInfo(OutputStream stream, byte[] info) {
        if (info != null) {
            try {
                stream.write(info);
                stream.write(" ".getBytes());
            } catch (Exception e) {
                log.error(e);
            }
        }
    }

    protected static byte[] extractTextFromMessagePart(Part p) throws Exception {
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
        ConversionService cs = Framework.getLocalService(ConversionService.class);

        String converterName = cs.getConverterName(baseType, TXT_MT);
        if (converterName == null) {
            return null;
        } else {
            BlobHolder result = cs.convert(converterName, new SimpleBlobHolder(new FileBlob(p.getInputStream())), null);
            return result.getBlob().getByteArray();
        }
    }

    protected static List<Part> getAttachmentParts(Part p) throws Exception {
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

    public BlobHolder convert(BlobHolder blobHolder,
            Map<String, Serializable> parameters) throws ConversionException {

        Blob inputBlob;

        try {
            inputBlob = blobHolder.getBlob();
        }
        catch (Exception e) {
            throw new ConversionException("Error while getting blob from Holder", e);
        }
        Blob outblob;
        try {
            outblob = extractTextFromMessage(inputBlob);
        } catch (Exception e) {
            throw new ConversionException("Error in Text conversion", e);
        }

        return new SimpleCachableBlobHolder(outblob);
    }

    public void init(ConverterDescriptor descriptor) {
        this.descriptor = descriptor;
    }

}
