/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.api.trash;

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
