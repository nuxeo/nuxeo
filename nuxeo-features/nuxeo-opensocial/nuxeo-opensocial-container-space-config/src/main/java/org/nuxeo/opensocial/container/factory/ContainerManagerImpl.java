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
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.spaces.api.Gadget;
import org.nuxeo.ecm.spaces.api.Space;
import org.nuxeo.ecm.spaces.api.SpaceManager;
import org.nuxeo.opensocial.container.client.bean.Container;
import org.nuxeo.opensocial.container.client.bean.GadgetBean;
import org.nuxeo.opensocial.container.factory.api.ContainerManager;
import org.nuxeo.opensocial.container.factory.utils.CoreSessionHelper;
import org.nuxeo.opensocial.gadgets.service.api.GadgetService;
import org.nuxeo.runtime.api.Framework;

import com.ibm.icu.util.StringTokenizer;

public class ContainerManagerImpl implements ContainerManager {

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

    private static final Object NX_BASE_URL = "nxBaseUrl";

    protected SpaceManager spaceManager() throws Exception {
        return Framework.getService(SpaceManager.class);
    }

    public Container createContainer(Map<String, String> containerParams)
            throws ClientException {
        try {
            String spaceId = getParamValue(DOC_REF, containerParams, true, null);
            Space space = spaceManager().getSpaceFromId(spaceId,
                    getCoreSession(containerParams));
            return createContainer(space, getLocale(containerParams),
                    getServerBase(containerParams));
        } catch (Exception e) {
            throw new ClientException("Space not found");
        }

    }

    protected static String getLocale(Map<String, String> params) {
        if (params.containsKey("locale"))
            return params.get("locale");
        return "ALL";
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

    private int getStructure(Space space) throws ClientException {
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
     * @param gadgetName : Name of gadget
     * @param gwtParams : Container parameters
     * @return GadgetBean
     */
    public GadgetBean addGadget(final String gadgetName,
            Map<String, String> gwtParams) throws Exception {

        String spaceId = getParamValue(DOC_REF, gwtParams, true, null);
        Space space;
        try {
            space = spaceManager().getSpaceFromId(spaceId,
                    getCoreSession(gwtParams));
        } catch (Exception e) {
            throw new ClientException("Space not found");
        }
        Gadget createGadget = space.createGadget(gadgetName);
        space.save();

        return GadgetFactory.getGadgetBean(createGadget, getPermissions(space),
                getLocale(gwtParams), getServerBase(gwtParams));

    }

    private String getServerBase(Map<String, String> gwtParams) {
        return gwtParams.get(NX_BASE_URL);
    }

    /**
     * Get a list of gadget
     * 
     * @return Map of gadgets, key is category and value is list of gadget name
     */
    public Map<String, ArrayList<String>> getGadgetList()
            throws ClientException {
        try {
            return Framework.getService(GadgetService.class).getGadgetNameByCategory();
        } catch (Exception e) {
            throw new ClientException();
        }
    }

    protected CoreSession getCoreSession(Map<String, String> gwtParams)
            throws Exception {
        return CoreSessionHelper.getCoreSession(gwtParams.get(ContainerManagerImpl.REPO_NAME));
    }

    public Container saveLayout(Map<String, String> containerParams,
            final String layout) throws ClientException {

        String spaceId = getParamValue(DOC_REF, containerParams, true, null);
        Space space;
        try {
            space = spaceManager().getSpaceFromId(spaceId,
                    getCoreSession(containerParams));

        } catch (Exception e) {
            throw new ClientException("Space not found");
        }
        space.setLayout(layout);
        space.save();
        return createContainer(space, getLocale(containerParams),
                getServerBase(containerParams));
    }

    private Container createContainer(Space space, String locale,
            String serverBase) {
        try {
            if (space != null) {
                ArrayList<GadgetBean> gadgets = new ArrayList<GadgetBean>();

                List<String> perms = getPermissions(space);

                for (Gadget g : space.getGadgets()) {
                    gadgets.add(GadgetFactory.getGadgetBean(g, perms, locale,
                            serverBase));
                }
                Collections.sort(gadgets);
                String layout = space.getLayout();
                if (layout == null || layout.equals(""))
                    layout = DEFAULT_LAYOUT;

                return new Container(gadgets, getStructure(space), layout,
                        perms, space.getId());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    static List<String> getPermissions(Space space) throws Exception {
        return space.isReadOnly() ? new ArrayList<String>()
                : space.getPermissions();
    }
}
