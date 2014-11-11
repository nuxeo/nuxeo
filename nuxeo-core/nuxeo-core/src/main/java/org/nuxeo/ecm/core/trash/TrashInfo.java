/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.trash;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;

/**
 * Info about the deletion/purge/undeletion of a list of document.
 */
public class TrashInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Docs found ok. */
    public List<DocumentModel> docs;

    /** Refs of common tree roots of docs found ok. */
    public List<DocumentRef> rootRefs;

    /** Paths of common tree roots of docs found ok. */
    public Set<Path> rootPaths;

    /** Refs of parents of common tree roots of docs found ok. */
    public Set<DocumentRef> rootParentRefs;

    /** Number of docs not ok due to permissions or lifecycle state. */
    public int forbidden;

    /** Number of docs not ok due to lock status. */
    public int locked;

    /** Number of docs not ok due to being proxies. */
    public int proxies;

}
