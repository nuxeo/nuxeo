/*
 * (C) Copyright 2002-20012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.diff.content.converters;

import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionException;

/**
 * Html converter for content diff.
 * <p>
 * Uses the "office2html" converter.
 *
 * @author Antoine Taillefer
 */
public class ContentDiffHtmlConverter extends AbstractContentDiffConverter {

    private static final String OFFICE_2_HTML_CONVERTER_NAME = "office2html";

    public BlobHolder convert(BlobHolder blobHolder,
            Map<String, Serializable> parameters) throws ConversionException {

        return convert(OFFICE_2_HTML_CONVERTER_NAME, blobHolder, parameters);
    }

}
