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
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.util.PDFOperator;
import org.apache.pdfbox.util.PDFStreamEngine;
import org.apache.pdfbox.util.PDFTextStripper;
import org.apache.pdfbox.util.operator.OperatorProcessor;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.cache.SimpleCachableBlobHolder;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;

public class PDF2TextConverter implements Converter {

    public static class PatchedPDFTextStripper extends PDFTextStripper {

        public PatchedPDFTextStripper() throws IOException {
            super();
            // platform independent line and paragraph separators
            setLineSeparator("\n");
            setParagraphEnd("\n\n");
            setArticleEnd("\n\n");
        }

        protected Object unrestrictedAccess(String name) {
            try {
                Field f = PDFStreamEngine.class.getDeclaredField(name);
                f.setAccessible(true);
                return f.get(this);
            } catch (Exception e) {
                throw new RuntimeException(
                        "Cannot get access to PDFStreamEngine fields", e);
            }
        }

        @SuppressWarnings("unchecked")
        protected Set<String> unsupportedOperators() {
            return (Set<String>) unrestrictedAccess("unsupportedOperators");
        }

        @SuppressWarnings("unchecked")
        protected Map<String, OperatorProcessor> operators() {
            return (Map<String, OperatorProcessor>) unrestrictedAccess("operators");
        }

        final static Set<StackTraceElement> loggedStacks = new HashSet<StackTraceElement>();

        @Override
        protected void processOperator(PDFOperator operator,
                List<COSBase> arguments) throws IOException {
            try {

                String operation = operator.getOperation();
                OperatorProcessor processor = operators().get(operation);
                if (processor != null) {
                    processor.setContext(this);
                    processor.process(operator, arguments);
                } else {
                    if (!unsupportedOperators().contains(operation)) {
                        log.info("unsupported/disabled operation: " + operation);
                        unsupportedOperators().add(operation);
                    }
                }
            } catch (Exception e) {
                StackTraceElement root = e.getStackTrace()[0];
                synchronized (loggedStacks) {
                    if (loggedStacks.contains(root)) {
                        return;
                    }
                    loggedStacks.add(root);
                }
                log.warn(
                        "Caught error in pdfbox during extraction (stack logged only once)",
                        e);
            }
        }

    }

    private static final Log log = LogFactory.getLog(PDF2TextConverter.class);

    @Override
    public BlobHolder convert(BlobHolder blobHolder,
            Map<String, Serializable> parameters) throws ConversionException {

        PDDocument document = null;
        File f = null;
        OutputStream fas = null;
        try {
            document = PDDocument.load(blobHolder.getBlob().getStream());
            // NXP-1556: if document is protected an IOException will be raised
            // Instead of catching the exception based on its message string
            // lets avoid sending messages that will generate this error
            // code taken from PDFTextStripper.writeText source.
            // only care about standard encryption and if it was decrypted with
            // the user password
            AccessPermission permission = document.getCurrentAccessPermission();
            if (permission.canExtractContent()) {
                PatchedPDFTextStripper textStripper = new PatchedPDFTextStripper();

                // use the position information to heuristically organize the
                // extracted paragraphs. This is also important for
                // right-to-left languages.
                textStripper.setSortByPosition(true);

                String text = textStripper.getText(document);
                // replace non breaking space by regular spaces (why?)
                // text = text.replace("\u00a0", " ");
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
                    "Error during text extraction with PDFBox", e);
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

    @Override
    public void init(ConverterDescriptor descriptor) {
    }

}
