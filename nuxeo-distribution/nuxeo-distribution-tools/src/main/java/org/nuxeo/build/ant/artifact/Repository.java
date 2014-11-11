/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.build.ant.artifact;

import org.apache.tools.ant.types.DataType;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Repository extends DataType {

    public String id;
    public String name;
    public String url;
    public String layout = "default";
    public ReleasesPolicy releasesPolicy;
    public SnapshotsPolicy snapshotsPolicy;

    public void setUrl(String url) {
        this.url = url;
    }

    public void setLayout(String layout) {
        this.layout = layout;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void addReleases(ReleasesPolicy releasesPolicy) {
        this.releasesPolicy = releasesPolicy;
    }

    public void addSnapshots(SnapshotsPolicy snapshotsPolicy) {
        this.snapshotsPolicy = snapshotsPolicy;
    }

}
