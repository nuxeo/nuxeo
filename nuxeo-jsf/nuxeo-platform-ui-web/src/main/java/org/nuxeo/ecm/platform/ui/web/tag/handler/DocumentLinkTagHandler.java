/*
 * (C) Copyright 2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id: DocumentLinkTagHandler.java 28610 2008-01-09 17:13:52Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.tag.handler;

import javax.el.ELContext;
import javax.el.ExpressionFactory;
import javax.el.MethodExpression;
import javax.faces.application.Application;
import javax.faces.component.ActionSource2;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.view.facelets.ComponentConfig;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.MetaRuleset;
import javax.faces.view.facelets.TagAttribute;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Component tag handler that wires a document link tag to a command link tag.
 * <p>
 * Useful when redirecting to a document using a post.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class DocumentLinkTagHandler extends GenericHtmlComponentHandler {

    private static final Log log = LogFactory.getLog(DocumentLinkTagHandler.class);

    private final TagAttribute document;

    private final TagAttribute view;

    public DocumentLinkTagHandler(ComponentConfig config) {
        super(config);
        document = getRequiredAttribute("document");
        view = getAttribute("view");
    }

    @Override
    @SuppressWarnings("rawtypes")
    protected MetaRuleset createMetaRuleset(Class type) {
        // expected created tag is an html command link
        MetaRuleset mr = super.createMetaRuleset(type);
        // alias title
        mr.alias("title", "value");
        mr.ignore("document");
        mr.ignore("action");
        mr.ignore("view");
        return mr;
    }

    /**
     * Sets action after component has been created.
     */
    @Override
    public void onComponentCreated(FaceletContext ctx, UIComponent c, UIComponent parent) {
        if (c instanceof ActionSource2) {
            ActionSource2 command = (ActionSource2) c;
            String docValue = getDocumentValue();
            String viewId = getViewValue();
            String actionValue;
            if (viewId == null) {
                actionValue = "#{navigationContext.navigateToDocument(" + docValue + ")}";
            } else {
                actionValue = "#{navigationContext.navigateToDocumentWithView(" + docValue + ", " + viewId + ")}";
            }
            FacesContext facesContext = ctx.getFacesContext();
            Application app = facesContext.getApplication();
            ExpressionFactory ef = app.getExpressionFactory();
            ELContext context = facesContext.getELContext();
            MethodExpression action = ef.createMethodExpression(context, actionValue, String.class, new Class[] {
                    DocumentModel.class, String.class });
            command.setActionExpression(action);
        }
    }

    private String getDocumentValue() {
        String docValue = document.getValue();
        docValue = docValue.trim();
        if (!((docValue.startsWith("${") || docValue.startsWith("#{")) && docValue.endsWith("}"))) {
            log.error("Invalid value for document " + docValue);
        }
        docValue = docValue.substring(2, docValue.length() - 1);
        return docValue;
    }

    private String getViewValue() {
        String viewName = null;
        if (view != null) {
            viewName = view.getValue();
            if (viewName != null) {
                viewName = viewName.trim();
                if ((viewName.startsWith("${") || viewName.startsWith("#{")) && viewName.endsWith("}")) {
                    viewName = viewName.substring(2, viewName.length() - 1);
                } else {
                    viewName = '\"' + viewName + '\"';
                }
            }
        }
        return viewName;
    }

}
