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
 */
package org.nuxeo.ecm.diff.content.adapter;

import java.util.Set;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.diff.content.ContentDiffAdapter;
import org.nuxeo.ecm.diff.content.ContentDiffException;

/**
 * Interface of the service that is responsible for managing ContentDiffAdapters.
 *
 * @author Antoine Taillefer
 * @since 5.6
 */
public interface ContentDiffAdapterManager {

    ContentDiffAdapter getAdapter(DocumentModel doc);

    boolean hasAdapter(DocumentModel doc);

    MimeTypeContentDiffer getContentDiffer(String mimeType);

    /**
     * @since 7.4
     */
    MimeTypeContentDiffer getContentDifferForName(String name);

    HtmlContentDiffer getHtmlContentDiffer() throws ContentDiffException;

    /**
     * Gets the blacklisted mime types blacklisted for HTML conversion.
     *
     * @since 10.10
     */
    Set<String> getHtmlConversionBlacklistedMimeTypes();

}
