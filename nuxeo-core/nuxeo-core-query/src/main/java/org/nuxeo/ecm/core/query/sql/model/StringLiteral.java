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
public class StringLiteral extends Literal {

    private static final long serialVersionUID = -7576440083973254527L;

    public final String value;

    public StringLiteral(String value) {
        this.value = value;
    }

    @Override
    public void accept(IVisitor visitor) {
        visitor.visitStringLiteral(this);
    }

    @Override
    public String asString() {
        return value;
    }

    @Override
    public String toString() {
        return new StringBuffer(value.length() + 2)
            .append("\'").append(value).append("\'").toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof StringLiteral) {
            return value.contentEquals(((StringLiteral) obj).value);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

}
