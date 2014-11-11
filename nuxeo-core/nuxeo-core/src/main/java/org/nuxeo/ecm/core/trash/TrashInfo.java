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
