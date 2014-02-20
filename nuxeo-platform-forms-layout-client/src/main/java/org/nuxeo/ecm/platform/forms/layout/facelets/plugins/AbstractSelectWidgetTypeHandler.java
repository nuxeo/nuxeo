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
package org.nuxeo.ecm.platform.forms.layout.facelets.plugins;

import java.util.ArrayList;
import java.util.List;

import javax.faces.view.facelets.CompositeFaceletHandler;
import javax.faces.view.facelets.FaceletHandler;
import javax.faces.view.facelets.TagAttributes;

import org.nuxeo.ecm.platform.forms.layout.api.Widget;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetSelectOption;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetSelectOptions;
import org.nuxeo.ecm.platform.forms.layout.facelets.FaceletHandlerHelper;
import org.nuxeo.ecm.platform.forms.layout.facelets.LeafFaceletHandler;
import org.nuxeo.ecm.platform.ui.web.component.UISelectItem;
import org.nuxeo.ecm.platform.ui.web.component.UISelectItems;

/**
 * Helper class for options generation depending on the widget definition
 *
 * @since 5.4.2
 */
public abstract class AbstractSelectWidgetTypeHandler extends
        AbstractWidgetTypeHandler {

    private static final long serialVersionUID = 1L;

    protected FaceletHandler getOptionsFaceletHandler(
            FaceletHandlerHelper helper, Widget widget) {
        FaceletHandler leaf = new LeafFaceletHandler();
        WidgetSelectOption[] selectOptions = widget.getSelectOptions();
        List<FaceletHandler> selectItems = new ArrayList<FaceletHandler>();
        // TODO: maybe check other widget properties to know if a default
        // disabled option should be added for instance
        String widgetTagConfigId = widget.getTagConfigId();
        if (selectOptions != null && selectOptions.length > 0) {
            for (WidgetSelectOption selectOption : selectOptions) {
                TagAttributes attrs = helper.getTagAttributes(selectOption);
                if (selectOption instanceof WidgetSelectOptions) {
                    selectItems.add(helper.getHtmlComponentHandler(
                            widgetTagConfigId, attrs, leaf,
                            UISelectItems.COMPONENT_TYPE, null));
                } else if (selectOption != null) {
                    selectItems.add(helper.getHtmlComponentHandler(
                            widgetTagConfigId, attrs, leaf,
                            UISelectItem.COMPONENT_TYPE, null));
                }
            }
        }
        return new CompositeFaceletHandler(
                selectItems.toArray(new FaceletHandler[0]));
    }

}
