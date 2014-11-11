/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.query.sql.model;

/**
 * Boolean literal.
 */
public class BooleanLiteral extends Literal {

    private static final long serialVersionUID = 1L;

    public final boolean value;

    public BooleanLiteral(boolean value) {
        this.value = value;
    }

    @Override
    public void accept(IVisitor visitor) {
        visitor.visitBooleanLiteral(this);
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
        if (obj instanceof BooleanLiteral) {
            return value == ((BooleanLiteral) obj).value;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Boolean.valueOf(value).hashCode();
    }

}
