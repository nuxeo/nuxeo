/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.forms.layout.io.plugins;

import java.util.Map;

import org.nuxeo.ecm.platform.forms.layout.api.WidgetDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.converters.AbstractWidgetDefinitionConverter;
import org.nuxeo.ecm.platform.forms.layout.api.converters.LayoutConversionContext;
import org.nuxeo.ecm.platform.forms.layout.io.plugins.helpers.TranslationHelper;

/**
 * Converter that replaces labels to translate by their translation in a given language.
 *
 * @since 5.5
 */
public class WidgetLabeli18nConverter extends AbstractWidgetDefinitionConverter {

    @Override
    public WidgetDefinition getWidgetDefinition(WidgetDefinition orig, LayoutConversionContext ctx) {
        String lang = ctx.getLanguage();
        if (orig.isTranslated() && lang != null) {
            // translate widget labels
            WidgetDefinition clone = getClonedWidget(orig);
            Map<String, String> labels = TranslationHelper.getTranslatedLabels(clone.getLabels(), lang);
            Map<String, String> helpLabels = TranslationHelper.getTranslatedLabels(clone.getHelpLabels(), lang);
            clone.setLabels(labels);
            clone.setHelpLabels(helpLabels);
            return clone;
        }
        return orig;
    }
}
