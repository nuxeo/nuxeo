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

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.spaces.api.Gadget;
import org.nuxeo.opensocial.container.client.bean.GadgetBean;
import org.nuxeo.opensocial.container.client.bean.GadgetPosition;
import org.nuxeo.opensocial.container.client.bean.PreferencesBean;
import org.nuxeo.opensocial.container.factory.GadgetManagerImpl;
import org.nuxeo.opensocial.container.factory.PreferenceManager;
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
	private List<PreferencesBean> userPrefs;
	private String renderUrl;
	private String viewer;
	private Integer shindigId;
	private boolean permission;
	private String name;
	private String spaceName;

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
		this.userPrefs = bean.getUserPrefs();
		this.renderUrl = bean.getRenderUrl();
		this.viewer = bean.getViewer();
		this.shindigId = bean.getShindigId();
		this.permission = bean.getPermission();
		this.name = bean.getName();
		this.spaceName = bean.getSpaceName();
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
		this.permission = permission;
		createGadgetBean();
	}

	@Override
	public Boolean getPermission() {
		return true;
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

	public void setPreferences(Map<String, String> updatePrefs)
			throws Exception {
		preferences = updatePrefs;
		for (PreferencesBean p : userPrefs) {
			if (updatePrefs.containsKey(p.getName())) {
				String val = URLDecoder.decode(updatePrefs.get(p.getName()),
						"UTF-8");
				if (p.getDataType().equals(BOOL)) {
					val = "true";
				} else if (p.getDataType().equals(BOOL)) {
					val = "false";
				}
				p.setValue(val);
			}
		}
		this.bean.setRenderUrl(UrlBuilder.buildShindigUrl(this));
		this.bean.setUserPrefs(userPrefs);
	}

	private String createRenderUrl() {
		try {
			return UrlBuilder.buildShindigUrl(this);
		} catch (Exception e) {
			log.error(e);
		}
		return null;
	}

	private ArrayList<PreferencesBean> createUserPrefs() {
		return PreferenceManager.getUserPreferences(this);
	}

	private HashMap<String, String> createPreferences(GadgetBean bean) {
		HashMap<String, String> prefs = new HashMap<String, String>();
		List<PreferencesBean> uPrefs = bean.getUserPrefs();
		if (uPrefs != null) {
			for (int i = 0; i < uPrefs.size(); i++) {
				PreferencesBean p = bean.getUserPrefs().get(i);
				prefs.put(p.getName(), p.getValue());
			}
		}
		return prefs;
	}

	public void createGadgetBean() {
		this.userPrefs = createUserPrefs();
		updateTitleInPreference();
		bean = new GadgetBean(shindigId, ref, title, viewer, userPrefs,
				permission, collapsed, name, spaceName);
		this.renderUrl = createRenderUrl();
		bean.setRenderUrl(renderUrl);
		bean.setPosition(this.position);
	}

	private void updateTitleInPreference() {
		for (PreferencesBean p : this.userPrefs) {
			if (GadgetManagerImpl.TITLE_KEY_PREF.equals(p.getName())) {
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
		return gadget.getId() != null && gadget.getId().equals(getId());
	}

	@Override
	public void setTitle(String title) {
		super.setTitle(title);
		this.bean.setTitle(title);
	}

	public void setName(String name) {
		this.name = spaceName;
	}

}
