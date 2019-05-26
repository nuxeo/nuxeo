/*
 * (C) Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Contributors:
 *     Anahide Tchertchian
 */

package org.nuxeo.ecm.platform.ui.web.validator;

import java.beans.FeatureDescriptor;
import java.util.Iterator;
import java.util.Locale;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ELResolver;
import javax.el.FunctionMapper;
import javax.el.ValueExpression;
import javax.el.ValueReference;
import javax.el.VariableMapper;
import javax.faces.el.CompositeComponentExpressionHolder;

import org.nuxeo.ecm.platform.ui.web.model.ProtectedEditableModel;

/**
 * Analyzes a {@link ValueExpression} and provides access to the base object and property name to which the expression
 * maps via the getReference() method.
 *
 * @since 7.2
 */
public class ValueExpressionAnalyzer {

    private ValueExpression expression;

    public ValueExpressionAnalyzer(ValueExpression expression) {
        this.expression = expression;
    }

    public ValueReference getReference(ELContext elContext) {
        InterceptingResolver resolver = new InterceptingResolver(elContext.getELResolver());
        try {
            expression.setValue(decorateELContext(elContext, resolver), null);
        } catch (ELException ele) {
            return null;
        }
        ValueReference reference = resolver.getValueReference();
        if (reference != null) {
            Object base = reference.getBase();
            if (base instanceof CompositeComponentExpressionHolder) {
                Object prop = reference.getProperty();
                if (prop instanceof String) {
                    ValueExpression ve = ((CompositeComponentExpressionHolder) base).getExpression((String) prop);
                    if (ve != null) {
                        this.expression = ve;
                        reference = getReference(elContext);
                    }
                }
            }
        }
        return reference;
    }

    private ELContext decorateELContext(final ELContext context, final ELResolver resolver) {
        return new ELContext() {

            // punch in our new ELResolver
            @Override
            public ELResolver getELResolver() {
                return resolver;
            }

            // The rest of the methods simply delegate to the existing context

            @Override
            @SuppressWarnings("rawtypes")
            public Object getContext(Class key) {
                return context.getContext(key);
            }

            @Override
            public Locale getLocale() {
                return context.getLocale();
            }

            @Override
            public boolean isPropertyResolved() {
                return context.isPropertyResolved();
            }

            @Override
            @SuppressWarnings("rawtypes")
            public void putContext(Class key, Object contextObject) {
                context.putContext(key, contextObject);
            }

            @Override
            public void setLocale(Locale locale) {
                context.setLocale(locale);
            }

            @Override
            public void setPropertyResolved(boolean resolved) {
                context.setPropertyResolved(resolved);
            }

            @Override
            public FunctionMapper getFunctionMapper() {
                return context.getFunctionMapper();
            }

            @Override
            public VariableMapper getVariableMapper() {
                return context.getVariableMapper();
            }
        };
    }

    static class ListItemMapper {

        protected ProtectedEditableModel model;

        protected Object value;

        public ListItemMapper(ProtectedEditableModel model, Object value) {
            super();
            this.model = model;
            this.value = value;
        }

        public ProtectedEditableModel getModel() {
            return model;
        }

        public Object getValue() {
            return value;
        }

        @Override
        public String toString() {
            final StringBuilder buf = new StringBuilder();
            buf.append("ListItemMapper");
            buf.append(" {");
            buf.append(" model=");
            buf.append(model);
            buf.append(", value=");
            buf.append(value);
            buf.append('}');
            return buf.toString();
        }

    }

    private static class InterceptingResolver extends ELResolver {

        private ELResolver delegate;

        private ValueReference valueReference;

        public InterceptingResolver(ELResolver delegate) {
            this.delegate = delegate;
        }

        public ValueReference getValueReference() {
            return valueReference;
        }

        // Capture the base and property rather than write the value
        @Override
        public void setValue(ELContext context, Object base, Object property, Object value) {
            if (base != null && property != null) {
                context.setPropertyResolved(true);
                valueReference = new ValueReference(base, property.toString());
            }
        }

        // The rest of the methods simply delegate to the existing context

        @Override
        public Object getValue(ELContext context, Object base, Object property) {
            Object value = delegate.getValue(context, base, property);
            if (base != null && ProtectedEditableModel.class.isAssignableFrom(base.getClass())
                    && "rowData".equals(property)) {
                // mapping a list element => wrap result
                return new ListItemMapper((ProtectedEditableModel) base, value);
            }
            return value;
        }

        @Override
        public Class<?> getType(ELContext context, Object base, Object property) {
            return delegate.getType(context, base, property);
        }

        @Override
        public boolean isReadOnly(ELContext context, Object base, Object property) {
            return delegate.isReadOnly(context, base, property);
        }

        @Override
        public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext context, Object base) {
            return delegate.getFeatureDescriptors(context, base);
        }

        @Override
        public Class<?> getCommonPropertyType(ELContext context, Object base) {
            return delegate.getCommonPropertyType(context, base);
        }

    }
}
