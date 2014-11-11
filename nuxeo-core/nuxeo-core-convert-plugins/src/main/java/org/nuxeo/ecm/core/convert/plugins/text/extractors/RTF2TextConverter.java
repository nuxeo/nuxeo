/*
 * (C) Copyright 2002-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.ecm.core.convert.plugins.text.extractors;

import java.io.File;
import java.io.FileInputStream;
import java.io.Serializable;
import java.util.Map;

import javax.swing.text.Document;
import javax.swing.text.rtf.RTFEditorKit;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.cache.SimpleCachableBlobHolder;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;

public class RTF2TextConverter implements Converter {

    @Override
    public BlobHolder convert(BlobHolder blobHolder,
            Map<String, Serializable> parameters) throws ConversionException {

        File f = null;
        try {
            RTFEditorKit rtfParser = new RTFEditorKit();
            Document document = rtfParser.createDefaultDocument();
            rtfParser.read(blobHolder.getBlob().getStream(), document, 0);
            String text = document.getText(0, document.getLength());
            f = File.createTempFile("swing-rtf2text", ".txt");
            FileUtils.writeFile(f, text);
            Blob blob = new FileBlob(new FileInputStream(f));
            blob.setMimeType("text/plain");
            return new SimpleCachableBlobHolder(blob);
        } catch (Exception e) {
            throw new ConversionException("Error during Word2Text conversion", e);
        } finally {
            if (f != null) {
                f.delete();
            }
        }
    }

    @Override
    public void init(ConverterDescriptor descriptor) {
    }

}
