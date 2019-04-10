/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.service;

import java.util.Collections;
import java.util.Set;

import org.nuxeo.ecm.core.api.IdRef;

/**
 * Data transfer object to fetch the list of references of synchronization roots for a given repo and user.
 */
public class SynchronizationRoots {

    protected final String repositoryName;

    protected final Set<String> paths;

    protected final Set<IdRef> refs;

    public SynchronizationRoots(String repositoryName, Set<String> paths, Set<IdRef> refs) {
        this.repositoryName = repositoryName;
        this.paths = paths;
        this.refs = refs;
    }

    public static final SynchronizationRoots getEmptyRoots(String repositoryName) {
        Set<String> emptyPaths = Collections.emptySet();
        Set<IdRef> emptyRefs = Collections.emptySet();
        return new SynchronizationRoots(repositoryName, emptyPaths, emptyRefs);
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public Set<String> getPaths() {
        return paths;
    }

    public Set<IdRef> getRefs() {
        return refs;
    }

    /**
     * @since 9.1
     */
    @Override
    public String toString() {
        return new StringBuilder(getClass().getSimpleName()).append("(repo = ")
                                                            .append(repositoryName)
                                                            .append(", paths = ")
                                                            .append(paths)
                                                            .append(", refs = ")
                                                            .append(refs)
                                                            .append(")")
                                                            .toString();
    }

}
