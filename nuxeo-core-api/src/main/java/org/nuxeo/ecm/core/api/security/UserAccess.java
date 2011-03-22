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
 *
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public final class UserAccess implements Serializable {

    public static final int RO = 1; // read only
    public static final int GRANTED = 2;
    public static final int DENIED  = 4;

    private static final long serialVersionUID = -2700532849385445481L;

    private final int value;

    public UserAccess(boolean granted) {
        this(granted, false);
    }

    public UserAccess(boolean granted, boolean readonly) {
        int val = granted ? GRANTED : DENIED;
        if (readonly) {
            val |= RO;
        }
        value = val;
    }

    public boolean isGranted() {
        return (value & GRANTED) != 0;
    }

    public boolean isDenied() {
        return (value & DENIED) != 0;
    }

    public boolean isReadOnly() {
        return (value & RO) != 0;
    }

}
