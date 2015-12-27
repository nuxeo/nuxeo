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

import java.util.List;

import org.nuxeo.ecm.platform.forms.layout.api.WidgetDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetSelectOption;
import org.nuxeo.ecm.platform.forms.layout.api.converters.AbstractWidgetDefinitionConverter;
import org.nuxeo.ecm.platform.forms.layout.api.converters.LayoutConversionContext;
import org.nuxeo.ecm.platform.forms.layout.io.plugins.helpers.VocabularyHelper;

/**
 * Abstract class to convert a chained vocabulary.
 * <p>
 * Only supports vocabularies with 2 levels for now
 *
 * @since 5.5
 */
public abstract class AbstractChainedVocabularyWidgetConverter extends AbstractWidgetDefinitionConverter {

    protected boolean isAccepted(String wType) {
        return "template".equals(wType) || WidgetDirectoryItemsConverter.isDirectoryWidget(wType);
    }

    protected abstract List<String> getAcceptedWidgetNames();

    protected abstract String getParentDirectoryName();

    protected abstract String getChildDirectoryName();

    @Override
    public WidgetDefinition getWidgetDefinition(WidgetDefinition widgetDef, LayoutConversionContext ctx) {
        String wType = widgetDef.getType();
        String wName = widgetDef.getName();
        if (getAcceptedWidgetNames().contains(wName) && isAccepted(wType)) {
            WidgetDefinition clone = getClonedWidget(widgetDef);
            // change select options on new widget
            WidgetSelectOption[] selectOptions = VocabularyHelper.getChainSelectVocabularySelectOptions(
                    getParentDirectoryName(), getChildDirectoryName(), ctx.getLanguage()).toArray(
                    new WidgetSelectOption[] {});
            clone.setSelectOptions(selectOptions);
            return clone;
        }
        return widgetDef;
    }
}
