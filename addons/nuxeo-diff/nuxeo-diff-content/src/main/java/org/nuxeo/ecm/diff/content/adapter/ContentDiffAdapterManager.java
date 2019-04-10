/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
