/*
 * Copyright 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the "License").  You may not use this file except
 * in compliance with the License.
 *
 * You can obtain a copy of the license at
 * glassfish/bootstrap/legal/CDDLv1.0.txt or
 * https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * HEADER in each file and include the License file at
 * glassfish/bootstrap/legal/CDDLv1.0.txt.  If applicable,
 * add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your
 * own identifying information: Portions Copyright [yyyy]
 * [name of copyright owner]
 */

package org.nuxeo.ecm.platform.ui.web.binding;

import javax.el.ELContext;
import javax.el.ELResolver;
import javax.el.FunctionMapper;
import javax.el.VariableMapper;

public final class EvaluationContext extends ELContext {

    private final ELContext elContext;

    private final FunctionMapper fnMapper;

    private final VariableMapper varMapper;

    public EvaluationContext(ELContext elContext, FunctionMapper fnMapper, VariableMapper varMapper) {
        this.elContext = elContext;
        this.fnMapper = fnMapper;
        this.varMapper = varMapper;
    }

    public ELContext getELContext() {
        return elContext;
    }

    @Override
    public FunctionMapper getFunctionMapper() {
        return fnMapper;
    }

    @Override
    public VariableMapper getVariableMapper() {
        return varMapper;
    }

    @Override
    public Object getContext(@SuppressWarnings("rawtypes") Class key) {
        return elContext.getContext(key);
    }

    @Override
    public ELResolver getELResolver() {
        return elContext.getELResolver();
    }

    @Override
    public boolean isPropertyResolved() {
        return elContext.isPropertyResolved();
    }

    @Override
    public void putContext(@SuppressWarnings("rawtypes") Class key, Object contextObject) {
        elContext.putContext(key, contextObject);
    }

    @Override
    public void setPropertyResolved(boolean resolved) {
        elContext.setPropertyResolved(resolved);
    }

}
