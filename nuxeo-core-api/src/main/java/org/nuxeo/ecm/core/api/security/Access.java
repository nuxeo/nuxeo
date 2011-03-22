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

package org.nuxeo.ecm.core.api.security;

import java.io.Serializable;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public final class Access implements Serializable {

    public static final Access GRANT = new Access(1);
    public static final Access DENY = new Access(0);
    public static final Access UNKNOWN = new Access(-1);

    private static final long serialVersionUID = 4797108620404301529L;

    private final int value;


    private Access(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    /**
     * If granted returns true, otherwise returns false.
     *
     * @return true if granted
     */
    public boolean toBoolean() {
        return value > 0;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    /**
     * Be aware of Java serialization.
     * Avoid initializing another instance than allowed constants.
     *
     * @return GRANT, DENY or UNKNOWN
     */
    private Object readResolve() {
        switch (value) {
        case 1:
            return GRANT;
        case 0:
            return DENY;
        default:
            return UNKNOWN;
        }
    }

}
