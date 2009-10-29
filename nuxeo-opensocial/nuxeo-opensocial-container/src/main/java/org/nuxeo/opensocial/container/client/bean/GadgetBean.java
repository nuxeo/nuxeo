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

package org.nuxeo.opensocial.container.client.bean;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * GadgetBean
 *
 * @author Guillaume Cusnieux
 */
public class GadgetBean implements Comparable<GadgetBean>, IsSerializable {

  private static final long serialVersionUID = 1L;
  private Integer shindigId;
  private String ref;
  private String title;
  private String renderUrl;
  private String viewer;
  private List<PreferencesBean> userPrefs;
  private Boolean permission;
  private GadgetPosition position;
  private Boolean collapsed;
  private String name;
  private String spaceName;

  /**
   * Default construcor (Specification of Gwt)
   */
  public GadgetBean() {
  }

  public Integer getShindigId() {
    return this.shindigId;
  }

  public GadgetBean(Integer shindigId, String ref, String title, String viewer,
      List<PreferencesBean> userPrefs, Boolean permission, Boolean collapsed,
      String name, String spaceName) {
    this.shindigId = shindigId;
    this.ref = ref;
    this.title = title;
    this.userPrefs = userPrefs;
    this.viewer = viewer;
    this.permission = permission;
    this.collapsed = collapsed;
    this.name = name;
    this.spaceName = spaceName;
  }

  public void setRenderUrl(String renderUrl) {
    this.renderUrl = renderUrl;
  }

  public String getTitle() {
    return title;
  }

  public String getSpaceName() {
    return spaceName;
  }

  public String getName() {
    return name;
  }

  public String getRef() {
    return ref;
  }

  public String getRenderUrl() {
    return renderUrl;
  }

  public List<PreferencesBean> getUserPrefs() {
    return userPrefs;
  }

  public void setUserPrefs(List<PreferencesBean> prefs) {
    userPrefs = prefs;
  }

  public String getViewer() {
    return viewer;
  }

  public Boolean getPermission() {
    return permission;
  }

  public Boolean isCollapse() {
    return collapsed;
  }

  public void setCollapse(Boolean collapsed) {
    this.collapsed = collapsed;
  }

  public void setPosition(GadgetPosition position) {
    this.position = position;
  }

  public GadgetPosition getGadgetPosition() {
    return position;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public void setPref(String key, String value) {
    for (PreferencesBean pref : this.userPrefs) {
      if (key.equals(pref.getName())) {
        pref.setValue(value);
        return;
      }
    }
  }

  public int compareTo(GadgetBean o) {
    Integer pos1 = o.getGadgetPosition()
        .getPosition();
    Integer pos2 = this.getGadgetPosition()
        .getPosition();
    if (pos1 > pos2)
      return -1;
    else if (pos1 == pos2)
      return 0;
    return 1;
  }

}
