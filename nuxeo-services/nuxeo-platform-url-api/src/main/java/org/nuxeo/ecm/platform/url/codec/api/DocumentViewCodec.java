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
 * $Id: DocumentViewCodec.java 29556 2008-01-23 00:59:39Z jcarsique $
 */

package org.nuxeo.ecm.platform.url.codec.api;

import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.api.DocumentViewCodecManager;

public interface DocumentViewCodec {

    String getPrefix();

    void setPrefix(String prefix);

    /**
     * Returns true if this codec should apply when iterating over codecs to
     * find a matching one.
     *
     * @see DocumentViewCodecManager#getUrlFromDocumentView(DocumentView,
     *      boolean, String)
     */
    boolean handleDocumentView(DocumentView docView);

    /**
     * Returns true if this codec should apply when iterating over codecs to
     * find a matching one.
     *
     * @see DocumentViewCodecManager#getDocumentViewFromUrl(String, boolean,
     *      String)
     */
    boolean handleUrl(String url);

    /**
     * Extracts the document view from given url.
     * <p>
     * The url is partial: it does not hold the context path information
     * (server:port/nuxeo).
     *
     * @param url the partial url to redirect to.
     * @return a document view instance.
     */
    DocumentView getDocumentViewFromUrl(String url);

    /**
     * Builds an url from the given document view.
     * <p>
     * The url should be partial: it should not hold the context path
     * information (server:port/nuxeo).
     *
     * @param docView
     * @return
     */
    String getUrlFromDocumentView(DocumentView docView);

}
