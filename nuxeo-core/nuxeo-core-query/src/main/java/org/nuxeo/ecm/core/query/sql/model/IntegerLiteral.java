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
public class IntegerLiteral extends Literal {

    private static final long serialVersionUID = 4769705314623462546L;

    public final long value;

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

    @Override
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
