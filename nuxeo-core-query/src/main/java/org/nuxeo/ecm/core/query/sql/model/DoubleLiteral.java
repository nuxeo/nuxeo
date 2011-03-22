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
public class DoubleLiteral extends Literal {

    private static final long serialVersionUID = 5003174671214111301L;

    public final double value;

    public DoubleLiteral(double value) {
        this.value = value;
    }

    public DoubleLiteral(Double value) {
        this.value = value;
    }

    public DoubleLiteral(Float value) {
        this.value = value;
    }

    public DoubleLiteral(String value) {
        this.value = Double.parseDouble(value);
    }

    @Override
    public void accept(IVisitor visitor) {
        visitor.visitDoubleLiteral(this);
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
        if (obj instanceof DoubleLiteral) {
            return value == ((DoubleLiteral) obj).value;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Double.valueOf(value).hashCode();
    }

}
