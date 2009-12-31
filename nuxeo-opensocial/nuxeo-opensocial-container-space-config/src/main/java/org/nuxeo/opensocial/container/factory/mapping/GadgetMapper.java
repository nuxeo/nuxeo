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

package org.nuxeo.opensocial.container.factory.mapping;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.shindig.gadgets.spec.View;
import org.nuxeo.ecm.spaces.api.Gadget;
import org.nuxeo.opensocial.container.client.bean.GadgetBean;
import org.nuxeo.opensocial.container.client.bean.GadgetPosition;
import org.nuxeo.opensocial.container.client.bean.GadgetView;
import org.nuxeo.opensocial.container.client.bean.PreferencesBean;
import org.nuxeo.opensocial.container.factory.GadgetManagerImpl;
import org.nuxeo.opensocial.container.factory.PreferenceManager;
import org.nuxeo.opensocial.container.factory.utils.GadgetsUtils;
import org.nuxeo.opensocial.container.factory.utils.UrlBuilder;

public class GadgetMapper extends GadgetBean implements Gadget {

  private Map<String, String> preferences = new HashMap<String, String>();
  private static final Log log = LogFactory.getLog(GadgetMapper.class);
  private static final String BOOL = "BOOL";

  private Integer pos;
  private String placeID;
  private String owner;
  private GadgetBean bean;
  private String title;
  private Boolean collapsed;
  private String ref;
  private GadgetPosition position;
  private List<PreferencesBean> defaultPrefs;
  private List<PreferencesBean> userPrefs;
  private String renderUrl;
  private String viewer;
  private Integer shindigId;
  private boolean permission;
  private String name;
  private String spaceName;
  private int height;
  private String htmlContent;

  /**
   * Constructor for convert GadgetBean to Gadget
   * 
   * @param bean
   */
  public GadgetMapper(GadgetBean bean) {
    this.bean = bean;
    this.title = bean.getTitle();
    this.collapsed = bean.isCollapse();
    this.ref = bean.getRef();
    this.position = bean.getGadgetPosition();
    if (this.position != null) {
      this.placeID = this.position.getPlaceID();
      this.pos = this.position.getPosition();
    }
    this.preferences = createPreferences(bean);
    this.defaultPrefs = bean.getDefaultPrefs();
    this.userPrefs = bean.getUserPrefs();
    this.height = bean.getHeight();
    this.htmlContent = bean.getHtmlContent();
    this.viewer = bean.getViewer();
    this.shindigId = bean.getShindigId();
    this.permission = bean.getPermission();
    this.name = bean.getName();
    this.spaceName = bean.getSpaceName();
    this.renderUrl = updateRenderUrl();
  }

  private String updateRenderUrl() {
    String url = createRenderUrl();
    this.bean.setRenderUrl(url);
    return url;
  }

  /**
   * Constructor for convert Gadget to GadgetBean
   * 
   * @param bean
   */
  public GadgetMapper(Gadget gadget, String viewer, int shindigId,
      boolean permission) {
    this.title = gadget.getTitle();
    this.spaceName = gadget.getName();
    this.name = getRealName(gadget.getName());
    this.collapsed = gadget.isCollapsed();
    this.ref = gadget.getId();
    this.placeID = gadget.getPlaceID();
    this.pos = gadget.getPosition();
    this.position = new GadgetPosition(placeID, pos);
    this.preferences = gadget.getPreferences();
    this.owner = gadget.getOwner();
    this.viewer = viewer;
    this.shindigId = shindigId;
    this.height = gadget.getHeight();
    this.htmlContent = gadget.getHtmlContent();
    this.permission = permission;
    createGadgetBean();
  }

  @Override
  public Boolean getPermission() {
    return permission;
  }

  @Override
  public String getRenderUrl() {
    return renderUrl;
  }

  @Override
  public Integer getShindigId() {
    return shindigId;
  }

  @Override
  public String getTitle() {
    return title;
  }

  @Override
  public List<PreferencesBean> getUserPrefs() {
    return userPrefs;
  }

  @Override
  public String getViewer() {
    return viewer;
  }

  public String getOwner() {
    return owner;
  }

  public String getCategory() {
    return null;
  }

  public String getDescription() {
    return null;
  }

  public String getId() {
    return this.ref;
  }

  public String getName() {
    return name;
  }

  public String getPlaceID() {
    return placeID;
  }

  public int getPosition() {
    return pos;
  }

  public Map<String, String> getPreferences() {
    return preferences;
  }

  public String getType() {
    return null;
  }

  public boolean isCollapsed() {
    return this.collapsed;
  }

  private String getRealName(String name) {
    StringTokenizer st = new StringTokenizer(name, ".");
    return st.nextToken();
  }

  public void setPreferences(Map<String, String> updatePrefs) throws Exception {
    this.preferences = updatePrefs;
    for (PreferencesBean p : userPrefs) {
      updatePrefValue(updatePrefs, p);
    }
    for (PreferencesBean p : defaultPrefs) {
      updatePrefValue(updatePrefs, p);
    }
    this.bean.setRenderUrl(UrlBuilder.buildShindigUrl(this));
    this.bean.setUserPrefs(userPrefs);
  }

  private void updatePrefValue(Map<String, String> updatePrefs,
      PreferencesBean p) throws UnsupportedEncodingException {
    if (updatePrefs.containsKey(p.getName())) {
      String val = URLDecoder.decode(updatePrefs.get(p.getName()), "UTF-8");
      if (p.getDataType()
          .equals(BOOL)) {
        val = "true";
      } else if (p.getDataType()
          .equals(BOOL)) {
        val = "false";
      }
      p.setValue(val);
    }
  }

  private String createRenderUrl() {
    try {
      return UrlBuilder.buildShindigUrl(this);
    } catch (Exception e) {
      log.error(e);
    }
    return null;
  }

  private Map<String, String> createPreferences(GadgetBean b) {
    Map<String, String> prefs = new HashMap<String, String>();
    return buildPreferences(buildPreferences(prefs, b.getUserPrefs()),
        b.getDefaultPrefs());
  }

  private Map<String, String> buildPreferences(Map<String, String> prefs,
      List<PreferencesBean> uPrefs) {
    if (uPrefs != null) {
      for (PreferencesBean p : uPrefs) {
        prefs.put(p.getName(), p.getValue());
      }
    }
    return prefs;
  }

  public void createGadgetBean() {
    this.userPrefs = PreferenceManager.getPreferences(this);
    this.defaultPrefs = PreferenceManager.getDefaultPreferences(this);
    updateTitleInPreference();
    this.bean = new GadgetBean(shindigId, ref, title, viewer, defaultPrefs,
        userPrefs, permission, collapsed, name, spaceName, createGadgetViews(),
        htmlContent, height);
    this.renderUrl = updateRenderUrl();
    this.bean.setRenderUrl(renderUrl);
    this.bean.setPosition(this.position);
  }

  private Map<String, GadgetView> createGadgetViews() {
    Map<String, GadgetView> gv = new HashMap<String, GadgetView>();
    try {
      Map<String, View> views = GadgetsUtils.getGadgetSpec(this)
          .getViews();
      for (String v : views.keySet()) {
        View view = views.get(v);
        gv.put(v, new GadgetView(view.getName(), view.getType()
            .toString()));
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return gv;
  }

  private void updateTitleInPreference() {
    for (PreferencesBean p : this.userPrefs) {
      if (GadgetManagerImpl.TITLE_KEY_PREF.equals(p.getName())
          && p.getValue() != null) {
        this.title = p.getValue();
        return;
      }
    }
  }

  /**
   * Use this method for get GadgetBean in GwtContainer because GadgetMapper
   * isn't serializabel
   * 
   * @return GadgetBean
   */
  public GadgetBean getGadgetBean() {
    return bean;
  }

  public boolean isEqualTo(Gadget gadget) {
    return gadget.getId() != null && gadget.getId()
        .equals(getId());
  }

  @Override
  public void setTitle(String title) {
    super.setTitle(title);
    this.bean.setTitle(title);
  }

  public void setName(String name) {
    this.name = spaceName;
  }

  public int getHeight() {
    return height;
  }

  public String getHtmlContent() {
    return htmlContent;
  }

}
