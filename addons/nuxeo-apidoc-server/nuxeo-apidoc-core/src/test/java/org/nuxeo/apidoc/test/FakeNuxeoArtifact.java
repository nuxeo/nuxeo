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
 *     Thierry Delprat
 */
package org.nuxeo.apidoc.test;

import org.nuxeo.apidoc.api.AssociatedDocuments;
import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.ecm.core.api.CoreSession;

public class FakeNuxeoArtifact implements NuxeoArtifact {

    public final String id;

    public String version;

    public final String type;

    public FakeNuxeoArtifact(NuxeoArtifact artifact) {
        id = artifact.getId();
        version = artifact.getVersion();
        type = artifact.getArtifactType();
    }

    @Override
    public AssociatedDocuments getAssociatedDocuments(CoreSession session) {
        return null;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getArtifactType() {
        return type;
    }

    @Override
    public String getHierarchyPath() {
        // TODO Auto-generated method stub
        return null;
    }

}
