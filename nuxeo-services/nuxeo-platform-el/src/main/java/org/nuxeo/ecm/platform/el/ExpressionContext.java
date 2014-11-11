/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Stephane Lacoin (Nuxeo EP Software Engineer)
 */
package org.nuxeo.ecm.platform.el;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.el.ArrayELResolver;
import javax.el.BeanELResolver;
import javax.el.CompositeELResolver;
import javax.el.ELContext;
import javax.el.ELResolver;
import javax.el.FunctionMapper;
import javax.el.MapELResolver;
import javax.el.ValueExpression;
import javax.el.VariableMapper;

public class ExpressionContext extends ELContext {

    private static class MyVariableMapper extends VariableMapper {

        protected final Map<String, ValueExpression> map = new HashMap<String, ValueExpression>();

        @Override
        public ValueExpression resolveVariable(String variable) {
            return map.get(variable);
        }

        @Override
        public ValueExpression setVariable(String variable, ValueExpression expression) {
            return map.put(variable, expression);
        }
    }

    private static class MyFunctionMapper extends FunctionMapper {

        private final Map<String, Method> map = new HashMap<String, Method>();

        public void setFunction(String prefix, String localName, Method method) {
            map.put(prefix + ":" + localName, method);
        }

        @Override
        public Method resolveFunction(String prefix, String localName) {
            return map.get(prefix + ":" + localName);
        }
    }

    private static class MyResolver extends CompositeELResolver {

        private MyResolver() {
            add(new DocumentModelResolver());
            add(new MapELResolver());
            add(new ArrayELResolver());
            add(new BeanELResolver());
        }

    }

    private final ELResolver resolver = new MyResolver();

    private final FunctionMapper functionMapper = new MyFunctionMapper();
    private final VariableMapper variableMapper = new MyVariableMapper();

    @Override
    public ELResolver getELResolver() {
        return resolver;
    }

    @Override
    public FunctionMapper getFunctionMapper() {
        return functionMapper;
    }

    @Override
    public VariableMapper getVariableMapper() {
        return variableMapper;
    }

}
