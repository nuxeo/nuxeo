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

package org.nuxeo.opensocial.container.factory.api;

import java.util.Collection;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.opensocial.container.client.bean.GadgetBean;

/**
 * @author Guillaume Cusnieux
 */
public interface GadgetManager {

    void removeGadget(GadgetBean gadget, Map<String, String> gwtParams)
            throws ClientException;

    GadgetBean savePreferences(GadgetBean gadget,
            Map<String, String> updatePrefs, Map<String, String> gwtParams)
            throws Exception;

    GadgetBean saveGadget(GadgetBean gadget, Map<String, String> gwtParams)
            throws ClientException;

    Boolean validateGadgets(Collection<GadgetBean> beans, Map<String, String> gwtParams)
            throws ClientException;

}
