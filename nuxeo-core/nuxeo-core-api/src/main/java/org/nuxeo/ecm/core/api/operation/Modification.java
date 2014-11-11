/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.operation;

import org.nuxeo.ecm.core.api.DocumentRef;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Modification {
    public final static int ADD_CHILD = 1;
    public final static int REMOVE_CHILD = 2;
    public final static int ORDER_CHILD = 4;
    public final static int CONTAINER_MODIFICATION = ADD_CHILD | REMOVE_CHILD | ORDER_CHILD;

    public final static int CREATE = 8;
    public final static int REMOVE = 16;
    public final static int MOVE = 32;
    public final static int EXISTENCE_MODIFICATION = CREATE | REMOVE | MOVE;

    public final static int CONTENT = 64;
    public final static int SECURITY = 128;
    public final static int STATE = 256;
    public final static int UPDATE_MODIFICATION =CONTENT | SECURITY | STATE;

    public int type;
    public DocumentRef ref;

    public Modification(DocumentRef ref, int type) {
        this.ref = ref;
        this.type = type;
    }

    public final boolean isUpdateModification() {
        return (type & UPDATE_MODIFICATION) != 0;
    }

    public final boolean isContainerModification() {
        return (type & CONTAINER_MODIFICATION) != 0;
    }

    public final boolean isExistenceModification() {
        return (type & EXISTENCE_MODIFICATION) != 0;
    }

    public final boolean isAddChild() {
        return (type & ADD_CHILD) != 0;
    }

    public final boolean isRemoveChild() {
        return (type & REMOVE_CHILD) != 0;
    }

    public final boolean isOrderChild() {
        return (type & ORDER_CHILD) != 0;
    }

    public final boolean isCreate() {
        return (type & CREATE) != 0;
    }

    public final boolean isRemove() {
        return (type & REMOVE) != 0;
    }

    public final boolean isContentUpdate() {
        return (type & CONTENT) != 0;
    }

    public final boolean isStateUpdate() {
        return (type & STATE) != 0;
    }

    public final boolean isSecurityUpdate() {
        return (type & SECURITY) != 0;
    }

    @Override
    public String toString() {
        return ref+" ["+type+"]";
    }

}
