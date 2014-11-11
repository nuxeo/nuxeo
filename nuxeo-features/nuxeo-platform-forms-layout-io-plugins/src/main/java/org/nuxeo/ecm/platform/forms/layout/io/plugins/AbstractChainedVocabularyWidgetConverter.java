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

import java.util.Arrays;
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
public abstract class AbstractChainedVocabularyWidgetConverter extends
        AbstractWidgetDefinitionConverter {

    protected List<String> getAcceptedWidgetTypes() {
        return Arrays.asList(new String[] { "template" });
    }

    protected abstract List<String> getAcceptedWidgetNames();

    protected abstract String getParentDirectoryName();

    protected abstract String getChildDirectoryName();

    @Override
    public WidgetDefinition getWidgetDefinition(WidgetDefinition widgetDef,
            LayoutConversionContext ctx) {
        String wType = widgetDef.getType();
        String wName = widgetDef.getName();
        if (getAcceptedWidgetNames().contains(wName)
                && getAcceptedWidgetTypes().contains(wType)) {
            WidgetDefinition clone = getClonedWidget(widgetDef);
            // change select options on new widget
            WidgetSelectOption[] selectOptions = VocabularyHelper.getChainSelectVocabularySelectOptions(
                    getParentDirectoryName(), getChildDirectoryName(),
                    ctx.getLanguage()).toArray(new WidgetSelectOption[] {});
            clone.setSelectOptions(selectOptions);
            return clone;
        }
        return widgetDef;
    }
}
