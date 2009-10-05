/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Leroy Merlin (http://www.leroymerlin.fr/) - initial implementation
 */

package org.nuxeo.opensocial.container.factory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.shindig.gadgets.GadgetSpecFactory;
import org.apache.shindig.gadgets.spec.UserPref;
import org.apache.shindig.gadgets.spec.UserPref.EnumValuePair;
import org.nuxeo.opensocial.container.client.bean.PreferencesBean;
import org.nuxeo.opensocial.container.client.bean.ValuePair;
import org.nuxeo.opensocial.container.factory.mapping.GadgetMapper;
import org.nuxeo.opensocial.container.factory.utils.NxGadgetContext;
import org.nuxeo.opensocial.container.factory.utils.UrlBuilder;
import org.nuxeo.opensocial.service.api.OpenSocialService;
import org.nuxeo.runtime.api.Framework;

public class PreferenceManager {

    private static final Log log = LogFactory.getLog(PreferenceManager.class);

    /**
     * Get User Preferences
     *
     * @param gadget
     * @return ArrayList<PreferencesBean>
     */
    public static ArrayList<PreferencesBean> getUserPreferences(
            GadgetMapper gadget) {
        return mergePreferences(getDefaultPreferences(gadget), gadget
                .getPreferences());
    }

    /**
     * Merge default preferences of gadget with preferences saving in Nuxeo
     *
     * @param defaultPrefs
     * @param loadPrefs
     * @return rrayList<PreferencesBean>
     */
    protected static ArrayList<PreferencesBean> mergePreferences(
            ArrayList<PreferencesBean> defaultPrefs,
            Map<String, String> loadPrefs) {
        ArrayList<PreferencesBean> prefs = new ArrayList<PreferencesBean>();
        if (defaultPrefs != null && loadPrefs != null) {
            for (PreferencesBean defaultPref : defaultPrefs) {
                prefs.add(setLoadValue(loadPrefs, defaultPref));
            }
        }
        return prefs;
    }

    protected static PreferencesBean setLoadValue(Map<String, String> prefs,
            PreferencesBean pref) {
        for (String name : prefs.keySet()) {
            if (name.equals(pref.getName())) {
                pref.setValue(prefs.get(name));
                break;
            }
        }
        return pref;
    }

    /**
     * Get default preferences with OpensocialService (Parse XML gadget)
     *
     * @param gadget
     * @return ArrayList<PreferencesBean>
     */
    protected static ArrayList<PreferencesBean> getDefaultPreferences(
            GadgetMapper gadget) {
        try {
            OpenSocialService service = Framework
                    .getService(OpenSocialService.class);

            String gadgetDef = UrlBuilder.getGadgetDef(gadget.getName());

            GadgetSpecFactory gadgetSpecFactory = service
                    .getGadgetSpecFactory();

            NxGadgetContext context = new NxGadgetContext(gadgetDef, gadget
                    .getViewer(), gadget.getOwner());

            List<UserPref> userPrefs = gadgetSpecFactory.getGadgetSpec(context)
                    .getUserPrefs();
            
            ArrayList<PreferencesBean> preferences = new ArrayList<PreferencesBean>();
            for (UserPref pref : userPrefs) {
                preferences.add(new PreferencesBean(pref.getDataType()
                        .toString(), pref.getDefaultValue(), pref
                        .getDisplayName(), getSerializableEnumValues(pref
                        .getOrderedEnumValues()), pref.getName(), null));
            }
            return preferences;
        } catch (Exception e) {
            log.error(e);
        }
        return null;
    }

    private static List<ValuePair> getSerializableEnumValues(
            List<EnumValuePair> orderedEnumValues) {
        List<ValuePair> values = new LinkedList<ValuePair>();
        for (EnumValuePair vPair : orderedEnumValues)
            values
                    .add(new ValuePair(vPair.getValue(), vPair
                            .getDisplayValue()));
        return values;
    }

}
