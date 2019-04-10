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
 *     Bogdan Stefanescu
 *     Thierry Delprat
 */
package org.nuxeo.apidoc.api;

import java.util.Collection;

public interface BundleInfo extends NuxeoArtifact {

    String TYPE_NAME = "NXBundle";

    String PROP_ARTIFACT_GROUP_ID = "nxbundle:artifactGroupId";

    String PROP_ARTIFACT_ID = "nxbundle:artifactId";

    String PROP_ARTIFACT_VERSION = "nxbundle:artifactVersion";

    String PROP_BUNDLE_ID = "nxbundle:bundleId";

    String PROP_JAR_NAME = "nxbundle:jarName";

    Collection<ComponentInfo> getComponents();

    String getFileName();

    String getBundleId();

    String[] getRequirements();

    String getManifest();

    String getLocation();

    String getArtifactGroupId();

    String getArtifactId();

    String getArtifactVersion();

}
