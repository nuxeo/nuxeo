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
package org.nuxeo.apidoc.api;

import java.util.List;

/**
 * Represents a Nuxeo package.
 *
 * @since 11.1
 */
public interface PackageInfo extends NuxeoArtifact {

    String TYPE_NAME = "NXPackage";

    String PROP_PACKAGE_ID = "nxpackage:packageId";

    String PROP_PACKAGE_NAME = "nxpackage:name";

    String PROP_VERSION = "nxpackage:version";

    String PROP_BUNDLES = "nxpackage:bundles";

    String PROP_DEPENDENCIES = "nxpackage:dependencies";

    String PROP_OPTIONAL_DEPENDENCIES = "nxpackage:optionalDependencies";

    String PROP_CONFLICTS = "nxpackage:conflicts";

    String PROP_PACKAGE_TYPE = "nxpackage:type";

    /**
     * @see org.nuxeo.connect.update.Package#getId()
     */
    @Override
    String getId();

    /**
     * @see org.nuxeo.connect.update.Package#getVersion()
     */
    @Override
    String getVersion();

    /**
     * @see org.nuxeo.connect.update.Package#getName()
     */
    String getName();

    /**
     * @see org.nuxeo.connect.update.Package#getTitle()
     */
    String getTitle();

    /**
     * @see org.nuxeo.connect.update.Package#getType()
     */
    String getPackageType();

    /**
     * Returns the list of bundles names identified for this package.
     * <p>
     * Matching is based on jars held by the package, retrieving the bundle name through the MANIFEST file.
     */
    List<String> getBundles();

    /**
     * @see org.nuxeo.connect.update.Package#getDependencies()
     */
    List<String> getDependencies();

    /**
     * @see org.nuxeo.connect.update.Package#getOptionalDependencies()
     */
    List<String> getOptionalDependencies();

    /**
     * @see org.nuxeo.connect.update.Package#getConflicts()
     */
    List<String> getConflicts();

}
