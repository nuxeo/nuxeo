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

package org.nuxeo.ecm.webapp.trashManagement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

public class TrashManagementService extends DefaultComponent {

    public static final String NAME = "org.nuxeo.ecm.webapp.trashManagement.TrashManagementService";

    private static final Log log = LogFactory.getLog(TrashManagementService.class);

    private boolean trashManagementEnabled;

    @Override
    public void activate(ComponentContext context) {
        log.debug("TrashManagementService activated");
    }

    @Override
    public void deactivate(ComponentContext context) {
        log.debug("TrashManagementService deactivated");
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        if ("config".equals(extensionPoint)){
            TrashConfigDescriptor descriptor = (TrashConfigDescriptor) contribution;
            trashManagementEnabled = descriptor.enabled;
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        trashManagementEnabled = false;
    }

    public boolean isTrashManagementEnabled(){
        return trashManagementEnabled;
    }

}
