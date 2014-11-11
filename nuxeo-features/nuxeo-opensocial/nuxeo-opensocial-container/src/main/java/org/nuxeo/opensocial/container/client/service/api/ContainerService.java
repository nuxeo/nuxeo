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

package org.nuxeo.opensocial.container.client.service.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.nuxeo.opensocial.container.client.bean.Container;
import org.nuxeo.opensocial.container.client.bean.ContainerServiceException;
import org.nuxeo.opensocial.container.client.bean.GadgetBean;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * ContainerService
 *
 * @author Guillaume Cusnieux
 */
@RemoteServiceRelativePath("gwtcontainer")
public interface ContainerService extends RemoteService {

    /**
     * Retrieves a specific container.
     */
    Container getContainer(Map<String, String> gwtParams)
            throws ContainerServiceException;

    /**
     * Saves layout of container.
     */
    Container saveLayout(Map<String, String> gwtParams, String layout)
            throws ContainerServiceException;

    /**
     * Saves preferences of gadget with form parameter.
     *
     * @param gadget
     * @param form new preferences
     * @param gwtParams
     */
    GadgetBean saveGadgetPreferences(GadgetBean gadget, String form,
            Map<String, String> gwtParams) throws ContainerServiceException;

    /**
     * Removes gadget.
     *
     * @return the removed GadgetBean
     */
    GadgetBean removeGadget(GadgetBean gadget, Map<String, String> gwtParams)
            throws ContainerServiceException;

    /**
     * Adds a gadget.
     *
     * @return the added GadgetBean
     */
    GadgetBean addGadget(String gadgetName, Map<String, String> gwtParams)
            throws ContainerServiceException;

    /**
     * Saves a collection of gadget
     */
    Boolean saveGadgetsCollection(Collection<GadgetBean> beans,
            Map<String, String> gwtParams) throws ContainerServiceException;

    /**
     * Gets collection of gadget name sorted by category.
     *
     * @return a map whose key are the categories and values are lists of gadget name
     */
    Map<String, ArrayList<String>> getGadgetList(Map<String, String> gwtParams)
            throws ContainerServiceException;

    /**
     * Saves a Gadget.
     *
     * @return the saved gadget bean
     */
    GadgetBean saveGadget(GadgetBean gadget, Map<String, String> gwtParams)
            throws ContainerServiceException;

}
