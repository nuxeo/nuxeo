/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.query.sql.model;


/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class IntegerLiteral extends Literal {

    private static final long serialVersionUID = 4769705314623462546L;

    // XXX: Should be final but it can't with the current parser impl.
    public long value;

    public IntegerLiteral(long value) {
        this.value = value;
    }

    public IntegerLiteral(Long value) {
        this.value = value;
    }

    public IntegerLiteral(Integer value) {
        this.value = value;
    }

    public IntegerLiteral(String value) {
        this.value = Long.parseLong(value);
    }

    public void accept(IVisitor visitor) {
        visitor.visitIntegerLiteral(this);
    }

    @Override
    public String asString() {
        return String.valueOf(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof IntegerLiteral) {
            return value == ((IntegerLiteral) obj).value;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Long.valueOf(value).hashCode();
    }

}
