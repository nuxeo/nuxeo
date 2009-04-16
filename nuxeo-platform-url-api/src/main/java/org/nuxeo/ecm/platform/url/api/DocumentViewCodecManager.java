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
 * $Id: DocumentViewCodecManager.java 22535 2007-07-13 14:57:58Z atchertchian $
 */

package org.nuxeo.ecm.platform.url.api;

import java.io.Serializable;

/**
 * Service used generate meaningful and permanent urls.
 * <p>
 * It handles a set of codecs, used to code/decode a document information
 * between a url and a {@link DocumentView} instance.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public interface DocumentViewCodecManager extends Serializable {

    /**
     * Returns the default codec name.
     * <p>
     * This information is set on codec descriptors.
     */
    String getDefaultCodecName();

    DocumentView getDocumentViewFromUrl(String url, boolean hasBaseUrl,
            String baseUrl);

    DocumentView getDocumentViewFromUrl(String codecName, String url,
            boolean hasBaseUrl, String baseUrl);

    String getUrlFromDocumentView(DocumentView docView, boolean needBaseUrl,
            String baseUrl);

    String getUrlFromDocumentView(String codecName, DocumentView docView,
            boolean needBaseUrl, String baseUrl);

}
