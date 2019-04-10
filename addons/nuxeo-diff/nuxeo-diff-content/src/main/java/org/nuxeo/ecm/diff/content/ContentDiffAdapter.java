/*
 * (C) Copyright 2006-20012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.diff.content;

import java.util.List;
import java.util.Locale;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.diff.content.adapter.base.ContentDiffConversionType;

/**
 * Interface for the content diff DocumentModel adapter.
 *
 * @author Antoine Taillefer
 * @since 5.6
 */
public interface ContentDiffAdapter {

    boolean cachable();

    String getFileContentDiffURL(DocumentModel otherDoc, ContentDiffConversionType conversionType, String locale);

    String getFileContentDiffURL(DocumentModel otherDoc, String xpath, ContentDiffConversionType conversionType,
            String locale);

    List<Blob> getFileContentDiffBlobs(DocumentModel otherDoc, ContentDiffConversionType conversionType, Locale locale)
            throws ContentDiffException, ConversionException;

    List<Blob> getFileContentDiffBlobs(DocumentModel otherDoc, String xpath, ContentDiffConversionType conversionType,
            Locale locale) throws ContentDiffException, ConversionException;

    void setAdaptedDocument(DocumentModel doc);

    void cleanup();

}
