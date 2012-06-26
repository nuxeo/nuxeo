/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 * Contributors:
 *     Stéphane Fourrier
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.opensocial.container.server.webcontent.gadgets.opensocial;

import static org.nuxeo.ecm.spaces.api.Constants.WC_OPEN_SOCIAL_GADGET_DEF_URL_PROPERTY;
import static org.nuxeo.ecm.spaces.api.Constants.WC_OPEN_SOCIAL_GADGET_NAME;
import static org.nuxeo.ecm.spaces.api.Constants.WC_OPEN_SOCIAL_USER_PREFS_PROPERTY;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetSpecFactory;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.ModulePrefs;
import org.apache.shindig.gadgets.spec.UserPref;
import org.apache.shindig.gadgets.spec.UserPref.EnumValuePair;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.opensocial.container.server.utils.UrlBuilder;
import org.nuxeo.opensocial.container.server.webcontent.abs.AbstractWebContentAdapter;
import org.nuxeo.opensocial.container.shared.webcontent.OpenSocialData;
import org.nuxeo.opensocial.container.shared.webcontent.enume.DataType;
import org.nuxeo.opensocial.helper.OpenSocialGadgetHelper;
import org.nuxeo.opensocial.service.api.OpenSocialService;
import org.nuxeo.runtime.api.Framework;

/**
 * @author Stéphane Fourrier
 */
public class OpenSocialAdapter extends
        AbstractWebContentAdapter<OpenSocialData> {

    private static final Log log = LogFactory.getLog(OpenSocialAdapter.class);

    public static final String SEPARATOR = "/";

    public OpenSocialAdapter(DocumentModel doc) {
        super(doc);
    }

    public void setGadgetDefUrl(String gadgetDefUrl) throws ClientException {
        doc.setPropertyValue(WC_OPEN_SOCIAL_GADGET_DEF_URL_PROPERTY,
                gadgetDefUrl);
    }

    public void setGadgetName(String gadgetDefUrl) throws ClientException {
        doc.setPropertyValue(WC_OPEN_SOCIAL_GADGET_NAME, gadgetDefUrl);
    }

    @SuppressWarnings("unchecked")
    public void feedFrom(OpenSocialData data) throws ClientException {
        super.setMetadataFrom(data);

        String gadgetDefUrl = OpenSocialGadgetHelper.computeGadgetDefUrlBeforeSave(data.getGadgetDef());

        setGadgetDefUrl(gadgetDefUrl);
        setGadgetName(data.getGadgetName());

        Map<String, Map<String, Serializable>> tmpMap = new LinkedHashMap<String, Map<String, Serializable>>();

        List<Map<String, Serializable>> savedUserPrefs = (List<Map<String, Serializable>>) doc.getPropertyValue(WC_OPEN_SOCIAL_USER_PREFS_PROPERTY);

        for (Map<String, Serializable> entry : savedUserPrefs) {
            tmpMap.put((String) entry.get("name"), entry);
        }
        // add the additional preferences as user preferences
        Map<String, String> additionalPreferences = data.getAdditionalPreferences();
        for (Entry<String, String> entry : additionalPreferences.entrySet()) {
            Map<String, Serializable> savedUserPref = new HashMap<String, Serializable>();
            savedUserPref.put("name", entry.getKey());
            savedUserPref.put("value", entry.getValue());
            savedUserPrefs.add(savedUserPref);
            tmpMap.put((String) savedUserPref.get("name"), savedUserPref);
        }

        for (org.nuxeo.opensocial.container.shared.webcontent.UserPref dataPref : data.getUserPrefs()) {
            if (dataPref.getActualValue() != null) {
                Map<String, Serializable> savedUserPref = new HashMap<String, Serializable>();
                savedUserPref.put("name", dataPref.getName());
                savedUserPref.put("value", dataPref.getActualValue());

                savedUserPrefs.add(savedUserPref);
                tmpMap.put((String) savedUserPref.get("name"), savedUserPref);
            }
        }
        savedUserPrefs.clear();
        savedUserPrefs.addAll(tmpMap.values());

        doc.setPropertyValue(WC_OPEN_SOCIAL_USER_PREFS_PROPERTY,
                (Serializable) savedUserPrefs);

        setFrameUrlFor(data);
    }

    private void setFrameUrlFor(OpenSocialData data) throws ClientException {
        data.computeFrameUrl();
    }

    @SuppressWarnings("unchecked")
    public OpenSocialData getData() throws ClientException {
        OpenSocialData data = new OpenSocialData();
        super.getMetadataFor(data);

        String gadgetDefUrl = OpenSocialGadgetHelper.computeGadgetDefUrlAfterLoad((String) doc.getPropertyValue(WC_OPEN_SOCIAL_GADGET_DEF_URL_PROPERTY));
        data.setGadgetDef(gadgetDefUrl);

        data.setGadgetName((String) doc.getPropertyValue(WC_OPEN_SOCIAL_GADGET_NAME));

        // We get the values from nuxeo for each saved preference
        List<Map<String, Serializable>> tempSavedUserPrefs = (List<Map<String, Serializable>>) doc.getPropertyValue(WC_OPEN_SOCIAL_USER_PREFS_PROPERTY);
        Map<String, String> savedUserPrefs = new HashMap<String, String>();

        for (Map<String, Serializable> preference : tempSavedUserPrefs) {
            String name = (String) preference.get("name");
            String value = (String) preference.get("value");
            savedUserPrefs.put(name, value);
        }

        // Get Preferences from shindig and wrap them into the data
        // We don't use the class provided by shindig because of not
        // implemented classes in GWT

        GadgetSpec gadgetSpec = getGadgetSpec(data);

        if (gadgetSpec != null) {

            List<org.nuxeo.opensocial.container.shared.webcontent.UserPref> dataUserPrefs = new ArrayList<org.nuxeo.opensocial.container.shared.webcontent.UserPref>();

            for (UserPref openSocialUserPref : gadgetSpec.getUserPrefs()) {
                org.nuxeo.opensocial.container.shared.webcontent.UserPref dataPref = new org.nuxeo.opensocial.container.shared.webcontent.UserPref(
                        openSocialUserPref.getName(),
                        DataType.valueOf(openSocialUserPref.getDataType().toString()));

                Map<String, String> enumValues = new LinkedHashMap<String, String>();

                for (EnumValuePair osprefValue : openSocialUserPref.getOrderedEnumValues()) {
                    enumValues.put(osprefValue.getDisplayValue(),
                            osprefValue.getValue());
                }

                dataPref.setEnumValues(enumValues);
                dataPref.setDisplayName(openSocialUserPref.getDisplayName());
                dataPref.setDefaultValue(openSocialUserPref.getDefaultValue());

                String name = dataPref.getName();

                if (savedUserPrefs.containsKey(name)) {
                    dataPref.setActualValue(savedUserPrefs.get(name));
                }

                dataUserPrefs.add(dataPref);
            }

            data.setUserPrefs(dataUserPrefs);

            Map<String, String> modulePreferences = buildModulePreferences(gadgetSpec);
            data.setModulePreferences(modulePreferences);
        }

        setFrameUrlFor(data);

        return data;
    }

    protected Map<String, String> buildModulePreferences(GadgetSpec gadgetSpec) {
        Map<String, String> modulePreferencesMap = new HashMap<String, String>();
        ModulePrefs modulePrefs = gadgetSpec.getModulePrefs();
        modulePreferencesMap.put("title", modulePrefs.getTitle());
        modulePreferencesMap.put("description", modulePrefs.getDescription());
        modulePreferencesMap.put("author", modulePrefs.getAuthor());
        modulePreferencesMap.put("author_email", modulePrefs.getAuthorEmail());
        modulePreferencesMap.put("width", modulePrefs.getAttribute("width"));
        modulePreferencesMap.put("height", modulePrefs.getAttribute("height"));
        return modulePreferencesMap;
    }

    private GadgetSpec getGadgetSpec(OpenSocialData data) {
        try {
            OpenSocialService service = Framework.getService(OpenSocialService.class);
            GadgetSpecFactory gadgetSpecFactory = service.getGadgetSpecFactory();
            NXGadgetContext context = new NXGadgetContext(data.getGadgetDef());
            return gadgetSpecFactory.getGadgetSpec(context);
        } catch (Exception e) {
            log.error("Unable to get gadget spec for " + data.getName(), e);
            return null;
        }
    }

}

class NXGadgetContext extends GadgetContext {
    protected String url;

    public NXGadgetContext(String url) {
        super();
        this.url = url;
    }

    @Override
    public Uri getUrl() {
        return Uri.parse(url);
    }

    @Override
    public boolean getIgnoreCache() {
        return false;
    }

    @Override
    public String getContainer() {
        return "default";
    }
}
