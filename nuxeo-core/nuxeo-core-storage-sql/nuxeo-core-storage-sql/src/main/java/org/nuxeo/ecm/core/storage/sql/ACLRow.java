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

package org.nuxeo.ecm.core.storage.sql;

import java.io.Serializable;
import java.util.Comparator;

/**
 * A simple value holding one row of the ACLs table.
 *
 * @author Florent Guillaume
 */
public class ACLRow implements Serializable {

    private static final long serialVersionUID = 1L;

    public final int pos;

    public final String name;

    public final boolean grant;

    public final String permission;

    public final String user;

    public final String group;

    public ACLRow(int pos, String name, boolean grant, String permission,
            String user, String group) {
        this.pos = pos;
        this.name = name;
        this.grant = grant;
        this.permission = permission;
        this.user = user;
        this.group = group;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + pos + ',' + name + ','
                + (grant ? "GRANT" : "DENY") + ',' + permission + ',' + user
                + ',' + group + ')';
    }

    /**
     * Comparator of {@link ACLRow}s according to their pos field.
     */
    public static class ACLRowPositionComparator implements Comparator<ACLRow> {

        public static final ACLRowPositionComparator INSTANCE = new ACLRowPositionComparator();

        @Override
        public int compare(ACLRow acl1, ACLRow acl2) {
            return acl1.pos - acl2.pos;
        }
    }

}
