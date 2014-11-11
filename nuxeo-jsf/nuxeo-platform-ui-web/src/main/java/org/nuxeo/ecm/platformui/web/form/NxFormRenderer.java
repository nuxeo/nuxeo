/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Guillaume Renard
 */
package org.nuxeo.ecm.platformui.web.form;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlForm;
import javax.faces.context.FacesContext;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.runtime.api.Framework;

import com.sun.faces.renderkit.html_basic.FormRenderer;

/**
 * Nuxeo h:form tag renderer override, to include javascript code preventing
 * multiple submissions of the form.
 *
 * @since 5.7.3
 */
public class NxFormRenderer extends FormRenderer {

    public static final String ENABLE_DOUBLE_CLICK_SHIELD = "nuxeo.jsf.enableDoubleClickShield";

    public static final String ENABLE_DOUBLE_CLICK_ON_ELEMENT = "disableDoubleClickShield";

    public static final String DOUBLE_CLICK_SHIELD_CSS_CLASS_FLAG = "doubleClickShielded";

    protected static boolean isDoubleShieldEnabled() {
        return !Framework.isBooleanPropertyFalse(ENABLE_DOUBLE_CLICK_SHIELD);
    }

    protected static boolean dcDisabledOnElement(UIComponent component) {
        if (component != null) {
            Object dcDisabledOnElement = component.getAttributes().get(
                    ENABLE_DOUBLE_CLICK_ON_ELEMENT);
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

    protected static boolean containsDoubleClickShieldClass(
            final String styleClass) {
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
    public void encodeBegin(FacesContext context, UIComponent component)
            throws IOException {

        if (component.isRendered() && isDoubleShieldEnabled()) {
            if (!dcDisabledOnElement(component)) {
                String styleClass = (String) ((HtmlForm) component).getAttributes().get(
                        "styleClass");
                if (StringUtils.isBlank(styleClass)) {
                    ((HtmlForm) component).setStyleClass(DOUBLE_CLICK_SHIELD_CSS_CLASS_FLAG);
                } else {
                    if (!containsDoubleClickShieldClass(styleClass)) {
                        ((HtmlForm) component).setStyleClass(styleClass + " "
                                + DOUBLE_CLICK_SHIELD_CSS_CLASS_FLAG);
                    }
                }
            }
        }

        super.encodeBegin(context, component);

    }

}
