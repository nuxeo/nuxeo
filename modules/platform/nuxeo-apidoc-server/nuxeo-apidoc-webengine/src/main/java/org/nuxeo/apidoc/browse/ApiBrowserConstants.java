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

/**
 * @since 11.1
 */
public class ApiBrowserConstants {

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

}
