/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

    public static final ComponentName NAME = new ComponentName(
            "org.nuxeo.ecm.platform.util.LocationManagerService");

    private static final Log log = LogFactory.getLog(LocationManagerService.class);

    private Map<String, RepositoryLocation> locations = new HashMap<String, RepositoryLocation>();

    @Override
    public void registerExtension(Extension extension) throws Exception {
        Object[] contribs = extension.getContributions();
        for (Object contrib : contribs) {
            register((LocationManagerPluginExtension) contrib);
        }
    }

    private void register(LocationManagerPluginExtension pluginExtension) {
        String locationName = pluginExtension.getLocationName();
        boolean locationEnabled = pluginExtension.getLocationEnabled();

        log.info("Registering location manager: " + locationName +
                (locationEnabled ? "" : " (disabled)"));

        RepositoryLocation locationTempPlugin = new RepositoryLocation(
                locationName);

        if (locations.containsKey(locationName)) {
            if (!locationEnabled) {
                locations.remove(locationName);
            }
        } else {
            locations.put(locationName, locationTempPlugin);
        }
    }

    @Override
    public void unregisterExtension(Extension extension) throws Exception {
        locations.clear();
        locations = null;
    }

    public Map<String, RepositoryLocation> getAvailableLocations() {
        return locations;
    }

}
