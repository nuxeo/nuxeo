/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.util;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;

/**
 * @author Thierry Delprat
 */
public class LocationManagerService extends DefaultComponent {

    public static final ComponentName NAME = new ComponentName("org.nuxeo.ecm.platform.util.LocationManagerService");

    private static final Log log = LogFactory.getLog(LocationManagerService.class);

    private Map<String, RepositoryLocation> locations = new HashMap<>();

    @Override
    public void registerExtension(Extension extension) {
        Object[] contribs = extension.getContributions();
        for (Object contrib : contribs) {
            register((LocationManagerPluginExtension) contrib);
        }
    }

    private void register(LocationManagerPluginExtension pluginExtension) {
        String locationName = pluginExtension.getLocationName();
        boolean locationEnabled = pluginExtension.getLocationEnabled();

        log.info("Registering location manager: " + locationName + (locationEnabled ? "" : " (disabled)"));

        RepositoryLocation locationTempPlugin = new RepositoryLocation(locationName);

        if (locations.containsKey(locationName)) {
            if (!locationEnabled) {
                locations.remove(locationName);
            }
        } else {
            locations.put(locationName, locationTempPlugin);
        }
    }

    @Override
    public void unregisterExtension(Extension extension) {
        locations.clear();
        locations = null;
    }

    public Map<String, RepositoryLocation> getAvailableLocations() {
        return locations;
    }

}
