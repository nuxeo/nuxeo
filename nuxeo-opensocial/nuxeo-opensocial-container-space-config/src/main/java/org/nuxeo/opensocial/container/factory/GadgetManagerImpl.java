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

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.spaces.api.Gadget;
import org.nuxeo.ecm.spaces.api.Space;
import org.nuxeo.ecm.spaces.api.SpaceManager;
import org.nuxeo.opensocial.container.client.bean.GadgetBean;
import org.nuxeo.opensocial.container.factory.api.GadgetManager;
import org.nuxeo.opensocial.container.factory.utils.CoreSessionHelper;
import org.nuxeo.runtime.api.Framework;

public class GadgetManagerImpl implements GadgetManager {

    private static final Log log = LogFactory.getLog(GadgetManagerImpl.class);

    private static final Object NX_BASE_URL = "nxBaseUrl";

    protected SpaceManager spaceManager() throws Exception {
        return Framework.getService(SpaceManager.class);
    }

    /**
     * Remove gadget to container
     * 
     * @param bean : Gadget to delete
     * @param gwtParams : container paramters
     */
    public void removeGadget(GadgetBean bean, Map<String, String> gwtParams)
            throws ClientException {
        try {
            String spaceId = getParamValue(ContainerManagerImpl.DOC_REF,
                    gwtParams, true, null);
            Space space = spaceManager().getSpaceFromId(spaceId,
                    getCoreSession(gwtParams));
            space.remove(GadgetFactory.getGadget(bean));
            space.save();
        } catch (Exception e) {
            log.error(e);
            throw new ClientException(e);
        }

    }

    protected CoreSession getCoreSession(Map<String, String> gwtParams)
            throws Exception {
        return CoreSessionHelper.getCoreSession(gwtParams.get(ContainerManagerImpl.REPO_NAME));
    }

    public GadgetBean saveGadget(GadgetBean bean, Map<String, String> gwtParams) {
        try {
            String spaceId = getParamValue(ContainerManagerImpl.DOC_REF,
                    gwtParams, true, null);
            Space space = spaceManager().getSpaceFromId(spaceId,
                    getCoreSession(gwtParams));

            space.save(GadgetFactory.getGadget(bean));

        } catch (Exception e) {
            log.error(e);
        }
        return bean;
    }

    /**
     * Save gadget preferences and update render url of gadget
     * 
     */
    public GadgetBean savePreferences(GadgetBean bean,
            Map<String, String> updatePrefs, Map<String, String> gwtParams)
            throws Exception {
        try {
            Space space = getCurrentSpace(gwtParams);
            String serverBase = getServerBase(gwtParams);
            Gadget gadget = GadgetFactory.getGadget(bean);
            if (updatePrefs != null)
                gadget.setPreferences(updatePrefs);
            space.save(gadget);
            return GadgetFactory.getGadgetBean(space.getGadget(gadget.getId()),
                    ContainerManagerImpl.getPermissions(space),
                    ContainerManagerImpl.getLocale(gwtParams), serverBase);
        } catch (Exception e) {
            log.error("GadgetManagerImpl - savePreferences : "
                    + e.fillInStackTrace());
        }
        return bean;

    }

    private String getServerBase(Map<String, String> gwtParams) {
        return gwtParams.get(NX_BASE_URL);
    }

    private Space getCurrentSpace(Map<String, String> gwtParams)
            throws Exception {
        CoreSession session = getCoreSession(gwtParams);

        String spaceId = getParamValue(ContainerManagerImpl.DOC_REF, gwtParams,
                true, null);
        return spaceManager().getSpaceFromId(spaceId, session);

    }

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

}
