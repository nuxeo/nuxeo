/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.actions.jsf;

import java.util.Locale;

import javax.el.ELContext;
import javax.el.ELResolver;
import javax.el.FunctionMapper;
import javax.el.VariableMapper;

import com.sun.faces.facelets.el.VariableMapperWrapper;

/**
 * Wrapper around another EL context to allow override of the variable mapper for some values in context.
 *
 * @since 5.7.3
 */
public class JSFELContext extends ELContext {

    protected final ELContext originalContext;

    protected final VariableMapper variableMapper;

    public JSFELContext(ELContext originalContext) {
        super();
        this.originalContext = originalContext;
        this.variableMapper = new VariableMapperWrapper(originalContext.getVariableMapper());
    }

    @Override
    public ELResolver getELResolver() {
        return originalContext.getELResolver();
    }

    @Override
    public FunctionMapper getFunctionMapper() {
        return originalContext.getFunctionMapper();
    }

    @Override
    public VariableMapper getVariableMapper() {
        return variableMapper;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Object getContext(Class key) {
        return originalContext.getContext(key);
    }

    @Override
    public Locale getLocale() {
        return originalContext.getLocale();
    }

    @Override
    public boolean isPropertyResolved() {
        return originalContext.isPropertyResolved();
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void putContext(Class key, Object contextObject) {
        originalContext.putContext(key, contextObject);
    }

    @Override
    public void setLocale(Locale locale) {
        originalContext.setLocale(locale);
    }

    @Override
    public void setPropertyResolved(boolean resolved) {
        originalContext.setPropertyResolved(resolved);
    }

}
