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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.shindig.common.xml.XmlException;
import org.apache.shindig.common.xml.XmlUtil;
import org.apache.shindig.gadgets.spec.SpecParserException;
import org.apache.shindig.gadgets.spec.UserPref;
import org.apache.shindig.gadgets.spec.UserPref.EnumValuePair;
import org.nuxeo.opensocial.container.client.bean.PreferencesBean;
import org.nuxeo.opensocial.container.client.bean.ValuePair;
import org.nuxeo.opensocial.container.factory.mapping.GadgetMapper;
import org.nuxeo.opensocial.container.factory.utils.GadgetsUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.common.collect.Lists;

public class PreferenceManager {

  static final Log log = LogFactory.getLog(PreferenceManager.class);

  /**
   * Get Default Preferences
   *
   * @param gadget
   * @return ArrayList<PreferencesBean>
   */
  public static ArrayList<PreferencesBean> getDefaultPreferences(
      GadgetMapper gadget) {
    return mergePreferences(DefaultPreference.getPreferences(),
        gadget.getPreferences());
  }

  public static ArrayList<PreferencesBean> getPreferences(GadgetMapper gadget) {
    return mergePreferences(getOpenSocialPreferences(gadget),
        gadget.getPreferences());
  }

  /**
   * Merge preferences of gadget with preferences saving in Nuxeo
   *
   * @param defaultPrefs
   * @param loadPrefs
   * @return rrayList<PreferencesBean>
   */
  protected static ArrayList<PreferencesBean> mergePreferences(
      List<UserPref> defaultPrefs, Map<String, String> loadPrefs) {
    ArrayList<PreferencesBean> prefs = new ArrayList<PreferencesBean>();
    if (loadPrefs != null) {
      for (UserPref pref : defaultPrefs)
        prefs.add(setLoadValue(loadPrefs, buildPrefBean(pref)));
    }
    return prefs;
  }

  private static PreferencesBean buildPrefBean(UserPref p) {
    return new PreferencesBean(p.getDataType()
        .toString(), p.getDefaultValue(), p.getDisplayName(),
        getSerializableEnumValues(p.getOrderedEnumValues()), p.getName(), null);
  }

  protected static PreferencesBean setLoadValue(Map<String, String> prefs,
      PreferencesBean p) {
    for (String name : prefs.keySet()) {
      if (name.equals(p.getName())) {
        p.setValue(prefs.get(name));
        break;
      }
    }
    return p;
  }

  /**
   * Get default preferences with OpensocialService (Parse XML gadget)
   *
   * @param gadget
   * @return  List<UserPref>
   */
  protected static List<UserPref> getOpenSocialPreferences(GadgetMapper gadget) {
    try {
      return GadgetsUtils.getGadgetSpec(gadget)
          .getUserPrefs();
    } catch (Exception e) {
      log.error(e);
      return new ArrayList<UserPref>();
    }
  }

  private static List<ValuePair> getSerializableEnumValues(
      List<EnumValuePair> orderedEnumValues) {
    List<ValuePair> values = new LinkedList<ValuePair>();
    for (EnumValuePair vPair : orderedEnumValues)
      values.add(new ValuePair(vPair.getValue(), vPair.getDisplayValue()));
    return values;
  }

}

class DefaultPreference {

  private static List<UserPref> defaultPreferences = null;
  private static final String NAME = "default-preferences.xml";

  static List<UserPref> getPreferences() {
    if (defaultPreferences == null) {
      try {
        loadAndParse();
      } catch (Exception e) {
        PreferenceManager.log.error(e);
      }
    }
    return defaultPreferences;
  }

  private static void loadAndParse() throws SpecParserException, IOException {

    BufferedReader br = new BufferedReader(new InputStreamReader(
        Thread.currentThread()
            .getContextClassLoader()
            .getResourceAsStream(NAME)));
    StringBuilder sb = new StringBuilder();
    String line;
    while ((line = br.readLine()) != null) {
      sb.append(line);
    }
    Element doc;
    try {
      doc = XmlUtil.parse(sb.toString());
    } catch (XmlException e) {
      throw new SpecParserException("Malformed XML in file " + NAME, e);
    }

    NodeList children = doc.getChildNodes();
    List<UserPref> userPrefs = Lists.newLinkedList();
    for (int i = 0, j = children.getLength(); i < j; ++i) {
      Node child = children.item(i);
      if (!(child instanceof Element)) {
        continue;
      }
      Element element = (Element) child;
      if ("UserPref".equals(element.getTagName())) {
        UserPref pref = new UserPref(element);
        userPrefs.add(pref);
      }
    }

    defaultPreferences = userPrefs;
  }
}
