/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.apidoc.browse;

import org.nuxeo.apidoc.api.BundleGroup;
import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.OperationInfo;
import org.nuxeo.apidoc.api.ServiceInfo;

/**
 * @since 11.1
 */
public class ApiBrowserConstants {

    public static final String DISTRIBUTION_ALIAS_CURRENT = "current";

    public static final String DISTRIBUTION_ALIAS_LATEST = "latest";

    public static final String DISTRIBUTION_ALIAS_ADM = "adm";

    public static final String PROPERTY_SITE_MODE = "org.nuxeo.apidoc.site.mode";

    public static final String PROPERTY_HIDE_CURRENT_DISTRIBUTION = "org.nuxeo.apidoc.hide.current.distribution";

    public static final String PROPERTY_TESTER_NAME = "org.nuxeo.ecm.tester.name";

    public static final String LIST_BUNDLEGROUPS = "listBundleGroups";

    public static final String VIEW_BUNDLEGROUP = "viewBundleGroup";

    public static final String LIST_BUNDLES = "listBundles";

    public static final String VIEW_BUNDLE = "viewBundle";

    public static final String LIST_COMPONENTS = "listComponents";

    public static final String VIEW_COMPONENT = "viewComponent";

    public static final String LIST_SERVICES = "listServices";

    public static final String VIEW_SERVICE = "viewService";

    public static final String LIST_EXTENSIONPOINTS = "listExtensionPoints";

    public static final String VIEW_EXTENSIONPOINT = "viewExtensionPoint";

    public static final String LIST_CONTRIBUTIONS = "listContributions";

    public static final String VIEW_CONTRIBUTION = "viewContribution";

    public static final String LIST_OPERATIONS = "listOperations";

    public static final String VIEW_OPERATION = "viewOperation";

    public static final String VIEW_DOCUMENTATION = "documentation";

    public static final boolean check(String url, String view) {
        return url.contains("/" + view);
    }

    public static final String getArtifactView(String artifactType) {
        String view = null;
        if (artifactType.equals(BundleInfo.TYPE_NAME)) {
            view = ApiBrowserConstants.VIEW_BUNDLE;
        } else if (artifactType.equals(BundleGroup.TYPE_NAME)) {
            view = ApiBrowserConstants.VIEW_BUNDLEGROUP;
        } else if (artifactType.equals(ComponentInfo.TYPE_NAME)) {
            view = ApiBrowserConstants.VIEW_COMPONENT;
        } else if (artifactType.equals(ExtensionInfo.TYPE_NAME)) {
            view = ApiBrowserConstants.VIEW_CONTRIBUTION;
        } else if (artifactType.equals(ExtensionPointInfo.TYPE_NAME)) {
            view = ApiBrowserConstants.VIEW_EXTENSIONPOINT;
        } else if (artifactType.equals(ServiceInfo.TYPE_NAME)) {
            view = ApiBrowserConstants.VIEW_SERVICE;
        } else if (artifactType.equals(OperationInfo.TYPE_NAME)) {
            view = ApiBrowserConstants.VIEW_OPERATION;
        }
        return view;
    }

}
