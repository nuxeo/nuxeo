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
 * $Id: DocumentViewCodecManager.java 22535 2007-07-13 14:57:58Z atchertchian $
 */

package org.nuxeo.ecm.platform.url.api;

import java.io.Serializable;

import org.nuxeo.ecm.platform.url.codec.api.DocumentViewCodec;

/**
 * Service used generate meaningful and permanent urls.
 * <p>
 * It handles a set of codecs, used to code/decode a document information between a url and a {@link DocumentView}
 * instance.
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
     * Returns the {@link DocumentViewCodec} with the given name or {@code null} if ot doesn't exist.
     *
     * @since 8.10
     */
    DocumentViewCodec getCodec(String name);

    /**
     * Returns a DocumentView applying for given url, or null.
     * <p>
     * Iterates over registered codecs, starting from the default codec, and if
     * {@link DocumentViewCodec#handleUrl(String)} returns true, calls
     * {@link DocumentViewCodec#getDocumentViewFromUrl(String)}. Stops iterating when a codec returns a non-null value.
     *
     * @param url the original url from request, including request parameters if any.
     * @param hasBaseUrl boolean indicating if base url should be removed from given url.
     * @param baseUrl value of the base url.
     */
    DocumentView getDocumentViewFromUrl(String url, boolean hasBaseUrl, String baseUrl);

    /**
     * Returns a DocumentView calling {@link DocumentViewCodec#getDocumentViewFromUrl(String)} on codec with given name.
     *
     * @param url the original url from request, including request parameters if any.
     * @param hasBaseUrl boolean indicating if base url should be removed from given url.
     * @param baseUrl value of the base url.
     */
    DocumentView getDocumentViewFromUrl(String codecName, String url, boolean hasBaseUrl, String baseUrl);

    /**
     * Returns an URL applying for given document view, or null.
     * <p>
     * Iterates over registered codecs, starting from the default codec, and if
     * {@link DocumentViewCodec#handleDocumentView(DocumentView)} returns true, calls
     * {@link DocumentViewCodec#getUrlFromDocumentView(DocumentView)}. Stops iterating when a codec returns a non-null
     * value. am docView the original document view from request
     *
     * @param docView the original document view from request
     * @param needBaseUrl boolean indicating if base url should be added to the url returned by the codec.
     * @param baseUrl value of the base url.
     */
    String getUrlFromDocumentView(DocumentView docView, boolean needBaseUrl, String baseUrl);

    /**
     * Returns an URL calling {@link DocumentViewCodec#getUrlFromDocumentView(DocumentView) on codec with given name.
     *
     * @param codecName the codec name to use
     * @param docView the original document view from request
     * @param needBaseUrl boolean indicating if base url should be added to the url returned by the codec.
     * @param baseUrl value of the base url.
     */
    String getUrlFromDocumentView(String codecName, DocumentView docView, boolean needBaseUrl, String baseUrl);

}
