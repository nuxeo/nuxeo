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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.el.FunctionMapper;
import javax.el.MethodExpression;
import javax.el.ValueExpression;
import javax.el.VariableMapper;
import javax.faces.FacesException;
import javax.faces.application.Application;
import javax.faces.application.FacesMessage;
import javax.faces.application.FacesMessage.Severity;
import javax.faces.application.NavigationHandler;
import javax.faces.application.StateManager;
import javax.faces.application.ViewHandler;
import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseStream;
import javax.faces.context.ResponseWriter;
import javax.faces.convert.Converter;
import javax.faces.el.MethodBinding;
import javax.faces.el.PropertyResolver;
import javax.faces.el.ReferenceSyntaxException;
import javax.faces.el.ValueBinding;
import javax.faces.el.VariableResolver;
import javax.faces.event.ActionListener;
import javax.faces.render.RenderKit;
import javax.faces.validator.Validator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
@SuppressWarnings("deprecation")
public abstract class MockFacesContext extends FacesContext {

    private static final Log log = LogFactory.getLog(MockFacesContext.class);

    protected Map<String, Object> expressions = new HashMap<String, Object>();

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
        return new MockApplication();
    }

    protected Object evaluateExpression(FacesContext context, String expression) {
        if (expressions.containsKey(expression)) {
            return expressions.get(expression);
        } else {
            log.error("Cannot evaluate expression: " + expression);
            return null;
        }
    }

    public abstract Object evaluateExpressionGet(FacesContext context,
            String expression, Class expectedType) throws ELException;

    public class MockApplication extends Application {

        @Override
        public Object evaluateExpressionGet(FacesContext context,
                String expression, Class expectedType) throws ELException {
            return ((MockFacesContext) context).evaluateExpressionGet(context,
                    expression, expectedType);
        }

        @Override
        public void addComponent(String componentType, String componentClass) {
        }

        @Override
        public void addConverter(Class targetClass, String converterClass) {
        }

        @Override
        public void addConverter(String converterId, String converterClass) {
        }

        @Override
        public void addValidator(String validatorId, String validatorClass) {
        }

        @Override
        public UIComponent createComponent(String componentType)
                throws FacesException {
            return null;
        }

        @Override
        public UIComponent createComponent(ValueBinding componentBinding,
                FacesContext context, String componentType)
                throws FacesException {
            return null;
        }

        @Override
        public Converter createConverter(Class targetClass) {
            return null;
        }

        @Override
        public Converter createConverter(String converterId) {
            return null;
        }

        @Override
        public MethodBinding createMethodBinding(String ref, Class[] params)
                throws ReferenceSyntaxException {
            return null;
        }

        @Override
        public Validator createValidator(String validatorId)
                throws FacesException {
            return null;
        }

        @Override
        public ValueBinding createValueBinding(String ref)
                throws ReferenceSyntaxException {
            return null;
        }

        @Override
        public ActionListener getActionListener() {
            return null;
        }

        @Override
        public Iterator<String> getComponentTypes() {
            return null;
        }

        @Override
        public Iterator<String> getConverterIds() {
            return null;
        }

        @Override
        public Iterator<Class> getConverterTypes() {
            return null;
        }

        @Override
        public Locale getDefaultLocale() {
            return null;
        }

        @Override
        public String getDefaultRenderKitId() {
            return null;
        }

        @Override
        public String getMessageBundle() {
            return null;
        }

        @Override
        public NavigationHandler getNavigationHandler() {
            return null;
        }

        @Override
        public PropertyResolver getPropertyResolver() {
            return null;
        }

        @Override
        public StateManager getStateManager() {
            return null;
        }

        @Override
        public Iterator<Locale> getSupportedLocales() {
            return null;
        }

        @Override
        public Iterator<String> getValidatorIds() {
            return null;
        }

        @Override
        public VariableResolver getVariableResolver() {
            return null;
        }

        @Override
        public ViewHandler getViewHandler() {
            return null;
        }

        @Override
        public void setActionListener(ActionListener listener) {
        }

        @Override
        public void setDefaultLocale(Locale locale) {
        }

        @Override
        public void setDefaultRenderKitId(String renderKitId) {
        }

        @Override
        public void setMessageBundle(String bundle) {
        }

        @Override
        public void setNavigationHandler(NavigationHandler handler) {
        }

        @Override
        public void setPropertyResolver(PropertyResolver resolver) {
        }

        @Override
        public void setStateManager(StateManager manager) {
        }

        @Override
        public void setSupportedLocales(Collection<Locale> locales) {
        }

        @Override
        public void setVariableResolver(VariableResolver resolver) {
        }

        @Override
        public void setViewHandler(ViewHandler handler) {
        }

        @Override
        public ExpressionFactory getExpressionFactory() {
            return new ExpressionFactory() {

                @Override
                public ValueExpression createValueExpression(ELContext arg0,
                        String arg1, Class arg2) {
                    return new MockValueExpression(arg1);
                }

                @Override
                public ValueExpression createValueExpression(Object arg0,
                        Class arg1) {
                    if (arg0 == null) {
                        return new MockValueExpression(null);
                    } else {
                        return new MockValueExpression(arg0.toString());
                    }
                }

                @Override
                public MethodExpression createMethodExpression(ELContext arg0,
                        String arg1, Class arg2, Class[] arg3) {
                    return null;
                }

                @Override
                public Object coerceToType(Object arg0, Class arg1) {
                    return arg0;
                }
            };
        }

    }

    public class MockValueExpression extends ValueExpression {

        private static final long serialVersionUID = 1L;

        protected final String expr;

        private MockValueExpression(String expr) {
            super();
            this.expr = expr;
        }

        @Override
        public boolean isLiteralText() {
            return false;
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public String getExpressionString() {
            return expr;
        }

        @Override
        public boolean equals(Object arg0) {
            return false;
        }

        @Override
        public void setValue(ELContext arg0, Object arg1) {
            // do nothing
        }

        @Override
        public boolean isReadOnly(ELContext arg0) {
            return false;
        }

        @Override
        public Object getValue(ELContext arg0) {
            return evaluateExpressionGet(null, expr, null);
        }

        @Override
        public Class<?> getType(ELContext arg0) {
            return null;
        }

        @Override
        public Class<?> getExpectedType() {
            return null;
        }
    };

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

    @Override
    public ELContext getELContext() {
        return new ELContext() {

            @Override
            public VariableMapper getVariableMapper() {
                return null;
            }

            @Override
            public FunctionMapper getFunctionMapper() {
                return null;
            }

            @Override
            public ELResolver getELResolver() {
                return null;
            }
        };
    }

}
