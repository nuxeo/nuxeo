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
 *     Guillaume Renard
 */
package org.nuxeo.ecm.platformui.web.form;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlForm;
import javax.faces.context.FacesContext;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;

import com.sun.faces.renderkit.html_basic.FormRenderer;

/**
 * Nuxeo h:form tag renderer override, to include javascript code preventing multiple submissions of the form.
 *
 * @since 5.7.3
 */
public class NxFormRenderer extends FormRenderer {

    public static final String ENABLE_DOUBLE_CLICK_SHIELD = "nuxeo.jsf.enableDoubleClickShield";

    public static final String ENABLE_DOUBLE_CLICK_ON_ELEMENT = "disableDoubleClickShield";

    public static final String DOUBLE_CLICK_SHIELD_CSS_CLASS_FLAG = "doubleClickShielded";

    protected static boolean isDoubleShieldEnabled() {
        ConfigurationService configurationService = Framework.getService(ConfigurationService.class);
        return !configurationService.isBooleanPropertyFalse(ENABLE_DOUBLE_CLICK_SHIELD);
    }

    protected static boolean dcDisabledOnElement(UIComponent component) {
        if (component != null) {
            Object dcDisabledOnElement = component.getAttributes().get(ENABLE_DOUBLE_CLICK_ON_ELEMENT);
            if (dcDisabledOnElement != null) {
                if (dcDisabledOnElement instanceof String) {
                    return Boolean.TRUE.equals(Boolean.valueOf((String) dcDisabledOnElement));
                } else if (dcDisabledOnElement instanceof Boolean) {
                    return Boolean.TRUE.equals(dcDisabledOnElement);
                }
            }
        } else {
            return true;
        }
        return false;
    }

    protected static boolean containsDoubleClickShieldClass(final String styleClass) {
        if (styleClass != null) {
            String[] split = styleClass.split(" ");
            for (String s : split) {
                if (s.equals(DOUBLE_CLICK_SHIELD_CSS_CLASS_FLAG)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void encodeBegin(FacesContext context, UIComponent component) throws IOException {

        if (component.isRendered() && isDoubleShieldEnabled()) {
            if (!dcDisabledOnElement(component)) {
                String styleClass = (String) ((HtmlForm) component).getAttributes().get("styleClass");
                if (StringUtils.isBlank(styleClass)) {
                    ((HtmlForm) component).setStyleClass(DOUBLE_CLICK_SHIELD_CSS_CLASS_FLAG);
                } else {
                    if (!containsDoubleClickShieldClass(styleClass)) {
                        ((HtmlForm) component).setStyleClass(styleClass + " " + DOUBLE_CLICK_SHIELD_CSS_CLASS_FLAG);
                    }
                }
            }
        }

        super.encodeBegin(context, component);

    }

}
