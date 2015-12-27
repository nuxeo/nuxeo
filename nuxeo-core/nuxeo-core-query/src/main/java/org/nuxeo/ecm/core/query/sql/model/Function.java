/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id$
 */

package org.nuxeo.ecm.core.query.sql.model;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
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
