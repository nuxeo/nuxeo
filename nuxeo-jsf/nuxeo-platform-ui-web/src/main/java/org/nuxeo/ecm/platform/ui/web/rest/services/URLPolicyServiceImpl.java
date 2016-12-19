/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id: URLPolicyServiceImpl.java 29556 2008-01-23 00:59:39Z jcarsique $
 */

package org.nuxeo.ecm.platform.ui.web.rest.services;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ExpressionFactory;
import javax.el.MethodExpression;
import javax.el.ValueExpression;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.io.download.DownloadService;
import org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants;
import org.nuxeo.ecm.platform.ui.web.auth.NuxeoAuthenticationFilter;
import org.nuxeo.ecm.platform.ui.web.rest.StaticNavigationHandler;
import org.nuxeo.ecm.platform.ui.web.rest.api.URLPolicyService;
import org.nuxeo.ecm.platform.ui.web.rest.descriptors.URLPatternDescriptor;
import org.nuxeo.ecm.platform.ui.web.rest.descriptors.ValueBindingDescriptor;
import org.nuxeo.ecm.platform.ui.web.util.BaseURL;
import org.nuxeo.ecm.platform.ui.web.util.ComponentTagUtils;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.api.DocumentViewCodecManager;
import org.nuxeo.ecm.platform.url.codec.DocumentFileCodec;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.runtime.api.Framework;

public class URLPolicyServiceImpl implements URLPolicyService {

    public static final String NAME = URLPolicyServiceImpl.class.getName();

    private static final Log log = LogFactory.getLog(URLPolicyServiceImpl.class);

    // this used to be a codec but now delegates to DownloadService
    public static final String DOWNLOADFILE_PATTERN = "downloadFile";

    protected final Map<String, URLPatternDescriptor> descriptors;

    protected StaticNavigationHandler viewIdManager;

    public URLPolicyServiceImpl() {
        // make sure the descriptors list order follows registration order, as
        // order may have an impact on resolved pattern
        descriptors = new LinkedHashMap<String, URLPatternDescriptor>();
    }

    protected List<URLPatternDescriptor> getURLPatternDescriptors() {
        // TODO: add cache
        List<URLPatternDescriptor> lst = new ArrayList<URLPatternDescriptor>();
        for (URLPatternDescriptor desc : descriptors.values()) {
            if (desc.getEnabled()) {
                // add default at first
                if (desc.getDefaultURLPolicy()) {
                    lst.add(0, desc);
                } else {
                    lst.add(desc);
                }
            }
        }
        return lst;
    }

    protected URLPatternDescriptor getDefaultPatternDescriptor() {
        for (URLPatternDescriptor desc : descriptors.values()) {
            if (desc.getEnabled()) {
                if (desc.getDefaultURLPolicy()) {
                    return desc;
                }
            }
        }
        return null;
    }

    @Override
    public String getDefaultPatternName() {
        URLPatternDescriptor desc = getDefaultPatternDescriptor();
        if (desc != null) {
            return desc.getName();
        }
        return null;
    }

    @Override
    public boolean hasPattern(String name) {
        URLPatternDescriptor desc = descriptors.get(name);
        return desc != null;
    }

    protected static DocumentViewCodecManager getDocumentViewCodecService() {
        return Framework.getService(DocumentViewCodecManager.class);
    }

    protected URLPatternDescriptor getURLPatternDescriptor(String patternName) {
        URLPatternDescriptor desc = descriptors.get(patternName);
        if (desc == null) {
            throw new IllegalArgumentException("Unknown pattern " + patternName);
        }
        return desc;
    }

    @Override
    public boolean isCandidateForDecoding(HttpServletRequest httpRequest) {
        // only rewrite GET/HEAD URLs
        String method = httpRequest.getMethod();
        if (!method.equals("GET") && !method.equals("HEAD")) {
            return false;
        }

        // look for appropriate pattern and see if it needs filter
        // preprocessing
        URLPatternDescriptor desc = getURLPatternDescriptor(httpRequest);
        if (desc != null) {
            return desc.getNeedFilterPreprocessing();
        }
        // return default pattern descriptor behaviour
        URLPatternDescriptor defaultPattern = getDefaultPatternDescriptor();
        if (defaultPattern != null) {
            return defaultPattern.getNeedFilterPreprocessing();
        }
        return false;
    }

    @Override
    public boolean isCandidateForEncoding(HttpServletRequest httpRequest) {
        Boolean forceEncoding = Boolean.FALSE;
        Object forceEncodingValue = httpRequest.getAttribute(FORCE_URL_ENCODING_REQUEST_KEY);
        if (forceEncodingValue instanceof Boolean) {
            forceEncoding = (Boolean) forceEncodingValue;
        }

        // only POST access need a redirect,unless with force encoding (this
        // happens when redirect is triggered after a seam page has been
        // processed)
        if (!forceEncoding.booleanValue() && !httpRequest.getMethod().equals("POST")) {
            return false;
        }

        Object skipRedirect = httpRequest.getAttribute(NXAuthConstants.DISABLE_REDIRECT_REQUEST_KEY);
        if (skipRedirect instanceof Boolean && ((Boolean) skipRedirect).booleanValue()) {
            return false;
        }

        // look for appropriate pattern and see if it needs redirect
        URLPatternDescriptor desc = getURLPatternDescriptor(httpRequest);
        if (desc != null) {
            return desc.getNeedRedirectFilter();
        }
        // return default pattern descriptor behaviour
        URLPatternDescriptor defaultPattern = getDefaultPatternDescriptor();
        if (defaultPattern != null) {
            return defaultPattern.getNeedRedirectFilter();
        }
        return false;
    }

    @Override
    public void setDocumentViewInRequest(HttpServletRequest request, DocumentView docView) {
        request.setAttribute(NXAuthConstants.REQUESTED_URL, NuxeoAuthenticationFilter.getRequestedUrl(request));
        request.setAttribute(DOCUMENT_VIEW_REQUEST_KEY, docView);
    }

    protected URLPatternDescriptor getURLPatternDescriptor(HttpServletRequest request) {
        URLPatternDescriptor res = null;
        for (URLPatternDescriptor desc : getURLPatternDescriptors()) {
            DocumentView docView = getDocumentViewFromRequest(desc.getName(), request);
            if (docView != null) {
                res = desc;
                break;
            }
        }
        // if (res == null && log.isDebugEnabled()) {
        // log.debug("Could not get url pattern for request "
        // + request.getRequestURL());
        // }
        return res;
    }

    @Override
    public DocumentView getDocumentViewFromRequest(HttpServletRequest request) {
        DocumentView docView = null;
        for (URLPatternDescriptor desc : getURLPatternDescriptors()) {
            docView = getDocumentViewFromRequest(desc.getName(), request);
            if (docView != null) {
                break;
            }
        }

        // if (docView == null && log.isDebugEnabled()) {
        // log.debug("Could not get document view from request "
        // + request.getRequestURL());
        // }
        return docView;
    }

    @Override
    public DocumentView getDocumentViewFromRequest(String patternName, HttpServletRequest request) {
        Object value = request.getAttribute(DOCUMENT_VIEW_REQUEST_KEY);
        if (value instanceof DocumentView) {
            DocumentView requestDocView = (DocumentView) value;
            // check if document view in request was set thanks to this pattern
            if (patternName.equals(requestDocView.getPatternName())) {
                return requestDocView;
            }
        }

        // try to build it from the request
        String url;
        String queryString = request.getQueryString();
        if (queryString != null) {
            url = new String(request.getRequestURL() + "?" + queryString);
        } else {
            url = new String(request.getRequestURL());
        }
        URLPatternDescriptor desc = getURLPatternDescriptor(patternName);
        String codecName = desc.getDocumentViewCodecName();
        DocumentView docView = null;
        DocumentViewCodecManager docViewService = getDocumentViewCodecService();
        if (docViewService != null) {
            docView = docViewService.getDocumentViewFromUrl(codecName, url, desc.getNeedBaseURL(),
                    BaseURL.getLocalBaseURL(request));
        }
        if (docView != null) {
            // set pattern name
            docView.setPatternName(patternName);
            // set other parameters as set in the url pattern if docView does
            // not hold them already
            Map<String, String> docViewParameters = docView.getParameters();
            Map<String, String> requestParameters = URIUtils.getRequestParameters(queryString);
            if (requestParameters != null) {
                ValueBindingDescriptor[] bindings = desc.getValueBindings();
                for (ValueBindingDescriptor binding : bindings) {
                    String paramName = binding.getName();
                    if (!docViewParameters.containsKey(paramName)) {
                        Object paramValue = requestParameters.get(paramName);
                        if (paramValue == null || paramValue instanceof String) {
                            docView.addParameter(paramName, (String) paramValue);
                        }
                    }
                }
            }
        }

        return docView;
    }

    protected URLPatternDescriptor getURLPatternDescriptor(DocumentView docView) {
        URLPatternDescriptor res = null;
        if (docView != null) {
            String patternName = docView.getPatternName();
            try {
                res = getURLPatternDescriptor(patternName);
            } catch (IllegalArgumentException e) {
            }
        }
        // if (res == null && log.isDebugEnabled()) {
        // log.debug("Could not get url pattern for document view");
        // }
        return res;
    }

    @Override
    public String getUrlFromDocumentView(DocumentView docView, String baseUrl) {
        String url = null;
        String patternName = docView.getPatternName();
        if (patternName != null) {
            // try with original document view pattern
            URLPatternDescriptor desc = getURLPatternDescriptor(patternName);
            if (desc != null) {
                // return corresponding url
                url = getUrlFromDocumentView(desc.getName(), docView, baseUrl);
            }
        }
        if (url == null) {
            // take first matching pattern
            List<URLPatternDescriptor> descs = getURLPatternDescriptors();
            for (URLPatternDescriptor desc : descs) {
                url = getUrlFromDocumentView(desc.getName(), docView, baseUrl);
                if (url != null) {
                    break;
                }
            }
        }
        // if (url == null && log.isDebugEnabled()) {
        // log.debug("Could not get url from document view");
        // }
        return url;
    }

    @Override
    public String getUrlFromDocumentView(String patternName, DocumentView docView, String baseUrl) {
        if (DOWNLOADFILE_PATTERN.equals(patternName)) {
            // this used to be a codec but now delegates to DownloadService
            DownloadService downloadService = Framework.getService(DownloadService.class);
            DocumentLocation docLoc = docView.getDocumentLocation();
            String repositoryName = docLoc.getServerName();
            String docId = docLoc.getDocRef().toString();
            String xpath = docView.getParameter(DocumentFileCodec.FILE_PROPERTY_PATH_KEY);
            String filename = docView.getParameter(DocumentFileCodec.FILENAME_KEY);
            String url = downloadService.getDownloadUrl(repositoryName, docId, xpath, filename);
            if (!StringUtils.isBlank(baseUrl)) {
                if (!baseUrl.endsWith("/")) {
                    baseUrl += "/";
                }
                url = baseUrl + url;
            }
            return url;
        }
        DocumentViewCodecManager docViewService = getDocumentViewCodecService();
        URLPatternDescriptor desc = getURLPatternDescriptor(patternName);
        String codecName = desc.getDocumentViewCodecName();
        return docViewService.getUrlFromDocumentView(codecName, docView, desc.getNeedBaseURL(), baseUrl);
    }

    @Override
    public void applyRequestParameters(FacesContext facesContext) {
        // try to set document view
        ExpressionFactory ef = facesContext.getApplication().getExpressionFactory();
        ELContext context = facesContext.getELContext();

        HttpServletRequest httpRequest = (HttpServletRequest) facesContext.getExternalContext().getRequest();

        URLPatternDescriptor pattern = getURLPatternDescriptor(httpRequest);
        if (pattern == null) {
            return;
        }

        DocumentView docView = getDocumentViewFromRequest(pattern.getName(), httpRequest);
        // pattern applies => document view will not be null
        if (docView != null) {
            String documentViewBinding = pattern.getDocumentViewBinding();
            if (documentViewBinding != null && !"".equals(documentViewBinding)) {
                // try to set it from custom mapping
                ValueExpression ve = ef.createValueExpression(context, pattern.getDocumentViewBinding(), Object.class);
                ve.setValue(context, docView);
            }
        }

        Map<String, String> docViewParameters = null;
        if (docView != null) {
            docViewParameters = docView.getParameters();
        }
        ValueBindingDescriptor[] bindings = pattern.getValueBindings();
        if (bindings != null && httpRequest.getAttribute(URLPolicyService.DISABLE_ACTION_BINDING_KEY) == null) {
            for (ValueBindingDescriptor binding : bindings) {
                if (!binding.getCallSetter()) {
                    continue;
                }
                String paramName = binding.getName();
                // try doc view parameters
                Object value = null;
                if (docViewParameters != null && docViewParameters.containsKey(paramName)) {
                    value = docView.getParameter(paramName);
                } else {
                    // try request attributes
                    value = httpRequest.getAttribute(paramName);
                }
                String expr = binding.getExpression();
                if (ComponentTagUtils.isValueReference(expr)) {
                    ValueExpression ve = ef.createValueExpression(context, expr, Object.class);
                    try {
                        ve.setValue(context, value);
                    } catch (ELException e) {
                        log.error("Could not apply request parameter '" + value + "' to expression '" + expr + "'", e);
                    }
                }
            }
        }
    }

    @Override
    public void appendParametersToRequest(FacesContext facesContext) {
        appendParametersToRequest(facesContext, null);
    }

    public void appendParametersToRequest(FacesContext facesContext, String pattern) {
        // try to get doc view from custom mapping
        DocumentView docView = null;
        ExpressionFactory ef = facesContext.getApplication().getExpressionFactory();
        ELContext context = facesContext.getELContext();
        HttpServletRequest httpRequest = (HttpServletRequest) facesContext.getExternalContext().getRequest();

        // get existing document view from given pattern, else create it
        URLPatternDescriptor patternDesc = null;
        if (pattern != null && !"".equals(pattern)) {
            patternDesc = getURLPatternDescriptor(pattern);
        } else {
            // iterate over pattern descriptors, and take the first one that
            // applies, or use the default one
            List<URLPatternDescriptor> descs = getURLPatternDescriptors();
            boolean applies = false;
            for (URLPatternDescriptor desc : descs) {
                String documentViewAppliesExpr = desc.getDocumentViewBindingApplies();
                if (!StringUtils.isBlank(documentViewAppliesExpr)) {
                    // TODO: maybe put view id to the request to help writing
                    // the EL expression
                    ValueExpression ve = ef.createValueExpression(context, documentViewAppliesExpr, Object.class);
                    try {
                        Object res = ve.getValue(context);
                        if (Boolean.TRUE.equals(res)) {
                            applies = true;
                        }
                    } catch (ELException e) {
                        if (log.isDebugEnabled()) {
                            log.debug(String.format("Error executing expression '%s' for " + "url pattern '%s': %s",
                                    documentViewAppliesExpr, desc.getName(), e.getMessage()));
                        }
                    }
                }
                if (applies) {
                    patternDesc = desc;
                    break;
                }
            }
            if (patternDesc == null) {
                // default on the default pattern desc
                patternDesc = getDefaultPatternDescriptor();
            }
        }
        if (patternDesc != null) {
            // resolved doc view values thanks to bindings
            Object docViewValue = null;
            String documentViewBinding = patternDesc.getDocumentViewBinding();
            if (!StringUtils.isBlank(documentViewBinding)) {
                ValueExpression ve = ef.createValueExpression(context, documentViewBinding, Object.class);
                docViewValue = ve.getValue(context);
            }
            if (docViewValue == null) {
                documentViewBinding = patternDesc.getNewDocumentViewBinding();
                if (!StringUtils.isBlank(documentViewBinding)) {
                    ValueExpression ve = ef.createValueExpression(context, documentViewBinding, Object.class);
                    docViewValue = ve.getValue(context);
                }
            }
            if (docViewValue instanceof DocumentView) {
                docView = (DocumentView) docViewValue;
                // set pattern name in case it was just created
                docView.setPatternName(patternDesc.getName());
                ValueBindingDescriptor[] bindings = patternDesc.getValueBindings();
                if (bindings != null) {
                    for (ValueBindingDescriptor binding : bindings) {
                        if (!binding.getCallGetter()) {
                            continue;
                        }
                        String paramName = binding.getName();
                        String expr = binding.getExpression();
                        try {
                            Object value;
                            if (ComponentTagUtils.isValueReference(expr)) {
                                ValueExpression ve = ef.createValueExpression(context, expr, Object.class);
                                value = ve.getValue(context);
                            } else {
                                value = expr;
                            }
                            if (docView != null) {
                                // do not set attributes on the request as
                                // document view will be put in the request
                                // anyway
                                docView.addParameter(paramName, (String) value);
                            } else {
                                httpRequest.setAttribute(paramName, value);
                            }
                        } catch (ELException e) {
                            log.error(String.format("Could not get parameter %s from expression %s", paramName, expr),
                                    e);
                        }
                    }
                }
            }
        }

        // save document view to the request
        setDocumentViewInRequest(httpRequest, docView);
    }

    @Override
    public String navigate(FacesContext facesContext) {
        HttpServletRequest httpRequest = (HttpServletRequest) facesContext.getExternalContext().getRequest();

        URLPatternDescriptor pattern = getURLPatternDescriptor(httpRequest);
        if (pattern == null) {
            return null;
        }

        DocumentView docView = getDocumentViewFromRequest(pattern.getName(), httpRequest);
        ExpressionFactory ef = facesContext.getApplication().getExpressionFactory();
        ELContext context = facesContext.getELContext();
        String actionBinding = pattern.getActionBinding();

        if (actionBinding != null && !"".equals(actionBinding)
                && httpRequest.getAttribute(URLPolicyService.DISABLE_ACTION_BINDING_KEY) == null) {
            MethodExpression action = ef.createMethodExpression(context, actionBinding, String.class,
                    new Class[] { DocumentView.class });
            return (String) action.invoke(context, new Object[] { docView });
        }
        return null;
    }

    // registries management

    @Override
    public void addPatternDescriptor(URLPatternDescriptor pattern) {
        String name = pattern.getName();
        if (descriptors.containsKey(name)) {
            // no merging right now
            descriptors.remove(name);
        }
        descriptors.put(pattern.getName(), pattern);
        log.debug("Added URLPatternDescriptor: " + name);
    }

    @Override
    public void removePatternDescriptor(URLPatternDescriptor pattern) {
        String name = pattern.getName();
        descriptors.remove(name);
        log.debug("Removed URLPatternDescriptor: " + name);
    }

    @Override
    public void initViewIdManager(ServletContext context, HttpServletRequest request, HttpServletResponse response) {
        if (viewIdManager == null) {
            viewIdManager = new StaticNavigationHandler(context, request, response);
        }
    }

    StaticNavigationHandler getViewIdManager() {
        if (viewIdManager == null) {
            throw new RuntimeException("View id manager is not initialized: "
                    + "URLPolicyService#initViewIdManager should " + "have been called first");
        }
        return viewIdManager;
    }

    @Override
    public String getOutcomeFromViewId(String viewId, HttpServletRequest httpRequest) {
        return getViewIdManager().getOutcomeFromViewId(viewId);
    }

    @Override
    public String getOutcomeFromUrl(String url, HttpServletRequest request) {
        String baseUrl = BaseURL.getBaseURL(request);
        // parse url to get outcome from view id
        String viewId = url;
        String webAppName = "/" + VirtualHostHelper.getWebAppName(request);
        if (viewId.startsWith(baseUrl)) {
            // url is absolute
            viewId = '/' + viewId.substring(baseUrl.length());
        } else if (viewId.startsWith(webAppName)) {
            // url is relative to the web app
            viewId = viewId.substring(webAppName.length());
        }
        int index = viewId.indexOf('?');
        if (index != -1) {
            viewId = viewId.substring(0, index);
        }
        return getOutcomeFromViewId(viewId, request);
    }

    @Override
    public String getViewIdFromOutcome(String outcome, HttpServletRequest httpRequest) {
        return getViewIdManager().getViewIdFromOutcome(outcome);
    }

    @Override
    public void clear() {
        descriptors.clear();
    }

    @Override
    public void flushCache() {
        viewIdManager = null;
    }

}
