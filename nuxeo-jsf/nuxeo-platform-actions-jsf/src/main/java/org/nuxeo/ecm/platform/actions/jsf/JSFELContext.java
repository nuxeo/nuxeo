/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
 * Wrapper around another EL context to allow override of the variable mapper
 * for some values in context.
 *
 * @since 5.7.3
 */
public class JSFELContext extends ELContext {

    protected final ELContext originalContext;

    protected final VariableMapper variableMapper;

    public JSFELContext(ELContext originalContext) {
        super();
        this.originalContext = originalContext;
        this.variableMapper = new VariableMapperWrapper(
                originalContext.getVariableMapper());
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