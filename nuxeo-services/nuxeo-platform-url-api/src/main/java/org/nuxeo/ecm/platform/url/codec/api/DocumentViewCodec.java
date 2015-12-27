/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
     * Returns true if this codec should apply when iterating over codecs to find a matching one.
     *
     * @see DocumentViewCodecManager#getUrlFromDocumentView(DocumentView, boolean, String)
     */
    boolean handleDocumentView(DocumentView docView);

    /**
     * Returns true if this codec should apply when iterating over codecs to find a matching one.
     *
     * @see DocumentViewCodecManager#getDocumentViewFromUrl(String, boolean, String)
     */
    boolean handleUrl(String url);

    /**
     * Extracts the document view from given url.
     * <p>
     * The url is partial: it does not hold the context path information (server:port/nuxeo).
     *
     * @param url the partial url to redirect to.
     * @return a document view instance.
     */
    DocumentView getDocumentViewFromUrl(String url);

    /**
     * Builds an url from the given document view.
     * <p>
     * The url should be partial: it should not hold the context path information (server:port/nuxeo).
     *
     * @param docView
     * @return
     */
    String getUrlFromDocumentView(DocumentView docView);

}
