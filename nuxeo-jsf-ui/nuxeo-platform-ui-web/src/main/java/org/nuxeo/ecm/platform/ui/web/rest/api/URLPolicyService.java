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
 * $Id: URLPolicyService.java 28924 2008-01-10 14:04:05Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.rest.api;

import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants;
import org.nuxeo.ecm.platform.ui.web.rest.StaticNavigationHandler;
import org.nuxeo.ecm.platform.ui.web.rest.descriptors.URLPatternDescriptor;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.api.DocumentViewCodecManager;

/**
 * Service used on the web layer to handle navigation using meaningful URLs.
 * <p>
 * It handles a document context description, and also performs JSF model related operations.
 * <p>
 * It holds pattern descriptors used to interact with the {@link DocumentViewCodecManager}.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public interface URLPolicyService {

    String POST_OUTCOME_REQUEST_KEY = "PostOutcome";

    String DOCUMENT_VIEW_REQUEST_KEY = "DocumentView";

    String DISABLE_ACTION_BINDING_KEY = "DisableActionBinding";

    /**
     * @deprecated: use {@link NXAuthConstants#DISABLE_REDIRECT_REQUEST_KEY} instead
     */
    @Deprecated
    String DISABLE_REDIRECT_REQUEST_KEY = "nuxeo.disable.redirect.wrapper";

    String FORCE_URL_ENCODING_REQUEST_KEY = "nuxeo.force.url.encoding";

    /**
     * Returns true if request is a GET request and filter preprocessing is turned on.
     */
    boolean isCandidateForDecoding(HttpServletRequest httpRequest);

    /**
     * Returns true if request is a POST request and filter redirection is turned on.
     */
    boolean isCandidateForEncoding(HttpServletRequest httpRequest);

    /**
     * Adds document view to the request for later retrieval.
     *
     * @param request the current request.
     * @param docView to save
     */
    void setDocumentViewInRequest(HttpServletRequest request, DocumentView docView);

    /**
     * Builds the document view from request information.
     * <p>
     * Delegates call to a document view codec found thanks to the default URL pattern descriptor.
     */
    DocumentView getDocumentViewFromRequest(HttpServletRequest request);

    /**
     * Builds the document view from request information.
     * <p>
     * Delegates call to a document view codec found thanks given pattern name.
     */
    DocumentView getDocumentViewFromRequest(String pattern, HttpServletRequest request);

    /**
     * Returns a URL given a document view.
     * <p>
     * Delegates call to a document view codec found thanks to the default URL pattern descriptor.
     */
    String getUrlFromDocumentView(DocumentView docView, String baseUrl);

    /**
     * Returns a URL given a document view.
     * <p>
     * Delegates call to a document view codec found thanks given pattern name.
     */
    String getUrlFromDocumentView(String pattern, DocumentView docView, String baseUrl);

    /**
     * Extracts parameters from request attributes.
     * <p>
     * Apply them to the model using EL value bindings described on URL pattern descriptors.
     * <p>
     * We look for binding values to set on the request attribute and on the document view parameters.
     */
    void applyRequestParameters(FacesContext facesContext);

    /**
     * Appends parameters to request so that the model can be restored after request.
     * <p>
     * Extract them using EL value bindings described on URL pattern descriptors.
     * <p>
     * If the document view is not null, values are set on its parameters. If the document view is null, values are set
     * on the request parameters.
     */
    void appendParametersToRequest(FacesContext facesContext);

    /**
     * Performs redirection action.
     * <p>
     * Extract it using an EL action binding described on URL pattern descriptors.
     * <p>
     * The action binding is called using given document view as parameter. If a sub URI is found, do nothing (may be an
     * invalid resource URL).
     */
    String navigate(FacesContext context);

    // registry of pattern descriptors

    /**
     * Returns the default pattern descriptor name
     */
    String getDefaultPatternName();

    /**
     * Returns true if the service holds a pattern descriptor with given name
     *
     * @since 5.5
     */
    boolean hasPattern(String name);

    void addPatternDescriptor(URLPatternDescriptor pattern);

    void removePatternDescriptor(URLPatternDescriptor pattern);

    void clear();

    /**
     * Initializes the view id manager {@link StaticNavigationHandler} using the given servlet context.
     *
     * @since 5.5
     * @since 6.0, passes the request and response too
     */
    void initViewIdManager(ServletContext context, HttpServletRequest request, HttpServletResponse response);

    /**
     * Returns the view id given an outcome, to dispatch to the right view given an outcome.
     * <p>
     * For instance, will return "/view_documents.xhtml" given "view_documents".
     *
     * @since 5.5
     */
    String getViewIdFromOutcome(String outcome, HttpServletRequest httpRequest);

    /**
     * Returns an outcome given a view id, to fill a document view when parsing a standard JSF URL.
     * <p>
     * For instance, will return "view_documents" given "/view_documents.xhtml" or "/view_documents.faces".
     *
     * @since 5.5
     */
    String getOutcomeFromViewId(String viewId, HttpServletRequest httpRequest);

    /**
     * Returns an outcome given a url, to fill a document view when parsing a standard JSF URL.
     * <p>
     * It parses the given url to extract the outcome, and then calls
     * {@link #getOutcomeFromViewId(String, HttpServletRequest)}
     *
     * @since 5.5
     */
    String getOutcomeFromUrl(String url, HttpServletRequest httpRequest);

    /**
     * Flushes the URLPolicyService cache, to be called when hot reload is performed for instance.
     *
     * @since 5.5
     */
    void flushCache();

}
