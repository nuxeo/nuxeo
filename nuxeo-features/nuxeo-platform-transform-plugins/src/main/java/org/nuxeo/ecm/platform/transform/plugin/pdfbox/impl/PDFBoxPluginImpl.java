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
 *     Nuxeo - initial API and implementation
 *
 * $Id: PDFBoxPluginImpl.java 28924 2008-01-10 14:04:05Z sfermigier $
 */

package org.nuxeo.ecm.platform.transform.plugin.pdfbox.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.ByteArrayBlob;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.transform.document.TransformDocumentImpl;
import org.nuxeo.ecm.platform.transform.interfaces.TransformDocument;
import org.nuxeo.ecm.platform.transform.plugin.AbstractPlugin;
import org.nuxeo.ecm.platform.transform.plugin.pdfbox.api.PDFBoxPlugin;
import org.nuxeo.ecm.platform.transform.timer.SimpleTimer;
import org.pdfbox.pdmodel.PDDocument;
import org.pdfbox.pdmodel.encryption.PDEncryptionDictionary;
import org.pdfbox.pdmodel.encryption.PDStandardEncryption;
import org.pdfbox.util.PDFTextStripper;

/**
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class PDFBoxPluginImpl extends AbstractPlugin implements PDFBoxPlugin {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(PDFBoxPluginImpl.class);

    public PDFBoxPluginImpl() {
        // Only takes PDF as sources documents.
        sourceMimeTypes = Arrays.asList("application/pdf");

        // Only outputs text.
        destinationMimeType = "plain/text";
    }

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
            // Below, this will likely fetch the stream from the streaming
            // server
            InputStream stream = sources[0].getBlob().getStream();
            if (stream != null) {
                trs.add(transformOne(stream));
            } else {
                log.warn("Stream is null, ignore");
            }
        } catch (Exception e) {
            log.error("An error occured while trying transform pdf to text...",
                    e);
        } finally {
            timer.stop();
            log.debug("Transformation terminated plugin side. " + timer);
        }

        return trs;
    }

    private TransformDocument transformOne(InputStream stream) throws Exception {

        PDDocument document = PDDocument.load(stream);
        PDFTextStripper textStripper = new PDFTextStripper();

        // Let's put the content on a file instead of working in memory

        // Jira NXP-1556: if document is protected an IOException will be raised
        // Instead of catching the exception based on its message string
        // lets avoid sending messages that will generate this error
        // code taken from PDFTextStripper.writeText source.
        Boolean isReadable = true;
        PDEncryptionDictionary encDictionary = document.getEncryptionDictionary();
        //only care about standard encryption and if it was decrypted with the
        //user password
        if (encDictionary instanceof PDStandardEncryption
                && !document.wasDecryptedWithOwnerPassword()) {
            PDStandardEncryption stdEncryption = (PDStandardEncryption) encDictionary;
            isReadable = stdEncryption.canExtractContent();
        }

        Blob blob;
        File f = null;
        if (isReadable) {
            f = File.createTempFile("pdfboplugin", ".txt");
            OutputStream fas = new FileOutputStream(f);
            byte[] bytes = textStripper.getText(document).getBytes();
            fas.write(bytes);
            blob = new FileBlob(new FileInputStream(f));
        } else {
            blob = new ByteArrayBlob(new byte[] {});
        }

        blob.setMimeType(destinationMimeType);

        document.close();

        if (f != null) {
            f.delete();
        }

        return new TransformDocumentImpl(blob);
    }
}
