/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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

import javax.faces.component.html.HtmlSelectOneRadio;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletHandler;
import javax.faces.view.facelets.TagConfig;

import org.nuxeo.ecm.platform.forms.layout.api.Widget;
import org.nuxeo.ecm.platform.forms.layout.api.exceptions.WidgetException;

/**
 * Displays a select one radio in edit mode, taking into account select options
 * declared on the widget.
 * <p>
 * Only edit mode is implemented for now.
 *
 * @since 5.7.3
 */
public class SelectOneRadioWidgetTypeHandler extends
        AbstractSelectWidgetTypeHandler {

    private static final long serialVersionUID = 1L;

    @Override
    public FaceletHandler getFaceletHandler(FaceletContext ctx,
            TagConfig tagConfig, Widget widget, FaceletHandler[] subHandlers)
            throws WidgetException {
        return getFaceletHandler(ctx, tagConfig, widget, subHandlers,
                HtmlSelectOneRadio.COMPONENT_TYPE);
    }

}
