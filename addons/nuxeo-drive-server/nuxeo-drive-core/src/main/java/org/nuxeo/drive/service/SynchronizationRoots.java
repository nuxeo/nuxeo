/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.service;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

import org.nuxeo.ecm.core.api.IdRef;

/**
 * Data transfer object to fetch the list of references of synchronization roots for a given repo and user.
 */
public class SynchronizationRoots implements Serializable {

    private static final long serialVersionUID = 5975197559729672670L;

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
