/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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

    private static final long serialVersionUID = -1725102431543210430L;

    public final String name;

    public String originalName;

    public final String cast;

    public final EsHint esHint;

    /** Arbitrary info associated to the reference. */
    public Object info;

    public Reference(String name) {
        this.name = name;
        cast = null;
        esHint = null;
    }

    /** @since 5.6 */
    public Reference(String name, String cast) {
        this.name = name;
        this.cast = cast;
        esHint = null;
    }

    /** @since 9.10 */
    public Reference(String name, String cast, EsHint hint) {
        this.name = name;
        this.cast = cast;
        this.esHint = hint;
    }

    /** @since 5.6 */
    public Reference(Reference other, String cast) {
        this.name = other.name;
        this.cast = cast;
        esHint = null;
    }

    /** @since 7.3 */
    public Reference(Reference other, EsHint hint) {
        this.name = other.name;
        cast = null;
        this.esHint = hint;
    }

    @Override
    public void accept(IVisitor visitor) {
        visitor.visitReference(this);
    }

    public void setInfo(Object info) {
        this.info = info;
    }

    public Object getInfo() {
        return info;
    }

    @Override
    public String toString() {
        if (cast != null) {
            return cast + '(' + name + ')';
        } else if (esHint != null) {
            return esHint.toString() + " " + name;
        }
        return name;
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
        if (!name.equals(other.name)) {
            return false;
        }
        if (cast != null || other.cast != null) {
            return StringUtils.equals(cast, other.cast);
        }
        if (esHint != null) {
            return esHint.equals(other.esHint);
        } else if (other.esHint != null) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = 31 + (cast == null ? 0 : cast.hashCode()) + (esHint == null ? 0 : esHint.hashCode());
        return 31 * result + name.hashCode();
    }

    public boolean isPathReference() {
        return false;
    }

}
