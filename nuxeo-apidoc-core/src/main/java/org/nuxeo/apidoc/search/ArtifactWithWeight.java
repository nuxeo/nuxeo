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

package org.nuxeo.apidoc.search;

import org.nuxeo.apidoc.api.NuxeoArtifact;

public class ArtifactWithWeight implements Comparable<ArtifactWithWeight> {

    protected NuxeoArtifact artifact;

    protected int hits=0;

    public ArtifactWithWeight(NuxeoArtifact artifact) {
        this.artifact=artifact;
        hits=1;
    }
    public NuxeoArtifact getArtifact() {
        return artifact;
    }

    public int getHitNumbers() {
        return hits;
    }

    public void addHit() {
        hits+=1;
    }

    public int compareTo(ArtifactWithWeight other) {
        return new Integer(hits).compareTo(new Integer(other.getHitNumbers()));
    }

}
