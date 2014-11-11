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

import org.nuxeo.ecm.platform.url.codec.api.DocumentViewCodec;

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

    /**
     * Returns a DocumentView applying for given url, or null.
     * <p>
     * Iterates over registered codecs, starting from the default codec, and if
     * {@link DocumentViewCodec#handleUrl(String)} returns true, calls
     * {@link DocumentViewCodec#getDocumentViewFromUrl(String)}. Stops
     * iterating when a codec returns a non-null value.
     *
     * @param url the original url from request, including request parameters
     *            if any.
     * @param hasBaseUrl boolean indicating if base url should be removed from
     *            given url.
     * @param baseUrl value of the base url.
     */
    DocumentView getDocumentViewFromUrl(String url, boolean hasBaseUrl,
            String baseUrl);

    /**
     * Returns a DocumentView calling
     * {@link DocumentViewCodec#getDocumentViewFromUrl(String, boolean, String)}
     * on codec with given name.
     *
     * @param url the original url from request, including request parameters
     *            if any.
     * @param hasBaseUrl boolean indicating if base url should be removed from
     *            given url.
     * @param baseUrl value of the base url.
     */
    DocumentView getDocumentViewFromUrl(String codecName, String url,
            boolean hasBaseUrl, String baseUrl);

    /**
     * Returns an URL applying for given document view, or null.
     * <p>
     * Iterates over registered codecs, starting from the default codec, and if
     * {@link DocumentViewCodec#handleDocumentView(DocumentView)} returns true,
     * calls {@link DocumentViewCodec#getUrlFromDocumentView(DocumentView)}.
     * Stops iterating when a codec returns a non-null value. am docView the
     * original document view from request
     *
     * @param docView the original document view from request
     * @param hasBaseUrl boolean indicating if base url should be added to the
     *            url returned by the codec.
     * @param baseUrl value of the base url.
     */
    String getUrlFromDocumentView(DocumentView docView, boolean needBaseUrl,
            String baseUrl);

    /**
     * Returns an URL calling
     * {@link DocumentViewCodec#getUrlFromDocumentView(DocumentView) on codec
     * with given name.
     *
     * @param docView the original document view from request
     * @param hasBaseUrl boolean indicating if base url should be added to the
     *            url returned by the codec.
     * @param baseUrl value of the base url.
     */
    String getUrlFromDocumentView(String codecName, DocumentView docView,
            boolean needBaseUrl, String baseUrl);

}
