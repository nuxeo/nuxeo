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

package org.nuxeo.apidoc.snapshot;

import java.util.Date;
import java.util.List;

import org.nuxeo.apidoc.api.BundleGroup;
import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.ServiceInfo;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;

/**
 *
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 *
 */
public interface DistributionSnapshot extends DistributionSnapshotDesc {

    public static final String TYPE_NAME ="NXDistribution";

    String getKey();

    List<BundleGroup> getBundleGroups();

    BundleGroup getBundleGroup(String groupId);

    List<String> getBundleIds();

    BundleInfo getBundle(String id);

    List<String> getComponentIds();

    List<String> getJavaComponentIds();

    List<String> getXmlComponentIds();

    ComponentInfo getComponent(String id);

    List<String> getServiceIds();

    ServiceInfo getService(String id);

    List<String> getExtensionPointIds();

    ExtensionPointInfo getExtensionPoint(String id);

    List<String> getContributionIds();

    ExtensionInfo getContribution(String id);

    List<String> getBundleGroupChildren(String groupId);

    List<Class> getSpi();


}
