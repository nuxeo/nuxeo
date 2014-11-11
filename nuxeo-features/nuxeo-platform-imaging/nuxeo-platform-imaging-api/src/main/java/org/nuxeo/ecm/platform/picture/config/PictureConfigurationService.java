/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package org.nuxeo.ecm.platform.picture.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.picture.api.adapters.PictureResourceAdapter;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

public class PictureConfigurationService extends DefaultComponent{

    public static final String NAME = "org.nuxeo.ecm.platform.picture.config.PictureConfigurationService";

    public static final String PICTURE_ADAPTER_EP = "PictureAdapter";

    public static final String DEFAULT_CONFIGURATION = "default";

    private static final Log log = LogFactory.getLog(PictureConfigurationService.class);

    private static Map<String, PictureAdapterDescriptor> pictureAdapterDescriptors;

    @Override
    public void activate(ComponentContext context) {
        pictureAdapterDescriptors = new HashMap<String, PictureAdapterDescriptor>();
    }

    @Override
    public void deactivate(ComponentContext context) {
        pictureAdapterDescriptors = null;
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
       if (extensionPoint.equals(PICTURE_ADAPTER_EP)) {
            PictureAdapterDescriptor pictureAdapter = (PictureAdapterDescriptor) contribution;
            registerPictureAdapter(pictureAdapter, contributor);
        } else {
            log.error("Extension point " + extensionPoint + "is unknown");
        }
    }

    public static void registerPictureAdapter(PictureAdapterDescriptor pictureAdapter,
            ComponentInstance contributor) {
        pictureAdapterDescriptors.put(pictureAdapter.getTypeName(), pictureAdapter);
        log.debug("registered Picture AdapterDescriptor: "
                + pictureAdapter.getName());
    }

    public static PictureResourceAdapter getAdapterForType(String docType)
            throws InstantiationException, IllegalAccessException {
        PictureAdapterDescriptor dad = pictureAdapterDescriptors.get(docType);
        if (dad != null) {
            return dad.getNewInstance();
        } else {
            return null;
        }
    }

}
