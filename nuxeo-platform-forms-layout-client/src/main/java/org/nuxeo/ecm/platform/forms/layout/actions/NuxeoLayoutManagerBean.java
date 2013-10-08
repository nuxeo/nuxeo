/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.forms.layout.actions;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import org.nuxeo.runtime.api.Framework;

/**
 * Session-scoped Seam component for layout related actions.
 *
 * @since 6.0
 */
public class NuxeoLayoutManagerBean {

    public static final String NAME = "nuxeoLayoutManagerBean";

    protected Boolean devModeSet = null;

    protected static String enabledMessage = "UI Development mode activated: press 'Shift+d' to activate/deactivate on a given page";

    protected static String disabledMessage = "UI Development mode deactivated";

    public boolean isDevModeSet() {
        if (devModeSet == null) {
            devModeSet = Boolean.valueOf(Framework.isDevModeSet());
            // add a faces message at init
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO,
                    enabledMessage, null);
            FacesContext faces = FacesContext.getCurrentInstance();
            faces.addMessage(null, message);
        }
        return devModeSet.booleanValue();
    }

    public void setDevModeSet(boolean value) {
        devModeSet = Boolean.valueOf(value);
    }

}