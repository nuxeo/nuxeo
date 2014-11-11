/*
 * (C) Copyright 2002-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Julien Anguenot
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.convert.plugins.text.extractors;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.cache.SimpleCachableBlobHolder;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;
import org.pdfbox.pdmodel.PDDocument;
import org.pdfbox.pdmodel.encryption.PDEncryptionDictionary;
import org.pdfbox.pdmodel.encryption.PDStandardEncryption;
import org.pdfbox.util.PDFTextStripper;

@SuppressWarnings("deprecation")
public class PDF2TextConverter implements Converter {

    private static final Log log = LogFactory.getLog(PDF2TextConverter.class);

    public BlobHolder convert(BlobHolder blobHolder,
            Map<String, Serializable> parameters) throws ConversionException {

        PDDocument document = null;
        File f = null;
        OutputStream fas = null;
        try {
            document = PDDocument.load(blobHolder.getBlob().getStream());
            PDFTextStripper textStripper = new PDFTextStripper();

            // NXP-1556: if document is protected an IOException will be raised
            // Instead of catching the exception based on its message string
            // lets avoid sending messages that will generate this error
            // code taken from PDFTextStripper.writeText source.
            Boolean isReadable = true;
            PDEncryptionDictionary encDictionary = document
                    .getEncryptionDictionary();
            // only care about standard encryption and if it was decrypted with
            // the
            // user password
            if (encDictionary instanceof PDStandardEncryption
                    && !document.wasDecryptedWithOwnerPassword()) {
                PDStandardEncryption stdEncryption = (PDStandardEncryption) encDictionary;
                isReadable = stdEncryption.canExtractContent();
            }
            if (isReadable) {
                String text = textStripper.getText(document);
                text = text.replace("\u00a0", " ");
                f = File.createTempFile("pdfboplugin", ".txt");
                fas = new FileOutputStream(f);
                fas.write(text.getBytes("UTF-8"));
                return new SimpleCachableBlobHolder(new FileBlob(
                        new FileInputStream(f), "text/plain", "UTF-8"));
            } else {
                return new SimpleCachableBlobHolder(new StringBlob(""));
            }
        } catch (Exception e) {
            throw new ConversionException(
                    "Error dring text extraction with PDFBox", e);
        } finally {
            if (document != null) {
                try {
                    document.close();
                } catch (Exception e) {
                    log.error("Error while closing PDFBox document", e);
                }
            }
            if (fas != null) {
                try {
                    fas.close();
                } catch (IOException e) {
                    log.error(e);
                }
            }
            if (f != null) {
                f.delete();
            }
        }
    }

    public void init(ConverterDescriptor descriptor) {
    }

}
