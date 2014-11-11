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

package org.nuxeo.ecm.core.query.sql.model;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Function implements Operand {

    private static final long serialVersionUID = -6107133982072616209L;

    public final String name;
    public final OperandList args;

    public Function(String name) {
        this(name, null);
    }

    public Function(String name, OperandList args) {
        this.name = name;
        this.args = args;
    }

    @Override
    public String toString() {
        return args == null ? name + "()" : name + '(' + args + ')';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Function) {
            Function func = (Function) obj;
            if (args == null) {
                return func.args == null;
            }
            return name.equals(func.name) && args.equals(func.args);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + (args != null ? args.hashCode() : 0);
        return result;
    }

    @Override
    public void accept(IVisitor visitor) {
        visitor.visitFunction(this);
    }

}
