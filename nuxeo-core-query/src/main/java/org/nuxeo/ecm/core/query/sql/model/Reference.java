/*
 * Copyright (c) 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.query.sql.model;

import org.apache.commons.lang.StringUtils;

/**
 * A named reference to a variable (this can be a field or table).
 * <p>
 * Can also include a cast.
 */
public class Reference implements Operand {

    private static final long serialVersionUID = -1725102431543210429L;

    public final String name;

    public final String cast;

    public Reference(String name) {
        this.name = name;
        cast = null;
    }

    /** @since 5.6 */
    public Reference(String name, String cast) {
        this.name = name;
        this.cast = cast;
    }

    /** @since 5.6 */
    public Reference(Reference other, String cast) {
        this.name = other.name;
        this.cast = cast;
    }

    @Override
    public void accept(IVisitor visitor) {
        visitor.visitReference(this);
    }

    @Override
    public String toString() {
        if (cast == null) {
            return name;
        } else {
            return cast + '(' + name + ')';
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Reference)) {
            return false;
        }
        return equals((Reference) obj);
    }

    private boolean equals(Reference other) {
        return name.equals(other.name) && StringUtils.equals(cast, other.cast);
    }

    @Override
    public int hashCode() {
        int result = 31 + (cast == null ? 0 : cast.hashCode());
        return 31 * result + name.hashCode();
    }

    public boolean isPathReference() {
        return false;
    }
}
