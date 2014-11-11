/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.search.api.client.search.results.document.impl;

import java.util.Set;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;

/**
 * A DocumentModel that caches info that DocumentModelImpl would fetch from the
 * core.
 *
 * <p>
 * Typical use-case: search engine results can have said info that is needed to
 * build a viable DocumentModel for results display.
 *
 * @author <a href="mailto:gracinet@nuxeo.com">Georges Racinet</a>
 *
 */
public class ResultDocumentModel extends DocumentModelImpl {

    private static final long serialVersionUID = 7921752462897297121L;

    private final String currentLifeCycleState;

    private final String versionLabel;

    @Deprecated
    public ResultDocumentModel(String type, String id, Path path,
            DocumentRef docRef, DocumentRef parentRef, String[] schemas,
            Set<String> facets, String lifeCycleState, String versionLabel,
            String repoName) {
        // no session id (sid) !
        super(null, type, id, path, null, docRef, parentRef, schemas, facets,
                null, null);
        repositoryName = repoName;

        currentLifeCycleState = lifeCycleState;
        this.versionLabel = versionLabel;
    }

    public ResultDocumentModel(String type, String id, Path path,
            DocumentRef docRef, DocumentRef parentRef, String[] schemas,
            Set<String> facets, String lifeCycleState, String versionLabel,
            String repoName, long flags) {
        this(type, id, path, docRef, parentRef, schemas, facets,
                lifeCycleState, versionLabel, repoName);
        this.flags = flags;
    }

    @Override
    public String getCurrentLifeCycleState() {
        return currentLifeCycleState;
    }

    @Override
    public String getVersionLabel() {
        return versionLabel;
    }

}
