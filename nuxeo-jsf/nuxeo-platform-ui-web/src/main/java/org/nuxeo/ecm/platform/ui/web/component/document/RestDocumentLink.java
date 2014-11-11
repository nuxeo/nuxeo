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

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.platform.ui.web.component.VariableManager;
import org.nuxeo.ecm.platform.ui.web.tag.fn.DocumentModelFunctions;

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
     * The document view service is queried to build it, and the tag attribute
     * named "value" is ignored.
     */
    @Override
    public Object getValue() {

        DocumentModel doc = getDocument();
        String repoName = getRepositoryName();
        if (doc == null && repoName == null) {
            return null;
        }

        String viewId = getView();

        Map<String, String> params = new LinkedHashMap<String, String>();
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
        Boolean nc = getNewConversation();

        return doc != null ? DocumentModelFunctions.documentUrl(pattern, doc,
                viewId, params, nc != null ? nc.booleanValue() : false)
                : DocumentModelFunctions.repositoryUrl(pattern, repoName,
                        viewId, params, nc != null ? nc.booleanValue() : false);
    }

    protected Param[] getParamList() {
        if (getChildCount() > 0) {
            ArrayList<Param> parameterList = new ArrayList<Param>();
            for (UIComponent kid : getChildren()) {
                if (kid instanceof UIParameter) {
                    UIParameter uiParam = (UIParameter) kid;
                    Object value = uiParam.getValue();
                    Param param = new Param(uiParam.getName(),
                            (value == null ? null : value.toString()));
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

    // XXX AT: useless right now
    public DocumentRef getDocumentIdRef() {
        if (documentIdRef != null) {
            return documentIdRef;
        }
        ValueExpression ve = getValueExpression("documentIdRef");
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
     * Saves the current value exposed as param to the request, and put new
     * variable value instead.
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
     * Restored the original value exposed as param to the request, and remove
     * current variable value.
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
    public boolean invokeOnComponent(FacesContext context, String clientId,
            ContextCallback callback) throws FacesException {
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
        return new Object[] { super.saveState(context), document,
                documentIdRef, view, tab, subTab, tabs, addTabInfo, pattern,
                newConversation, var, resolveOnly };
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
        var = (String) values[10];
        resolveOnly = (Boolean) values[11];
    }

}
