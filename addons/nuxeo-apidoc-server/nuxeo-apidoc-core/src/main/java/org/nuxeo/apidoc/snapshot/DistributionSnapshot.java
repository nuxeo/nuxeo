/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

    OperationInfo getOperation(String id);

    List<OperationInfo> getOperations();

    JavaDocHelper getJavaDocHelper();
}
