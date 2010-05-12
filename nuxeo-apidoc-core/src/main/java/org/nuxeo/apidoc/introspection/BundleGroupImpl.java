/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.apidoc.introspection;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.apidoc.api.BaseNuxeoArtifact;
import org.nuxeo.apidoc.api.BundleGroup;

/**
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 */
public class BundleGroupImpl extends BaseNuxeoArtifact implements BundleGroup {

    protected final String key;
    protected final String name;
    protected final List<BundleGroup> subGroups = new ArrayList<BundleGroup>();
    protected final List<String> bundleIds = new ArrayList<String>();
    protected final String version;

    public BundleGroupImpl(String key, String version) {
        this.key = key;
        if (key.startsWith("grp:")) {
            name = key.substring(4);
        } else {
            name = key;
        }
        this.version = version;
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public void add(BundleGroupImpl group) {
        subGroups.add(group);
    }

    public void add(String bundleId) {
        bundleIds.add(bundleId);
    }

    public List<BundleGroup> getSubGroups() {
        return subGroups;
    }

    public List<String> getBundleIds() {
        return bundleIds;
    }

    @Override
    public String getId() {
        return key;
    }

    public String getVersion() {
        return version;
    }

    public String getArtifactType() {
        return TYPE_NAME;
    }

}
