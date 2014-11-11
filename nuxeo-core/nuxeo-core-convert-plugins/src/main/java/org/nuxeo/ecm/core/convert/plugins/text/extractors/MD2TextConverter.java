/*
 * (C) Copyright 2002-2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.core.convert.plugins.text.extractors;

import java.io.Serializable;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.cache.SimpleCachableBlobHolder;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;

/**
 * Markdown to text converter.
 * <p>
 * It basically returns a {@link StringBlob} with the markdown text and the
 * plain/text mime type.
 *
 * @author Antoine Taillefer (ataillefer@nuxeo.com)
 * @since 5.6
 */
public class MD2TextConverter implements Converter {

    private static final Log LOGGER = LogFactory.getLog(MD2TextConverter.class);

    @Override
    public BlobHolder convert(BlobHolder blobHolder,
            Map<String, Serializable> parameters) throws ConversionException {

        try {
            Blob blob = blobHolder.getBlob();
            if (blob == null) {
                LOGGER.warn("Trying to convert a blobHolder that has a null blob. Nothing to do, returning the blobHolder.");
                return blobHolder;
            }
            String text = blob.getString();
            return new SimpleCachableBlobHolder(new StringBlob(text,
                    "text/plain"));
        } catch (Exception e) {
            throw new ConversionException("Error during MD2Text conversion", e);
        }
    }

    @Override
    public void init(ConverterDescriptor descriptor) {
    }

}
