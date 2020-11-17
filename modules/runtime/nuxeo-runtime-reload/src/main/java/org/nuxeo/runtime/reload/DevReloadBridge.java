/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc <kleturc@nuxeo.com>
 *
 */
package org.nuxeo.runtime.reload;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.nuxeo.runtime.RuntimeServiceException;
import org.nuxeo.runtime.api.Framework;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

/**
 * This class is used from DevFrameworkBootstrap class by reflection. It is instantiated by reflection too, so keep the
 * default constructor.
 *
 * @since 9.3
 * @apiNote We need to keep common type for arguments and returned value.
 */
public class DevReloadBridge {

    /**
     * Keep this default constructor. The class is instantiated from ReloadServiceInvoker by reflection.
     */
    public DevReloadBridge() {
        // nothing to do
    }

    /**
     * @return the deployed bundles as a map associating the bundle symbolic name to its location.
     */
    public Map<String, String> reloadBundles(List<String> bundleNamesToUndeploy, List<File> bundlesToDeploy) {
        ReloadService reloadService = Framework.getService(ReloadService.class);

        ReloadContext context = new ReloadContext("dev-bundles").undeploy(bundleNamesToUndeploy)
                                                                .deploy(bundlesToDeploy);

        try {
            // reload server
            ReloadResult result = reloadService.reloadBundles(context);
            // convert result to something usual
            // furthermore, we just need the deployed bundles list in DevFrameworkBootstrap
            return result.deployedBundles()
                         .stream()
                         .collect(Collectors.toMap(Bundle::getSymbolicName, Bundle::getLocation));
        } catch (BundleException e) {
            throw new RuntimeServiceException("Unable to reload server", e);
        }
    }

}
