/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: ListWidgetTypeHandler.java 26053 2007-10-16 01:45:43Z atchertchian $
 */

package org.nuxeo.ecm.platform.forms.layout.facelets.plugins;

import javax.faces.view.facelets.TagConfig;

import org.nuxeo.ecm.platform.forms.layout.api.Widget;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;

/**
 * List widget type, using a fixed template.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class ListWidgetTypeHandler extends TemplateWidgetTypeHandler {

    public static final String COMPAT_TEMPLATE_PROPERTY_NAME = "compatTemplate";

    public ListWidgetTypeHandler(TagConfig config) {
        super(config);
    }

    @Override
    protected String getTemplateValue(Widget widget) {
        ConfigurationService configurationService = Framework.getService(ConfigurationService.class);
        boolean useCompat = configurationService.isBooleanTrue("nuxeo.jsf.listWidget.compatEnabled");
        if (useCompat) {
            return lookupProperty(COMPAT_TEMPLATE_PROPERTY_NAME, widget);
        } else {
            return super.getTemplateValue(widget);
        }
    }

}
