/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
