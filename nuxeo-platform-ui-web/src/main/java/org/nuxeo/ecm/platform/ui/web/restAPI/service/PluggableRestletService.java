/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.restAPI.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.restlet.Restlet;

/**
 * Runtime service used to register restlets
 *
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 */
public class PluggableRestletService extends DefaultComponent {

    public static final String NAME = "org.nuxeo.ecm.platform.ui.web.restAPI.service.PluggableRestletService";

    private static final Log log = LogFactory.getLog(PluggableRestletService.class);

    private Map<String, RestletPluginDescriptor> restletsDescriptors;

    @Override
    public void activate(ComponentContext context) {
        restletsDescriptors = new HashMap<String, RestletPluginDescriptor>();
    }

    @Override
    public void deactivate(ComponentContext context) {
        restletsDescriptors = null;
    }

    private void mergeDescriptors(RestletPluginDescriptor newContrib) {
        RestletPluginDescriptor oldDescriptor = restletsDescriptors.get(newContrib.getName());

        // Enable/Disable
        if (newContrib.getEnabled() != null) {
            oldDescriptor.setEnabled(newContrib.getEnabled());
        }

        // override URL
        if (newContrib.getUrlPatterns() != null
                && !newContrib.getUrlPatterns().isEmpty()) {
            oldDescriptor.getUrlPatterns().addAll(newContrib.getUrlPatterns());
        }

        // override class (NXP-170)
        if (newContrib.getClassName() != null) {
            oldDescriptor.setClassName(newContrib.getClassName());
        }
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {

        RestletPluginDescriptor descriptor = (RestletPluginDescriptor) contribution;

        if (restletsDescriptors.containsKey(descriptor.getName())) {
            mergeDescriptors(descriptor);
            log.debug("merged RestletDescriptor: "
                    + descriptor.getName());
        } else {
            restletsDescriptors.put(descriptor.getName(), descriptor);
            log.debug("registered RestletDescriptor: "
                    + descriptor.getName());
        }
    }

    public List<String> getContributedRestletNames() {
        List<String> res = new ArrayList<String>();

        res.addAll(restletsDescriptors.keySet());
        return res;
    }

    public RestletPluginDescriptor getContributedRestletDescriptor(String name) {
        if (restletsDescriptors.containsKey(name)) {
            return restletsDescriptors.get(name);
        } else {
            return null;
        }
    }

    public Restlet getContributedRestletByName(String name) {
        if (restletsDescriptors.containsKey(name)) {

            RestletPluginDescriptor rpd = restletsDescriptors.get(name);
            if (rpd == null) {
                log.error("Error while creating Restlet instance. Cannot get RestletPluginDescriptor for name: "
                        + name);
                return null;
            }
            Class<Restlet> theClass = rpd.getClassName();
            if (theClass == null) {
                log.error("Error while creating Restlet instance. Class not available for restlet descriptor: "
                        + name);
                return null;
            }
            Restlet restlet;
            try {
                restlet = theClass.newInstance();
            } catch (InstantiationException e) {
                log.error("Error while creating Restlet instance : " + e.getMessage());
                return null;
            } catch (IllegalAccessException e) {
                log.error("Error while creating Restlet instance : " + e.getMessage());
                return null;
            }
            return restlet;
        } else {
            log.error("Restlet " + name + " is not registred");
            return null;
        }
    }

}
