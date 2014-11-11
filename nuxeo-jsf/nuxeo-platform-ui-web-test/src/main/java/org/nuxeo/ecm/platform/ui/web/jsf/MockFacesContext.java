/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.ui.web.jsf;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ExpressionFactory;
import javax.faces.application.Application;
import javax.faces.application.FacesMessage;
import javax.faces.application.FacesMessage.Severity;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseStream;
import javax.faces.context.ResponseWriter;
import javax.faces.render.RenderKit;

import org.nuxeo.ecm.platform.el.ExpressionContext;

/**
 * Mock faces context that can be used to resolve expressions in tests.
 * <p>
 * Usage:
 *
 * <pre>
 * MockFacesContext facesContext = new MockFacesContext() {
 *     public Object evaluateExpressionGet(FacesContext context,
 *             String expression, Class expectedType) throws ELException {
 *         if (&quot;#{myTestExpression}&quot;.equals(expression)) {
 *             return myTestResult;
 *         }
 *         return null;
 *     }
 * };
 * facesContext.setCurrent();
 * assertNotNull(FacesContext.getCurrentInstance());
 * </pre>
 *
 * @author Anahide Tchertchian
 */
public class MockFacesContext extends FacesContext {

    protected Application app = new MockApplication();

    protected Map<String, Object> variables = new HashMap<String, Object>();

    protected Map<String, Object> expressions = new HashMap<String, Object>();

    public void mapVariable(String key, Object value) {
        variables.put(key, value);
    }

    public void resetVariables() {
        variables.clear();
    }

    public void mapExpression(String expr, Object res) {
        expressions.put(expr, res);
    }

    public void resetExpressions() {
        expressions.clear();
    }

    public void setCurrent() {
        setCurrentInstance(this);
    }

    public void relieveCurrent() {
        setCurrentInstance(null);
    }

    @Override
    public Application getApplication() {
        return app;
    }

    @Override
    public ELContext getELContext() {
        ELContext ctx = new ExpressionContext();
        ExpressionFactory ef = getApplication().getExpressionFactory();
        for (Map.Entry<String, Object> var : variables.entrySet()) {
            ctx.getVariableMapper().setVariable(var.getKey(),
                    ef.createValueExpression(var.getValue(), Object.class));
        }
        return ctx;
    }

    protected Object evaluateExpression(FacesContext context, String expression) {
        if (expressions.containsKey(expression)) {
            return expressions.get(expression);
        } else {
            ExpressionFactory ef = context.getApplication().getExpressionFactory();
            ELContext elCtx = getELContext();
            return ef.createValueExpression(elCtx, expression, Object.class).getValue(
                    elCtx);
        }
    }

    @Deprecated
    @SuppressWarnings("rawtypes")
    public Object evaluateExpressionGet(FacesContext context,
            String expression, Class expectedType) throws ELException {
        return evaluateExpression(context, expression);
    }

    @Override
    public void addMessage(String clientId, FacesMessage message) {
    }

    @Override
    public Iterator<String> getClientIdsWithMessages() {
        return null;
    }

    @Override
    public ExternalContext getExternalContext() {
        return null;
    }

    @Override
    public Severity getMaximumSeverity() {
        return null;
    }

    @Override
    public Iterator<FacesMessage> getMessages() {
        return null;
    }

    @Override
    public Iterator<FacesMessage> getMessages(String clientId) {
        return null;
    }

    @Override
    public RenderKit getRenderKit() {
        return null;
    }

    @Override
    public boolean getRenderResponse() {
        return false;
    }

    @Override
    public boolean getResponseComplete() {
        return false;
    }

    @Override
    public ResponseStream getResponseStream() {
        return null;
    }

    @Override
    public ResponseWriter getResponseWriter() {
        return null;
    }

    @Override
    public UIViewRoot getViewRoot() {
        return null;
    }

    @Override
    public void release() {
    }

    @Override
    public void renderResponse() {
    }

    @Override
    public void responseComplete() {
    }

    @Override
    public void setResponseStream(ResponseStream responseStream) {
    }

    @Override
    public void setResponseWriter(ResponseWriter responseWriter) {
    }

    @Override
    public void setViewRoot(UIViewRoot root) {
    }

}
