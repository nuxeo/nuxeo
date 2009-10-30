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
import java.util.Collections;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.spaces.api.Gadget;
import org.nuxeo.ecm.spaces.api.Space;
import org.nuxeo.ecm.spaces.api.SpaceManager;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceException;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceNotFoundException;
import org.nuxeo.opensocial.container.client.bean.Container;
import org.nuxeo.opensocial.container.client.bean.GadgetBean;
import org.nuxeo.opensocial.container.factory.api.ContainerManager;
import org.nuxeo.opensocial.container.factory.mapping.GadgetMapper;
import org.nuxeo.opensocial.container.factory.utils.CoreSessionHelper;
import org.nuxeo.opensocial.container.factory.utils.PermissionHelper;
import org.nuxeo.opensocial.gadgets.service.api.GadgetService;
import org.nuxeo.runtime.api.Framework;

import com.ibm.icu.util.StringTokenizer;

public class ContainerManagerImpl implements ContainerManager {

	private static final Log log = LogFactory
			.getLog(ContainerManagerImpl.class);

	/**
	 * Constant of default container params key
	 */
	public static final String DOC_REF = "docRef";
	public static final String REPO_NAME = "repoName";

	private static final String LAYOUT_PREFIX = "x-";
	private static final String LAYOUT_SEPARATOR = "-";
	public static final int DEFAULT_STRUCTURE = 3;
	public static final String DEFAULT_LAYOUT = LAYOUT_PREFIX
			+ DEFAULT_STRUCTURE + LAYOUT_SEPARATOR + "default";

	private int shindigId = 0;

	public Container createContainer(Map<String, String> containerParams)
			throws ClientException {
		String spaceId = getParamValue(DOC_REF, containerParams, true, null);
		String repositoryName = getParamValue(REPO_NAME, containerParams,
				false, CoreSessionHelper.DEFAULT_REPOSITORY_NAME);
		CoreSession session = getCoreSession(containerParams);
		if (session != null) {
			try {
				return createContainer(Framework.getService(SpaceManager.class)
						.getSpaceFromId(spaceId, session), session);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			log.error("Unable to get core session from repository name '"
					+ repositoryName + "'.");
		}
		return null;
	}

	protected CoreSession getCoreSession(Map<String, String> containerParams)
			throws ClientException {
		try {
			return CoreSessionHelper.getCoreSession(containerParams
					.get(REPO_NAME));
		} catch (Exception e) {
			e.printStackTrace();
			throw new ClientException();
		}
	}

	/**
	 * 
	 * @param key
	 * @param containerParams
	 * @param required
	 * @param defaultValue
	 * @return
	 */
	private String getParamValue(String key,
			Map<String, String> containerParams, boolean required,
			String defaultValue) {
		String value = containerParams.get(key);
		String retour = null;
		if (value == null) {
			if (required)
				throw new RuntimeException("Container param for key '" + key
						+ "' is required");
			else
				retour = defaultValue;
		} else
			retour = value;
		return retour;
	}

	private int getStructure(Space space) {
		int structure = DEFAULT_STRUCTURE;
		if (space.getLayout() != null) {
			try {
				StringTokenizer st = new StringTokenizer(space.getLayout(),
						LAYOUT_SEPARATOR);
				if (st.hasMoreTokens()) {
					st.nextToken();
					structure = Integer.parseInt(st.nextToken());
				}

			} catch (NumberFormatException nfe) {
			}
		}
		return structure;
	}

	/**
	 * Add Gadget to Container
	 * 
	 * @param gadgetName
	 *            : Name of gadget
	 * @param gwtParams
	 *            : Container parameters
	 * @return GadgetBean
	 */
	public GadgetBean addGadget(final String gadgetName,
			Map<String, String> gwtParams) throws ClientException {

		String spaceId = getParamValue(DOC_REF, gwtParams, true, null);
		CoreSession session = getCoreSession(gwtParams);
		if (session != null) {

			SpaceManager service;
			try {
				service = Framework.getService(SpaceManager.class);
				Gadget g = new Gadget() {

					public String getCategory() {
						return null;
					}

					public String getDescription() {
						return null;
					}

					public String getId() {
						return gadgetName;
					}

					public String getName() {
						return gadgetName;
					}

					public String getOwner() {
						return null;
					}

					public String getPlaceID() {
						return null;
					}

					public int getPosition() {
						return 0;
					}

					public Map<String, String> getPreferences() {
						return null;
					}

					public String getTitle() {
						return gadgetName;
					}

					public boolean isCollapsed() {
						return false;
					}

					public boolean isEqualTo(Gadget gadget) {
						return gadget.getId() != null
								&& gadget.getId().equals(getId());
					}

				};

				Gadget createGadget = service.createGadget(g, service
						.getSpaceFromId(spaceId, session), session);

				return new GadgetMapper(createGadget, session.getPrincipal()
						.getName(), shindigId++, PermissionHelper.canWrite(
						spaceId, session)).getGadgetBean();
			} catch (Exception e) {
				throw new ClientException();
			}
		}
		return null;
	}

	/**
	 * Get a list of gadget
	 * 
	 * @return Map of gadgets, key is category and value is list of gadget name
	 */
	public Map<String, ArrayList<String>> getGadgetList()
			throws ClientException {
		try {
			return Framework.getService(GadgetService.class)
					.getGadgetNameByCategory();
		} catch (Exception e) {
			throw new ClientException();
		}
	}

	public Container saveLayout(Map<String, String> containerParams,
			final String layout) throws ClientException {

		CoreSession session = getCoreSession(containerParams);
		if (session != null) {
			String spaceId = getParamValue(DOC_REF, containerParams, true, null);
			Space space = createUpdateSpace(spaceId, session, layout);
			return createContainer(space, session);
		}
		return null;
	}

	private Container createContainer(Space space, CoreSession session) {
		try {
			if (space != null) {
				ArrayList<GadgetBean> gadgets = new ArrayList<GadgetBean>();
				Boolean perm = PermissionHelper
						.canWrite(space.getId(), session);
				for (Gadget g : Framework.getService(SpaceManager.class)
						.getGadgetsForSpace(space, session)) {
					gadgets.add(new GadgetMapper(g, session.getPrincipal()
							.getName(), shindigId++, perm).getGadgetBean());
				}
				Collections.sort(gadgets);
				String layout = space.getLayout();
				if (layout == null || layout.equals(""))
					layout = DEFAULT_LAYOUT;

				return new Container(gadgets, getStructure(space), layout, perm);
			}
		} catch (SpaceException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	private Space createUpdateSpace(String spaceId, CoreSession session,
			final String layout) {
		try {
			final Space space = Framework.getService(SpaceManager.class)
					.getSpaceFromId(spaceId, session);
			Space spaceUpdate = new Space() {

				public String getCategory() {
					return space.getCategory();
				}

				public String getDescription() {
					return space.getDescription();
				}

				public String getId() {
					return space.getId();
				}

				public String getLayout() {
					return layout;
				}

				public String getName() {
					return space.getName();
				}

				public String getOwner() {
					return space.getOwner();
				}

				public String getTheme() {
					return space.getTheme();
				}

				public String getTitle() {
					return space.getTitle();
				}

				public boolean isEqualTo(Space space) {
					return space.isEqualTo(space);
				}

			};
			return Framework.getService(SpaceManager.class).updateSpace(
					spaceUpdate, session);

		} catch (SpaceNotFoundException e) {
			log.error(e);
		} catch (SpaceException e) {
			log.error(e);
		} catch (Exception e) {
			log.error(e);
		}
		return null;
	}

}
