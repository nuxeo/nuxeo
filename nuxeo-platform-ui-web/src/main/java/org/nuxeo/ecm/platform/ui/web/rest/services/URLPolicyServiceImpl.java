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
 * $Id: URLPolicyServiceImpl.java 29556 2008-01-23 00:59:39Z jcarsique $
 */

package org.nuxeo.ecm.platform.ui.web.rest.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.el.ELContext;
import javax.el.ExpressionFactory;
import javax.el.MethodExpression;
import javax.el.ValueExpression;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants;
import org.nuxeo.ecm.platform.ui.web.auth.NuxeoAuthenticationFilter;
import org.nuxeo.ecm.platform.ui.web.rest.api.URLPolicyService;
import org.nuxeo.ecm.platform.ui.web.rest.descriptors.URLPatternDescriptor;
import org.nuxeo.ecm.platform.ui.web.rest.descriptors.ValueBindingDescriptor;
import org.nuxeo.ecm.platform.ui.web.util.BaseURL;
import org.nuxeo.ecm.platform.ui.web.util.ComponentTagUtils;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.api.DocumentViewCodecManager;
import org.nuxeo.runtime.api.Framework;

public class URLPolicyServiceImpl implements URLPolicyService {

    public static final String NAME = URLPolicyServiceImpl.class.getName();

    private static final Log log = LogFactory.getLog(URLPolicyServiceImpl.class);

    protected final Map<String, URLPatternDescriptor> descriptors;

    public URLPolicyServiceImpl() {
        descriptors = new HashMap<String, URLPatternDescriptor>();
    }

    protected List<URLPatternDescriptor> getURLPatternDescriptors() {
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

    public String getDefaultPatternName() {
        URLPatternDescriptor desc = getDefaultPatternDescriptor();
        if (desc != null) {
            return desc.getName();
        }
        return null;
    }

    protected static DocumentViewCodecManager getDocumentViewCodecService() {
        try {
            return Framework.getService(DocumentViewCodecManager.class);
        } catch (Exception e) {
            log.error("Could not retrieve the document view service", e);
        }
        return null;
    }

    protected URLPatternDescriptor getURLPatternDescriptor(String patternName) {
        URLPatternDescriptor desc = descriptors.get(patternName);
        if (desc == null) {
            throw new IllegalArgumentException("Unknow pattern " + patternName);
        }
        return desc;
    }

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

    public boolean isCandidateForEncoding(HttpServletRequest httpRequest) {
        Boolean forceEncoding = false;
        Object forceEncodingValue = httpRequest.getAttribute(FORCE_URL_ENCODING_REQUEST_KEY);
        if (forceEncodingValue instanceof Boolean) {
            forceEncoding = (Boolean) forceEncodingValue;
        }

        // only POST access need a redirect,unless with force encoding (this
        // happens when redirect is triggered after a seam page has been
        // processed)
        if (!forceEncoding && !httpRequest.getMethod().equals("POST")) {
            return false;
        }

        Object skipRedirect = httpRequest.getAttribute(NXAuthConstants.DISABLE_REDIRECT_REQUEST_KEY);
        if (skipRedirect instanceof Boolean && (Boolean) skipRedirect) {
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

    public void setDocumentViewInRequest(HttpServletRequest request,
            DocumentView docView) {
        request.setAttribute(NXAuthConstants.REQUESTED_URL,
                NuxeoAuthenticationFilter.getRequestedUrl(request));
        request.setAttribute(DOCUMENT_VIEW_REQUEST_KEY, docView);
    }

    protected URLPatternDescriptor getURLPatternDescriptor(
            HttpServletRequest request) {
        URLPatternDescriptor res = null;
        for (URLPatternDescriptor desc : getURLPatternDescriptors()) {
            DocumentView docView = getDocumentViewFromRequest(desc.getName(),
                    request);
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

    public DocumentView getDocumentViewFromRequest(String patternName,
            HttpServletRequest request) {
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
        DocumentViewCodecManager docViewService = getDocumentViewCodecService();
        DocumentView docView = docViewService.getDocumentViewFromUrl(codecName,
                url, desc.getNeedBaseURL(), BaseURL.getLocalBaseURL(request));
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

    public String getUrlFromDocumentView(DocumentView docView, String baseUrl) {
        String url = null;
        for (URLPatternDescriptor desc : getURLPatternDescriptors()) {
            url = getUrlFromDocumentView(desc.getName(), docView, baseUrl);
            if (url != null) {
                break;
            }
        }
        // if (url == null && log.isDebugEnabled()) {
        // log.debug("Could not get url from document view");
        // }
        return url;
    }

    public String getUrlFromDocumentView(String patternName,
            DocumentView docView, String baseUrl) {
        DocumentViewCodecManager docViewService = getDocumentViewCodecService();
        URLPatternDescriptor desc = getURLPatternDescriptor(patternName);
        String codecName = desc.getDocumentViewCodecName();
        return docViewService.getUrlFromDocumentView(codecName, docView,
                desc.getNeedBaseURL(), baseUrl);
    }

    public void applyRequestParameters(FacesContext facesContext) {
        // try to set document view
        ExpressionFactory ef = facesContext.getApplication().getExpressionFactory();
        ELContext context = facesContext.getELContext();

        HttpServletRequest httpRequest = (HttpServletRequest) facesContext.getExternalContext().getRequest();

        URLPatternDescriptor pattern = getURLPatternDescriptor(httpRequest);
        if (pattern == null) {
            return;
        }

        DocumentView docView = getDocumentViewFromRequest(pattern.getName(),
                httpRequest);
        // pattern applies => document view will not be null
        if (docView != null) {
            String documentViewBinding = pattern.getDocumentViewBinding();
            if (documentViewBinding != null && !"".equals(documentViewBinding)) {
                // try to set it from custom mapping
                ValueExpression ve = ef.createValueExpression(context,
                        pattern.getDocumentViewBinding(), Object.class);
                ve.setValue(context, docView);
            }
        }

        Map<String, String> docViewParameters = null;
        if (docView != null) {
            docViewParameters = docView.getParameters();
        }
        ValueBindingDescriptor[] bindings = pattern.getValueBindings();
        if (bindings != null) {
            for (ValueBindingDescriptor binding : bindings) {
                String paramName = binding.getName();
                // try doc view parameters
                Object value = null;
                if (docViewParameters != null
                        && docViewParameters.containsKey(paramName)) {
                    value = docView.getParameter(paramName);
                } else {
                    // try request attributes
                    value = httpRequest.getAttribute(paramName);
                }
                String expr = binding.getExpression();
                if (ComponentTagUtils.isValueReference(expr)) {
                    ValueExpression ve = ef.createValueExpression(context,
                            expr, Object.class);
                    try {
                        ve.setValue(context, value);
                    } catch (Exception e) {
                        log.error(String.format(
                                "Could not apply request parameter %s "
                                        + "to expression %s", value, expr));
                    }
                }
            }
        }
    }

    public void appendParametersToRequest(FacesContext facesContext) {
        appendParametersToRequest(facesContext, null);
    }

    public void appendParametersToRequest(FacesContext facesContext,
            String pattern) {
        // try to get doc view from custom mapping
        DocumentView docView = null;
        ExpressionFactory ef = facesContext.getApplication().getExpressionFactory();
        ELContext context = facesContext.getELContext();
        HttpServletRequest httpRequest = (HttpServletRequest) facesContext.getExternalContext().getRequest();

        // get existing document view from given pattern, else create it
        URLPatternDescriptor patternDesc;
        if (pattern != null && !"".equals(pattern)) {
            patternDesc = getURLPatternDescriptor(pattern);
        } else {
            patternDesc = getDefaultPatternDescriptor();
        }
        if (patternDesc != null) {
            // resolved doc view values thanks to bindings
            Object docViewValue = null;
            String documentViewBinding = patternDesc.getDocumentViewBinding();
            if (documentViewBinding != null && !"".equals(documentViewBinding)) {
                ValueExpression ve = ef.createValueExpression(context,
                        documentViewBinding, Object.class);
                docViewValue = ve.getValue(context);
                if (docViewValue == null) {
                    documentViewBinding = patternDesc.getNewDocumentViewBinding();
                    if (documentViewBinding != null
                            && !"".equals(documentViewBinding)) {
                        ve = ef.createValueExpression(context,
                                documentViewBinding, Object.class);
                        docViewValue = ve.getValue(context);
                    }
                }
            }
            if (docViewValue instanceof DocumentView) {
                docView = (DocumentView) docViewValue;
                // set pattern name in case it was just created
                docView.setPatternName(patternDesc.getName());
                ValueBindingDescriptor[] bindings = patternDesc.getValueBindings();
                if (bindings != null) {
                    for (ValueBindingDescriptor binding : bindings) {
                        String paramName = binding.getName();
                        String expr = binding.getExpression();
                        try {
                            Object value;
                            if (ComponentTagUtils.isValueReference(expr)) {
                                ValueExpression ve = ef.createValueExpression(
                                        context, expr, Object.class);
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
                        } catch (Exception e) {
                            log.error(String.format(
                                    "Could not get parameter %s from expression %s",
                                    paramName, expr));
                        }
                    }
                }
            }
        }

        // save document view to the request
        setDocumentViewInRequest(httpRequest, docView);
    }

    public String navigate(FacesContext facesContext) {
        HttpServletRequest httpRequest = (HttpServletRequest) facesContext.getExternalContext().getRequest();

        URLPatternDescriptor pattern = getURLPatternDescriptor(httpRequest);
        if (pattern == null) {
            return null;
        }

        DocumentView docView = getDocumentViewFromRequest(pattern.getName(),
                httpRequest);
        ExpressionFactory ef = facesContext.getApplication().getExpressionFactory();
        ELContext context = facesContext.getELContext();
        String actionBinding = pattern.getActionBinding();
        if (actionBinding != null && !"".equals(actionBinding)) {
            MethodExpression action = ef.createMethodExpression(context,
                    actionBinding, String.class,
                    new Class[] { DocumentView.class });
            return (String) action.invoke(context, new Object[] { docView });
        }
        return null;
    }

    // registries management

    public void addPatternDescriptor(URLPatternDescriptor pattern) {
        String name = pattern.getName();
        if (descriptors.containsKey(name)) {
            // no merging right now
            descriptors.remove(name);
        }
        descriptors.put(pattern.getName(), pattern);
        log.debug("Added URLPatternDescriptor: " + name);
    }

    public void removePatternDescriptor(URLPatternDescriptor pattern) {
        String name = pattern.getName();
        descriptors.remove(name);
        log.debug("Removed URLPatternDescriptor: " + name);
    }

    public void clear() {
        descriptors.clear();
    }

}
