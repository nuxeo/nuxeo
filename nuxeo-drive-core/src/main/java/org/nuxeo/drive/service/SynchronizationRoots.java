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

import java.util.Set;

import org.nuxeo.ecm.core.api.IdRef;

/**
 * Data transfer object to fetch the list of references of synchronization roots
 * for a given repo and user.
 */
public class SynchronizationRoots {

    public final String repositoryName;

    public final Set<String> paths;

    public final Set<IdRef> refs;

    public SynchronizationRoots(String repositoryName, Set<String> paths,
            Set<IdRef> refs) {
        this.repositoryName = repositoryName;
        this.paths = paths;
        this.refs = refs;
    }

}
