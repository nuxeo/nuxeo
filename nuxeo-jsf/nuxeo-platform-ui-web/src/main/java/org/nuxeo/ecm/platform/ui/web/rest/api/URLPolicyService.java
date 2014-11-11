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
 * $Id: URLPolicyService.java 28924 2008-01-10 14:04:05Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.rest.api;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants;
import org.nuxeo.ecm.platform.ui.web.rest.descriptors.URLPatternDescriptor;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.api.DocumentViewCodecManager;

/**
 * Service used on the web layer to handle navigation using meaningful URLs.
 * <p>
 * It handles a document context description, and also performs JSF model
 * related operations.
 * <p>
 * It holds pattern descriptors used to interact with the
 * {@link DocumentViewCodecManager}.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public interface URLPolicyService {

    String POST_OUTCOME_REQUEST_KEY = "PostOutcome";

    String DOCUMENT_VIEW_REQUEST_KEY = "DocumentView";

    /**
     * @deprecated: use {@link NXAuthConstants#DISABLE_REDIRECT_REQUEST_KEY}
     *              instead
     */
    @Deprecated
    String DISABLE_REDIRECT_REQUEST_KEY = "nuxeo.disable.redirect.wrapper";

    String FORCE_URL_ENCODING_REQUEST_KEY = "nuxeo.force.url.encoding";

    /**
     * Returns true if request is a GET request and filter preprocessing is
     * turned on.
     */
    boolean isCandidateForDecoding(HttpServletRequest httpRequest);

    /**
     * Returns true if request is a POST request and filter redirection is
     * turned on.
     */
    boolean isCandidateForEncoding(HttpServletRequest httpRequest);

    /**
     * Adds document view to the request for later retrieval.
     *
     * @param request the current request.
     * @param docView to save
     */
    void setDocumentViewInRequest(HttpServletRequest request,
            DocumentView docView);

    /**
     * Builds the document view from request information.
     * <p>
     * Delegates call to a document view codec found thanks to the default URL
     * pattern descriptor.
     */
    DocumentView getDocumentViewFromRequest(HttpServletRequest request);

    /**
     * Builds the document view from request information.
     * <p>
     * Delegates call to a document view codec found thanks given pattern name.
     */
    DocumentView getDocumentViewFromRequest(String pattern,
            HttpServletRequest request);

    /**
     * Returns a URL given a document view.
     * <p>
     * Delegates call to a document view codec found thanks to the default URL
     * pattern descriptor.
     */
    String getUrlFromDocumentView(DocumentView docView, String baseUrl);

    /**
     * Returns a URL given a document view.
     * <p>
     * Delegates call to a document view codec found thanks given pattern name.
     */
    String getUrlFromDocumentView(String pattern, DocumentView docView,
            String baseUrl);

    /**
     * Extracts parameters from request attributes.
     * <p>
     * Apply them to the model using EL value bindings described on URL pattern
     * descriptors.
     * <p>
     * We look for binding values to set on the request attribute and on the
     * document view parameters.
     */
    void applyRequestParameters(FacesContext facesContext);

    /**
     * Appends parameters to request so that the model can be restored after
     * request.
     * <p>
     * Extract them using EL value bindings described on URL pattern
     * descriptors.
     * <p>
     * If the document view is not null, values are set on its parameters. If
     * the document view is null, values are set on the request parameters.
     */
    void appendParametersToRequest(FacesContext facesContext);

    /**
     * Performs redirection action.
     * <p>
     * Extract it using an EL action binding described on URL pattern
     * descriptors.
     * <p>
     * The action binding is called using given document view as parameter. If a
     * sub URI is found, do nothing (may be an invalid resource URL).
     */
    String navigate(FacesContext context);

    // registry of pattern descriptors

    String getDefaultPatternName();

    void addPatternDescriptor(URLPatternDescriptor pattern);

    void removePatternDescriptor(URLPatternDescriptor pattern);

    void clear();

}
