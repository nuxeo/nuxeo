/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.ecm.platform.ui.web.component;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlForm;
import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.Framework;

/**
 * Override the default form component to add warnings for nested forms issues when debug mode is on.
 *
 * @since 5.7
 */
public class NXHtmlForm extends HtmlForm {

    private static final Log log = LogFactory.getLog(NXHtmlForm.class);

    @Override
    public void encodeBegin(FacesContext context) throws IOException {
        if (Framework.isDevModeSet()) {
            // sanity check before checking for nested forms: issue an error if
            // there is a parent container that is a form
            UIComponent parent = getParent();
            while (parent != null) {
                if (parent instanceof NXHtmlForm) {
                    log.error("Form component with id '" + getId() + "' is already surrounded by a form with id '"
                            + parent.getId() + "'");
                    break;
                }
                parent = parent.getParent();
            }
        }
        super.processDecodes(context);
    }

}