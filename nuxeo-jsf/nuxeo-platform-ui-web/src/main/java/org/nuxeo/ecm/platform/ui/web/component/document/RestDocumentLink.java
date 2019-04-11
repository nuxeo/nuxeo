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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: RestDocumentLink.java 25089 2007-09-18 17:41:58Z ogrisel $
 */

package org.nuxeo.ecm.platform.ui.web.component.document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.el.ELException;
import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.component.ContextCallback;
import javax.faces.component.UIComponent;
import javax.faces.component.UIParameter;
import javax.faces.component.html.HtmlOutputLink;
import javax.faces.context.FacesContext;
import javax.faces.event.FacesEvent;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.platform.ui.web.component.VariableManager;
import org.nuxeo.ecm.platform.ui.web.tag.fn.DocumentModelFunctions;
import org.nuxeo.ecm.platform.ui.web.util.BaseURL;

import com.sun.faces.renderkit.html_basic.HtmlBasicRenderer.Param;

/**
 * Component that gives generates a Restful link given a document.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class RestDocumentLink extends HtmlOutputLink {

    public static final String COMPONENT_TYPE = RestDocumentLink.class.getName();

    public static final String COMPONENT_FAMILY = RestDocumentLink.class.getName();

    public static final String DEFAULT_VIEW_ID = "view_documents";

    protected static final Param[] EMPTY_PARAMS = new Param[0];

    protected DocumentModel document;

    /**
     * @since 5.7
     */
    protected String repositoryName;

    protected DocumentRef documentIdRef;

    /**
     * @since 7.4
     */
    protected DocumentRef documentPathRef;

    protected String view;

    protected String tab;

    protected String subTab;

    /**
     * @since 5.4.2
     */
    protected String tabs;

    protected Boolean addTabInfo;

    protected String pattern;

    protected Boolean newConversation;

    /**
     * @since 7.3
     */
    protected String baseURL;

    /**
     * @since 5.7
     */
    protected String var;

    /**
     * @since 5.7
     */
    protected Boolean resolveOnly;

    @Override
    public String getFamily() {
        return COMPONENT_FAMILY;
    }

    /**
     * Override to build the URL thanks to other tag attributes information.
     * <p>
     * The document view service is queried to build it, and the tag attribute named "value" is ignored.
     */
    @Override
    public Object getValue() {
        DocumentModel doc = getDocument();
        DocumentRef documentIdRef = getDocumentIdRef();
        DocumentRef documentPathRef = getDocumentPathRef();
        String repoName = getRepositoryName();
        if (doc == null && repoName == null || (documentIdRef != null || documentPathRef != null) && repoName == null) {
            return null;
        }

        String viewId = getView();

        Map<String, String> params = new LinkedHashMap<>();
        String tabValue = getTab();
        String subTabValue = getSubTab();
        String tabValues = getTabs();
        if (tabValues == null) {
            tabValues = "";
        }
        if (tabValue != null && !tabValue.isEmpty()) {
            if (!tabValues.isEmpty()) {
                tabValues += ",";
            }
            tabValues += ":" + tabValue;
            // params.put("tabId", tabValue); // BBB
            if (subTabValue != null) {
                tabValues += ":" + subTabValue;
                // params.put("subTabId", subTabValue); // BBB
            }
        } else {
            if (Boolean.TRUE.equals(getAddTabInfo())) {
                // reset tab info, resetting the tab will reset the sub tab
                if (!tabValues.isEmpty()) {
                    tabValues += ",";
                }
                tabValues += ":" + WebActions.NULL_TAB_ID;
                // params.put("tabId", WebActions.NULL_TAB_ID); // BBB
            }
        }
        if (!tabValues.isEmpty()) {
            params.put("tabIds", tabValues);
        }

        // add parameters from f:param sub tags
        Param[] paramTags = getParamList();
        for (Param param : paramTags) {
            String pn = param.name;
            if (pn != null && pn.length() != 0) {
                String pv = param.value;
                if (pv != null && pv.length() != 0) {
                    params.put(pn, pv);
                }
            }
        }

        String pattern = getPattern();
        String baseURL = getBaseURL();
        if (StringUtils.isEmpty(baseURL)) {
            baseURL = BaseURL.getBaseURL();
        }

        // new conversation variable handled by renderer
        boolean useNewConversation = true;
        if (doc != null) {
            return DocumentModelFunctions.documentUrl(pattern, doc, viewId, params, useNewConversation, baseURL);
        } else if (documentIdRef != null) {
            DocumentLocation docLoc = new DocumentLocationImpl(repoName, documentIdRef);
            return DocumentModelFunctions.documentUrl(pattern, docLoc, viewId, params, useNewConversation, baseURL);
        } else if (documentPathRef != null) {
            DocumentLocation docLoc = new DocumentLocationImpl(repoName, documentPathRef);
            return DocumentModelFunctions.documentUrl(pattern, docLoc, viewId, params, useNewConversation, baseURL);
        } else {
            return DocumentModelFunctions.repositoryUrl(pattern, repoName, viewId, params, useNewConversation, baseURL);
        }
    }

    protected Param[] getParamList() {
        if (getChildCount() > 0) {
            ArrayList<Param> parameterList = new ArrayList<>();
            for (UIComponent kid : getChildren()) {
                if (kid instanceof UIParameter) {
                    UIParameter uiParam = (UIParameter) kid;
                    Object value = uiParam.getValue();
                    Param param = new Param(uiParam.getName(), (value == null ? null : value.toString()));
                    parameterList.add(param);
                }
            }
            return parameterList.toArray(new Param[parameterList.size()]);
        } else {
            return EMPTY_PARAMS;
        }

    }

    // setters and getters for tag attributes

    public String getPattern() {
        if (pattern != null) {
            return pattern;
        }
        ValueExpression ve = getValueExpression("pattern");
        if (ve != null) {
            try {
                return (String) ve.getValue(getFacesContext().getELContext());
            } catch (ELException e) {
                throw new FacesException(e);
            }
        } else {
            return null;
        }
    }

    public void setPattern(String codec) {
        pattern = codec;
    }

    public DocumentModel getDocument() {
        if (document != null) {
            return document;
        }
        ValueExpression ve = getValueExpression("document");
        if (ve != null) {
            try {
                return (DocumentModel) ve.getValue(getFacesContext().getELContext());
            } catch (ELException e) {
                throw new FacesException(e);
            }
        } else {
            return null;
        }
    }

    public void setDocument(DocumentModel document) {
        this.document = document;
    }

    public String getRepositoryName() {
        if (repositoryName != null) {
            return repositoryName;
        }
        ValueExpression ve = getValueExpression("repositoryName");
        if (ve != null) {
            try {
                return (String) ve.getValue(getFacesContext().getELContext());
            } catch (ELException e) {
                throw new FacesException(e);
            }
        } else {
            return null;
        }
    }

    public void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
    }

    public DocumentRef getDocumentIdRef() {
        if (documentIdRef != null) {
            return documentIdRef;
        }
        ValueExpression ve = getValueExpression("documentId");
        if (ve != null) {
            try {
                String id = (String) ve.getValue(getFacesContext().getELContext());
                if (id != null) {
                    return new IdRef(id);
                } else {
                    return null;
                }
            } catch (ELException e) {
                throw new FacesException(e);
            }
        } else {
            return null;
        }
    }

    public void setDocumentIdRef(DocumentRef documentIdRef) {
        this.documentIdRef = documentIdRef;
    }

    public DocumentRef getDocumentPathRef() {
        if (documentPathRef != null) {
            return documentPathRef;
        }
        ValueExpression ve = getValueExpression("documentPath");
        if (ve != null) {
            try {
                String id = (String) ve.getValue(getFacesContext().getELContext());
                if (id != null) {
                    return new IdRef(id);
                } else {
                    return null;
                }
            } catch (ELException e) {
                throw new FacesException(e);
            }
        } else {
            return null;
        }
    }

    public void setDocumentPathRef(DocumentRef documentPathRef) {
        this.documentPathRef = documentPathRef;
    }

    /**
     * Returns true if URL must link to a page in a new conversation.
     * <p>
     * Defaults to false.
     */
    public Boolean getNewConversation() {
        if (newConversation != null) {
            return newConversation;
        }
        ValueExpression ve = getValueExpression("newConversation");
        if (ve != null) {
            try {
                return Boolean.valueOf(!Boolean.FALSE.equals(ve.getValue(getFacesContext().getELContext())));
            } catch (ELException e) {
                throw new FacesException(e);
            }
        } else {
            // default value
            return Boolean.FALSE;
        }
    }

    public void setNewConversation(Boolean newConversation) {
        this.newConversation = newConversation;
    }

    public String getSubTab() {
        if (subTab != null) {
            return subTab;
        }
        ValueExpression ve = getValueExpression("subTab");
        if (ve != null) {
            try {
                return (String) ve.getValue(getFacesContext().getELContext());
            } catch (ELException e) {
                throw new FacesException(e);
            }
        } else {
            return null;
        }
    }

    public void setSubTab(String subTab) {
        this.subTab = subTab;
    }

    public String getTab() {
        if (tab != null) {
            return tab;
        }
        ValueExpression ve = getValueExpression("tab");
        if (ve != null) {
            try {
                return (String) ve.getValue(getFacesContext().getELContext());
            } catch (ELException e) {
                throw new FacesException(e);
            }
        } else {
            return null;
        }
    }

    public void setTab(String tab) {
        this.tab = tab;
    }

    /**
     * @since 7.3
     */
    public String getBaseURL() {
        if (baseURL != null) {
            return baseURL;
        }
        ValueExpression ve = getValueExpression("baseURL");
        if (ve != null) {
            try {
                return (String) ve.getValue(getFacesContext().getELContext());
            } catch (ELException e) {
                throw new FacesException(e);
            }
        } else {
            return null;
        }
    }

    /**
     * @since 7.3
     */
    public void setBaseURL(String baseURL) {
        this.baseURL = baseURL;
    }

    public String getView() {
        if (view != null) {
            return view;
        }
        ValueExpression ve = getValueExpression("view");
        if (ve != null) {
            try {
                return (String) ve.getValue(getFacesContext().getELContext());
            } catch (ELException e) {
                throw new FacesException(e);
            }
        } else {
            return null;
        }
    }

    public void setView(String view) {
        this.view = view;
    }

    public Boolean getAddTabInfo() {
        if (addTabInfo != null) {
            return addTabInfo;
        }
        ValueExpression ve = getValueExpression("addTabInfo");
        if (ve != null) {
            try {
                return Boolean.valueOf(!Boolean.FALSE.equals(ve.getValue(getFacesContext().getELContext())));
            } catch (ELException e) {
                throw new FacesException(e);
            }
        } else {
            // default value
            return Boolean.TRUE;
        }
    }

    public void setAddTabInfo(Boolean addTabInfo) {
        this.addTabInfo = addTabInfo;
    }

    public String getTabs() {
        if (tabs != null) {
            return tabs;
        }
        ValueExpression ve = getValueExpression("tabs");
        if (ve != null) {
            try {
                return (String) ve.getValue(getFacesContext().getELContext());
            } catch (ELException e) {
                throw new FacesException(e);
            }
        } else {
            return null;
        }
    }

    public void setTabs(String tabs) {
        this.tabs = tabs;
    }

    public Boolean getResolveOnly() {
        if (resolveOnly != null) {
            return resolveOnly;
        }
        ValueExpression ve = getValueExpression("resolveOnly");
        if (ve != null) {
            try {
                return Boolean.valueOf(!Boolean.FALSE.equals(ve.getValue(getFacesContext().getELContext())));
            } catch (ELException e) {
                throw new FacesException(e);
            }
        } else {
            // default value
            return Boolean.FALSE;
        }
    }

    public void setResolveOnly(Boolean resolveOnly) {
        this.resolveOnly = resolveOnly;
    }

    public String getVar() {
        if (var != null) {
            return var;
        }
        ValueExpression ve = getValueExpression("var");
        if (ve != null) {
            try {
                return (String) ve.getValue(getFacesContext().getELContext());
            } catch (ELException e) {
                throw new FacesException(e);
            }
        } else {
            return null;
        }
    }

    public void setVar(String var) {
        this.var = var;
    }

    // "resolveOnly" attribute management: expose value instead of rendering
    // the tag

    /**
     * Saves the current value exposed as param to the request, and put new variable value instead.
     * <p>
     * Returns the original value exposed to the request.
     *
     * @since 5.7
     */
    protected Object beforeRender() {
        String var = getVar();
        Object orig = VariableManager.saveRequestMapVarValue(var);
        if (Boolean.TRUE.equals(getResolveOnly())) {
            VariableManager.putVariableToRequestParam(var, getValue());
        }
        return orig;
    }

    /**
     * Restored the original value exposed as param to the request, and remove current variable value.
     *
     * @since 5.7
     */
    protected void afterRender(Object origVarValue) {
        String var = getVar();
        VariableManager.restoreRequestMapVarValue(var, origVarValue);
    }

    /**
     * @since 5.7
     */
    @Override
    public boolean invokeOnComponent(FacesContext context, String clientId, ContextCallback callback)
            throws FacesException {
        Object varValue = beforeRender();
        try {
            return super.invokeOnComponent(context, clientId, callback);
        } finally {
            afterRender(varValue);
        }
    }

    /**
     * @since 5.7
     */
    @Override
    public void broadcast(FacesEvent event) {
        Object varValue = beforeRender();
        try {
            super.broadcast(event);
        } finally {
            afterRender(varValue);
        }
    }

    /**
     * @since 5.7
     */
    @Override
    public void encodeBegin(FacesContext context) throws IOException {
        if (!Boolean.TRUE.equals(getResolveOnly())) {
            super.encodeBegin(context);
        }
    }

    @Override
    public void encodeChildren(FacesContext context) throws IOException {
        Object varValue = beforeRender();
        try {
            super.encodeChildren(context);
        } finally {
            afterRender(varValue);
        }
    }

    /**
     * @since 5.7
     */
    @Override
    public void encodeEnd(FacesContext context) throws IOException {
        if (!Boolean.TRUE.equals(getResolveOnly())) {
            super.encodeEnd(context);
        }
    }

    // state holder

    @Override
    public Object saveState(FacesContext context) {
        return new Object[] { super.saveState(context), document, documentIdRef, view, tab, subTab, tabs, addTabInfo,
                pattern, newConversation, baseURL, var, resolveOnly };
    }

    @Override
    public void restoreState(FacesContext context, Object state) {
        Object[] values = (Object[]) state;
        super.restoreState(context, values[0]);
        document = (DocumentModel) values[1];
        documentIdRef = (DocumentRef) values[2];
        view = (String) values[3];
        tab = (String) values[4];
        subTab = (String) values[5];
        tabs = (String) values[6];
        addTabInfo = (Boolean) values[7];
        pattern = (String) values[8];
        newConversation = (Boolean) values[9];
        baseURL = (String) values[10];
        var = (String) values[11];
        resolveOnly = (Boolean) values[12];
    }

}
