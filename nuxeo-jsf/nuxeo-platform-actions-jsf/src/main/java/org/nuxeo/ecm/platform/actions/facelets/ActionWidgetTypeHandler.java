/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.platform.actions.facelets;

import org.nuxeo.ecm.platform.forms.layout.api.Widget;
import org.nuxeo.ecm.platform.forms.layout.facelets.plugins.TemplateWidgetTypeHandler;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;

/**
 * Action widget type, allowing definition of a compat template.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class ActionWidgetTypeHandler extends TemplateWidgetTypeHandler {

    private static final long serialVersionUID = 1L;

    public static final String COMPAT_TEMPLATE_PROPERTY_NAME = "compat_template";

    @Override
    protected String getTemplateValue(Widget widget) {
        ConfigurationService cs = Framework.getService(ConfigurationService.class);
        boolean useCompat = cs.isBooleanPropertyTrue("nuxeo.jsf.actions.removeActionOptims");
        if (useCompat) {
            String compatTemplate = lookupProperty(COMPAT_TEMPLATE_PROPERTY_NAME, widget);
            if (compatTemplate != null) {
                return compatTemplate;
            }
        }
        return super.getTemplateValue(widget);
    }

}
