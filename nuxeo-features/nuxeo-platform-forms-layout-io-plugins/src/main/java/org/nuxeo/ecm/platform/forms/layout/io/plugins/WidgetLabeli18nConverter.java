/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.platform.forms.layout.io.plugins;

import java.util.Map;

import org.nuxeo.ecm.platform.forms.layout.api.WidgetDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.converters.AbstractWidgetDefinitionConverter;
import org.nuxeo.ecm.platform.forms.layout.api.converters.LayoutConversionContext;
import org.nuxeo.ecm.platform.forms.layout.io.plugins.helpers.TranslationHelper;

/**
 * Converter that replaces labels to translate by their translation in a given
 * language.
 *
 * @since 5.5
 */
public class WidgetLabeli18nConverter extends AbstractWidgetDefinitionConverter {

    @Override
    public WidgetDefinition getWidgetDefinition(WidgetDefinition orig,
            LayoutConversionContext ctx) {
        String lang = ctx.getLanguage();
        if (orig.isTranslated() && lang != null) {
            // translate widget labels
            WidgetDefinition clone = getClonedWidget(orig);
            Map<String, String> labels = TranslationHelper.getTranslatedLabels(
                    clone.getLabels(), lang);
            Map<String, String> helpLabels = TranslationHelper.getTranslatedLabels(
                    clone.getHelpLabels(), lang);
            clone.setLabels(labels);
            clone.setHelpLabels(helpLabels);
            return clone;
        }
        return orig;
    }
}