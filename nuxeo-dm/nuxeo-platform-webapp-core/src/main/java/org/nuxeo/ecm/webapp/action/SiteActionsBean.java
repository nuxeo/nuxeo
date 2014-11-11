/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.webapp.action;


import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;

import static org.jboss.seam.ScopeType.STATELESS;

/**
 * Performs re-rendering of webcontainer layout widgets.
 *
 * @author Anahide Tchertchian
 * @author rux added the site name validation
 */

@Name("siteActions")
@Scope(STATELESS)
public class SiteActionsBean {

    private static final Log log = LogFactory.getLog(SiteActionsBean.class);

    /**
     * Validates the web container fields. If the workspace is web container, it
     * also needs to have name. The usual required JSF component can't be used,
     * because it will block the validation no matter if the checkbox is set or
     * not. As result, the widget validation is used. The both values need to be
     * available in layout to be used.
     * @param context
     * @param component
     * @param value
     */
    public void validateName(FacesContext context, UIComponent component,
            Object value) {

        Map<String, Object> attributes = component.getAttributes();

        String wcId = (String) attributes.get("webContainerId");
        if (wcId == null) {
            log.debug("Cannot validate name: input wcId not found");
            return;
        }

        UIInput wcComp = (UIInput) component.findComponent(wcId);
        if (wcComp == null) {
            log.debug("Cannot validate name: input wcId not found second time");
            return;
        }

        Boolean propValue = (Boolean) wcComp.getLocalValue();
        boolean isWC = false;
        if (propValue != null) {
            isWC = propValue;
        }
        if (!isWC) {
            // no need validation if not web container
            return;
        }

        String nameId = (String) attributes.get("nameId");
        if (nameId == null) {
            log.error("Cannot validate name: input id(s) not found");
            return;
        }

        UIInput nameComp = (UIInput) component.findComponent(nameId);
        if (nameComp == null) {
            log.error("Cannot validate name: input(s) not found second time");
            return;
        }

        Object nameObj = nameComp.getLocalValue();

        if (nameObj == null || StringUtils.isBlank(nameObj.toString())) {
            FacesMessage message = new FacesMessage(
                    FacesMessage.SEVERITY_ERROR, ComponentUtils.translate(
                            context, "label.error.need.name.webcontainer"),
                    null);
            throw new ValidatorException(message);
        }

    }

}
