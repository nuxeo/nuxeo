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
 *
 */
public class LocationManagerService extends DefaultComponent {

    public static final ComponentName NAME =
        new ComponentName("org.nuxeo.ecm.platform.util.LocationManagerService");

    private static final Log log = LogFactory.getLog(LocationManagerService.class);

    private Map<String, RepositoryLocation> locations = new HashMap<String, RepositoryLocation>();

    @Override
    public void registerExtension(Extension extension) throws Exception {
        Object[] contribs = extension.getContributions();
        for (Object contrib : contribs) {
            log.info("registering LocationManager Plugin ... ");
            LocationManagerPluginExtension locationManagerPlugin
                    = (LocationManagerPluginExtension) contrib;
            register(locationManagerPlugin);
        }
    }

    private void register(LocationManagerPluginExtension pluginExtension) {
        String locationName = pluginExtension.getLocationName();
        Boolean locationEnabled = pluginExtension.getLocationEnabled();

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
    public void unregisterExtension(Extension extension) throws Exception {
        locations.clear();
        locations = null;
    }

/*    @Override
    public void activate(RuntimeContext context) throws Exception {
        super.activate(context);
    }

    @Override
    public void deactivate(RuntimeContext context) throws Exception {
        super.deactivate(context);
    }
  */
    public Map<String, RepositoryLocation> getAvailableLocations() {
        return locations;
    }

}
