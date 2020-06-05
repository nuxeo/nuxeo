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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;

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

    String CONNECT_URL_PROP_NAME = "org.nuxeo.apidoc.connect.url";

    String DEFAULT_CONNECT_URL = "https://connect.nuxeo.com/nuxeo/site/";

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

    /**
     * Returns the corresponding URL for this package on the marketplace.
     * <p>
     * URLs are in the form:
     * https://connect.nuxeo.com/nuxeo/site/marketplace/package/platform-explorer?version=11.1.0-SNAPSHOT
     * <p>
     * Base URL can be configured on the {@link ConfigurationService}, see {@link #CONNECT_URL_PROP_NAME}.
     *
     * @param checkValidity if true, the URL will be checked and null will be returned if the URL is not valid.
     */
    public static String getMarketplaceURL(PackageInfo pkg, boolean checkValidity) {
        if (pkg == null) {
            return null;
        }
        String baseUrl = Framework.getService(ConfigurationService.class)
                                  .getString(CONNECT_URL_PROP_NAME, DEFAULT_CONNECT_URL);
        String url = String.format("%smarketplace/package/%s?version=%s", baseUrl, pkg.getName(), pkg.getVersion());
        if (checkValidity) {
            try {
                HttpURLConnection huc = (HttpURLConnection) new URL(url).openConnection();
                huc.setRequestMethod("HEAD");
                if (HttpURLConnection.HTTP_OK != huc.getResponseCode()) {
                    return null;
                }
            } catch (IOException e) {
                return null;
            }
        }
        return url;
    }

}
