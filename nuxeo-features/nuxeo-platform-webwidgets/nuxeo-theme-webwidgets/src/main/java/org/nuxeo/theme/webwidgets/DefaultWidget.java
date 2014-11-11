/*
 * (C) Copyright 2006-2007 Nuxeo SAS <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jean-Marc Orliaguet, Chalmers
 *
 * $Id$
 */

package org.nuxeo.theme.webwidgets;

public class DefaultWidget implements Widget {

    private final String name;

    private final String uid;

    public DefaultWidget(final String name, final String uid) {
        this.name = name;
        this.uid = uid;
    }

    public String getUid() {
        return uid;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other instanceof DefaultWidget) {
            return ((DefaultWidget) other).uid == uid;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Integer.parseInt(uid);
    }

}
