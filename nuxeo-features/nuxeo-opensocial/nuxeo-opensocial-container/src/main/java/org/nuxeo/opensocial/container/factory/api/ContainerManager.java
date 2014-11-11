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

import java.util.ArrayList;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.opensocial.container.client.bean.Container;
import org.nuxeo.opensocial.container.client.bean.GadgetBean;

public interface ContainerManager {

  Container createContainer(Map<String, String> gwtParams)
      throws ClientException;

  GadgetBean addGadget(String gadgetName, Map<String, String> gwtParams)
      throws ClientException;

  Map<String, ArrayList<String>> getGadgetList() throws ClientException;

  Container saveLayout(Map<String, String> gwtParams, String layout)
      throws ClientException;

}
