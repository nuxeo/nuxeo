/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.launcher.connect.fake;

import org.nuxeo.connect.connector.fake.AbstractFakeConnector;
import org.nuxeo.connect.update.PackageType;

public class LocalConnectFakeConnector extends AbstractFakeConnector {

    protected String addonJSON;

    protected String hotfixJSON;

    protected String studioJSON;

    public LocalConnectFakeConnector(String addonJSON, String hotfixJSON, String studioJSON) {
        super();
        this.addonJSON = addonJSON;
        this.hotfixJSON = hotfixJSON;
        this.studioJSON = studioJSON;
    }

    @Override
    protected String getJSONDataForStatus() {

        return "{ contractStatus : 'active', endDate : '10/12/2020'}";
    }

    @Override
    protected String getJSONDataForDownloads(String type) {
        String data = null;
        if (PackageType.ADDON.getValue().equals(type)) {
            data = addonJSON;
        } else if (PackageType.HOT_FIX.getValue().equals(type)) {
            data = hotfixJSON;
        } else if (PackageType.STUDIO.getValue().equals(type)) {
            data = studioJSON;
        } else {
            data = "[]";
        }
        return data;
    }

}
