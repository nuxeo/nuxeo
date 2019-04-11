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

import org.nuxeo.ecm.platform.forms.layout.api.BuiltinModes;
import org.nuxeo.ecm.platform.forms.layout.api.BuiltinWidgetModes;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetSelectOption;
import org.nuxeo.ecm.platform.forms.layout.api.converters.AbstractWidgetDefinitionConverter;
import org.nuxeo.ecm.platform.forms.layout.api.converters.LayoutConversionContext;
import org.nuxeo.ecm.platform.forms.layout.io.plugins.helpers.VocabularyHelper;

/**
 * Converter that adds translated select options to widgets rendering directory items
 *
 * @since 5.5
 */
public class WidgetDirectoryItemsConverter extends AbstractWidgetDefinitionConverter {

    public static final String DIR_NAME_PROPERTY = "directoryName";

    protected enum SUPPORTED_DIR_TYPES {
        selectOneDirectory, selectManyDirectory, selectOneRadioDirectory, selectManyCheckboxDirectory, suggestOneDirectory, suggestManyDirectory
    }

    /**
     * @since 7.3
     */
    public static boolean isDirectoryWidget(String wType) {
        for (SUPPORTED_DIR_TYPES item : SUPPORTED_DIR_TYPES.values()) {
            if (item.name().equals(wType)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public WidgetDefinition getWidgetDefinition(WidgetDefinition widgetDef, LayoutConversionContext ctx) {
        String wType = widgetDef.getType();
        if (isDirectoryWidget(wType)) {
            String dirName = (String) widgetDef.getProperties(BuiltinModes.ANY, BuiltinModes.ANY).get(DIR_NAME_PROPERTY);
            if (dirName == null) {
                dirName = (String) widgetDef.getProperties(BuiltinModes.ANY, BuiltinWidgetModes.EDIT).get(
                        DIR_NAME_PROPERTY);
            }
            if (dirName == null) {
                dirName = (String) widgetDef.getProperties(BuiltinModes.ANY, BuiltinWidgetModes.VIEW).get(
                        DIR_NAME_PROPERTY);
            }
            if (dirName != null) {
                WidgetDefinition clone = getClonedWidget(widgetDef);
                // change select options on new widget
                WidgetSelectOption[] selectOptions = VocabularyHelper.getVocabularySelectOptions(dirName,
                        ctx.getLanguage()).toArray(new WidgetSelectOption[] {});
                clone.setSelectOptions(selectOptions);
                return clone;
            }
        }
        return widgetDef;
    }
}
