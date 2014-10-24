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

import java.io.Serializable;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import org.nuxeo.ecm.platform.ui.web.rest.FancyNavigationHandler;
import org.nuxeo.runtime.api.Framework;

/**
 * Session-scoped Seam component for layout related actions.
 *
 * @since 6.0
 */
public class NuxeoLayoutManagerBean implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String NAME = "nuxeoLayoutManagerBean";

    protected Boolean devModeSet = null;

    protected static String activatedMessage = "UI Development mode activated: "
            + "keep the 'shift' key pressed and mouse hover elements in the page.";

    protected static String deactivatedMessage = "UI Development mode deactivated";

    public boolean isDevModeSet() {
        if (devModeSet == null) {
            setDevModeSet(false, Framework.isDevModeSet());
            // maybe enable by default when dev mode is set
            // setDevModeSet(Framework.isDevModeSet());
        }
        return devModeSet.booleanValue();
    }

    public void setDevModeSet(boolean activated) {
        setDevModeSet(activated, true);
    }

    public void setDevModeSet(boolean activated, boolean addMessage) {
        devModeSet = Boolean.valueOf(activated);
        if (addMessage) {
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO,
                    activated ? activatedMessage : deactivatedMessage, null);
            FacesContext faces = FacesContext.getCurrentInstance();
            faces.addMessage(null, message);
            // avoid redirect for message to be displayed
            HttpServletRequest httpRequest = (HttpServletRequest) faces.getExternalContext().getRequest();
            httpRequest.setAttribute(
                    FancyNavigationHandler.DISABLE_REDIRECT_FOR_URL_REWRITE,
                    Boolean.TRUE);
        }
    }

}