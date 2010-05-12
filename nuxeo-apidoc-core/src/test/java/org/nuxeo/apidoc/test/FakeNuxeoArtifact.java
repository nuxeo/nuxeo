/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.apidoc.test;

import org.nuxeo.apidoc.api.AssociatedDocuments;
import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.ecm.core.api.CoreSession;

public class FakeNuxeoArtifact implements NuxeoArtifact {

    public String id;

    public String version;

    public String type;

    public FakeNuxeoArtifact(NuxeoArtifact artifact) {
        this.id = artifact.getId();
        this.version = artifact.getVersion();
        this.type = artifact.getArtifactType();
    }

    public AssociatedDocuments getAssociatedDocuments(CoreSession session) {
        return null;
    }

    public String getId() {
        return id;
    }

    public String getVersion() {
        return version;
    }

    public String getArtifactType() {
        return type;
    }

}
