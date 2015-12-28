/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
