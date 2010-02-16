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

package org.nuxeo.apidoc.api;

import java.util.Collection;
/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 *
 */
public interface BundleInfo extends NuxeoArtifact {

    public static final String TYPE_NAME ="NXBundle";

    public Collection<ComponentInfo> getComponents();

    public String getFileName();

    public String getBundleId();

    public String[] getRequirements();

    public String getManifest();

    public String getLocation();

    public String getArtifactGroupId();

    public String getArtifactId();

    public String getArtifactVersion();

}