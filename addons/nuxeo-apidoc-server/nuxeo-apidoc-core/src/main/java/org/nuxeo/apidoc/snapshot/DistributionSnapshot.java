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
package org.nuxeo.apidoc.snapshot;

import java.util.List;

import org.nuxeo.apidoc.api.BundleGroup;
import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.OperationInfo;
import org.nuxeo.apidoc.api.SeamComponentInfo;
import org.nuxeo.apidoc.api.ServiceInfo;
import org.nuxeo.apidoc.documentation.JavaDocHelper;
import org.nuxeo.ecm.automation.OperationException;

public interface DistributionSnapshot extends DistributionSnapshotDesc {

    String TYPE_NAME = "NXDistribution";

    String CONTAINER_TYPE_NAME = "Workspace";

    String PROP_NAME = "nxdistribution:name";

    String PROP_VERSION = "nxdistribution:version";

    String PROP_KEY = "nxdistribution:key";

    String getKey();

    void cleanPreviousArtifacts();

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

    List<ExtensionInfo> getContributions();

    ExtensionInfo getContribution(String id);

    List<String> getBundleGroupChildren(String groupId);

    List<Class<?>> getSpi();

    List<String> getSeamComponentIds();

    List<SeamComponentInfo> getSeamComponents();

    SeamComponentInfo getSeamComponent(String id);

    boolean containsSeamComponents();

    OperationInfo getOperation(String id) throws OperationException;

    List<OperationInfo> getOperations() throws OperationException;

    JavaDocHelper getJavaDocHelper();
}
