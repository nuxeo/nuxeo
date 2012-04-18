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
 * Text converter for content diff.
 * <p>
 * Uses the "any2text" converter.
 *
 * @author Antoine Taillefer
 */
public class ContentDiffTextConverter extends AbstractContentDiffConverter {

    private static final String ANY_2_TEXT_CONVERTER_NAME = "any2text";

    public BlobHolder convert(BlobHolder blobHolder,
            Map<String, Serializable> parameters) throws ConversionException {

        return convert(ANY_2_TEXT_CONVERTER_NAME, blobHolder, parameters);
    }

}
