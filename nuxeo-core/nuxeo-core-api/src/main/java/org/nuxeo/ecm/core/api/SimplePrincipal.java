/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     dragos
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api;

import java.io.Serializable;
import java.security.Principal;

/**
 * Used to change permission in connect.
 */
//FIXME remove this when connect will be changed to use SystemPrincipal
public class SimplePrincipal implements Principal, Serializable {

    private static final long serialVersionUID = 4899743263998931844L;

    final String id;

    public SimplePrincipal(String id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return id;
    }

}
