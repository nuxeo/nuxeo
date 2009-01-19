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
 *     Anahide Tchertchian
 *
 * $Id$
 */

package org.nuxeo.webengine.sites;

import static org.jboss.seam.ScopeType.STATELESS;

import javax.faces.application.FacesMessage;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;

/**
 * Performs validation and re-rendering of webcontainer layout widgets.
 *
 * @author Anahide Tchertchian
 */
@Name("siteActions")
@Scope(STATELESS)
public class SiteActionsBean {

    private static final Log log = LogFactory.getLog(SiteActionsBean.class);

    public static final String SCHEMA_NAME = "webcontainer";

    public static final String ISWEBCONTAINER_PROPERTY_NAME = "isWebContainer";

    public void validateEmail(FacesContext context, UIComponent component,
            Object value) {
        // validate value, and in case of error:
        if (!(value instanceof String)) {
            FacesMessage message = new FacesMessage(
                    FacesMessage.SEVERITY_ERROR, ComponentUtils.translate(
                            context, "label.userManager.wrong.username"), null);
            ((EditableValueHolder) component).setValid(false);
            context.addMessage(component.getClientId(context), message);
            // also add global message
            context.addMessage(null, message);
        }
    }

    public String toggleWebContainerLayout(DocumentModel webContainer)
            throws ClientException {
        if (webContainer == null || !webContainer.hasSchema(SCHEMA_NAME)) {
            log.error("Invalid document model...");
            return null;
        }
        Object isWebContainer = webContainer.getProperty(SCHEMA_NAME,
                ISWEBCONTAINER_PROPERTY_NAME);
        // assume it's false by default => toggle to true
        boolean newValue = true;
        if (isWebContainer instanceof Boolean) {
            newValue = !((Boolean) isWebContainer);
        }
        webContainer.setProperty(SCHEMA_NAME, ISWEBCONTAINER_PROPERTY_NAME,
                newValue);
        return null;
    }
}
