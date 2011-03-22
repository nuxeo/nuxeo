/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime.expression;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.jexl.JexlContext;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @author  <a href="mailto:rspivak@nuxeo.com">Ruslan Spivak</a>
 */
public class Context implements JexlContext {

    private Map<String, Object> vars = new HashMap<String, Object>();

    public Context() {
    }

    public Context(Map<String, Object> map) {
        vars = map;
    }

    @Override
    public Map<String, Object> getVars() {
        return vars;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setVars(Map vars) {
        this.vars = vars;
    }

    public void put(String key, Object value) {
        vars.put(key, value);
    }
}
