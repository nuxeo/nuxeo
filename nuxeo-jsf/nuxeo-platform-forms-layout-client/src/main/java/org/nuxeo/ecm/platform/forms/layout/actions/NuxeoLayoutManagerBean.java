/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, activated ? activatedMessage
                    : deactivatedMessage, null);
            FacesContext faces = FacesContext.getCurrentInstance();
            faces.addMessage(null, message);
            // avoid redirect for message to be displayed
            HttpServletRequest httpRequest = (HttpServletRequest) faces.getExternalContext().getRequest();
            httpRequest.setAttribute(FancyNavigationHandler.DISABLE_REDIRECT_FOR_URL_REWRITE, Boolean.TRUE);
        }
    }

}
