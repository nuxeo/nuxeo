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

package org.nuxeo.ecm.core.api;

/**
 * An ID reference to a document.
 *
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class IdRef implements DocumentRef {

    private static final long serialVersionUID = 2796201881930443026L;

    public final String value;


    public IdRef(String value) {
        this.value = value;
    }

    @Override
    public int type() {
        return ID;
    }

    @Override
    public Object reference() {
        return value;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof IdRef) {
            return ((IdRef) obj).value.equals(value);
        }
        // it is not possible to compare an IdRef with a PathRef
        return false;
    }

    @Override
    public String toString() {
        return value;
    }

}
