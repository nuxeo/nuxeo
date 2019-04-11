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

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.nuxeo.connect.connector.fake.AbstractFakeConnector;
import org.nuxeo.connect.data.AbstractJSONSerializableData;
import org.nuxeo.connect.data.DownloadablePackage;
import org.nuxeo.connect.data.PackageDescriptor;
import org.nuxeo.connect.identity.LogicalInstanceIdentifier;
import org.nuxeo.connect.update.PackageType;

public class LocalConnectFakeConnector extends AbstractFakeConnector {

    protected String addonJSON;

    protected String hotfixJSON;

    protected String studioJSON;

    protected String addonWithPrivateJSON;

    public LocalConnectFakeConnector(String addonJSON, String hotfixJSON, String studioJSON, String addonWithPrivateJSON) {
        super();
        this.addonJSON = addonJSON;
        this.hotfixJSON = hotfixJSON;
        this.studioJSON = studioJSON;
        this.addonWithPrivateJSON = addonWithPrivateJSON;
    }

    @Override
    protected String getJSONDataForStatus() {

        return "{ contractStatus : 'active', endDate : '10/12/2020'}";
    }

    @Override
    protected String getJSONDataForDownloads(String type) {
        String data = null;
        if (PackageType.ADDON.getValue().equals(type)) {
            if (LogicalInstanceIdentifier.isRegistered()) {
                data = addonWithPrivateJSON;
            } else {
                data = addonJSON;
            }
        } else if (PackageType.HOT_FIX.getValue().equals(type)) {
            data = hotfixJSON;
        } else if (PackageType.STUDIO.getValue().equals(type)) {
            data = studioJSON;
        } else {
            data = "[]";
        }
        return data;
    }

    @Override
    protected String getJSONDataForDownload(String pkgId) {
        Map<DownloadablePackage, String> jsonByPackage = new HashMap<>();
        jsonByPackage.putAll(getJSONbyPackageFromJSON(addonJSON));
        jsonByPackage.putAll(getJSONbyPackageFromJSON(hotfixJSON));
        jsonByPackage.putAll(getJSONbyPackageFromJSON(studioJSON));

        DownloadablePackage foundPkg = jsonByPackage.keySet().stream().filter(
                pkg -> pkg.getId().equals(pkgId)).findFirst().orElse(null);
        return jsonByPackage.get(foundPkg);
    }

    private Map<DownloadablePackage, String> getJSONbyPackageFromJSON(String json) {
        Map<DownloadablePackage, String> result = new HashMap<>();
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject ob = (JSONObject) array.get(i);
                result.put(AbstractJSONSerializableData.loadFromJSON(PackageDescriptor.class, ob), ob.toString());
            }
        } catch (JSONException e) {
            throw new RuntimeException("Unable to parse json", e);
        }
        return result;
    }

}
