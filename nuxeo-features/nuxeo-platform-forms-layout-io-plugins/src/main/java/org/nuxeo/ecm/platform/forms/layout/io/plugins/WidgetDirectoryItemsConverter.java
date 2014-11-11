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

import org.nuxeo.ecm.platform.forms.layout.api.BuiltinModes;
import org.nuxeo.ecm.platform.forms.layout.api.BuiltinWidgetModes;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetSelectOption;
import org.nuxeo.ecm.platform.forms.layout.api.converters.AbstractWidgetDefinitionConverter;
import org.nuxeo.ecm.platform.forms.layout.api.converters.LayoutConversionContext;
import org.nuxeo.ecm.platform.forms.layout.io.plugins.helpers.VocabularyHelper;

/**
 * Converter that adds translated select options to widgets rendering directory
 * items
 *
 * @since 5.5
 */
public class WidgetDirectoryItemsConverter extends
        AbstractWidgetDefinitionConverter {

    public static final String DIR_NAME_PROPERTY = "directoryName";

    protected static enum SUPPORTED_DIR_TYPES {
        selectOneDirectory, selectManyDirectory
    }

    protected boolean isDirectoryWidget(String wType) {
        return SUPPORTED_DIR_TYPES.selectManyDirectory.name().equals(wType)
                || SUPPORTED_DIR_TYPES.selectOneDirectory.name().equals(wType);
    }

    @Override
    public WidgetDefinition getWidgetDefinition(WidgetDefinition widgetDef,
            LayoutConversionContext ctx) {
        String wType = widgetDef.getType();
        if (isDirectoryWidget(wType)) {
            String dirName = (String) widgetDef.getProperties(BuiltinModes.ANY,
                    BuiltinModes.ANY).get(DIR_NAME_PROPERTY);
            if (dirName == null) {
                dirName = (String) widgetDef.getProperties(BuiltinModes.ANY,
                        BuiltinWidgetModes.EDIT).get(DIR_NAME_PROPERTY);
            }
            if (dirName == null) {
                dirName = (String) widgetDef.getProperties(BuiltinModes.ANY,
                        BuiltinWidgetModes.VIEW).get(DIR_NAME_PROPERTY);
            }
            if (dirName != null) {
                WidgetDefinition clone = getClonedWidget(widgetDef);
                // change select options on new widget
                WidgetSelectOption[] selectOptions = VocabularyHelper.getVocabularySelectOptions(
                        dirName, ctx.getLanguage()).toArray(
                        new WidgetSelectOption[] {});
                clone.setSelectOptions(selectOptions);
                return clone;
            }
        }
        return widgetDef;
    }
}
