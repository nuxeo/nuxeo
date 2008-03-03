/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: WidgetTypeTagHandler.java 26053 2007-10-16 01:45:43Z atchertchian $
 */

package org.nuxeo.ecm.platform.forms.layout.facelets;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.el.ELException;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.forms.layout.api.Widget;
import org.nuxeo.ecm.platform.forms.layout.service.WebLayoutManager;
import org.nuxeo.ecm.platform.ui.web.util.ComponentTagUtils;
import org.nuxeo.runtime.api.Framework;

import com.sun.facelets.FaceletContext;
import com.sun.facelets.tag.TagAttribute;
import com.sun.facelets.tag.TagConfig;
import com.sun.facelets.tag.TagHandler;

/**
 * Widget type tag handler.
 * <p>
 * Applies a {@link WidgetTypeHandler} resolved from a widget created for given
 * type name and mode, and uses other tag attributes to fill the widget
 * properties.
 * <p>
 * Does not handle sub widgets.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 */
public class WidgetTypeTagHandler extends TagHandler {

    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(WidgetTypeTagHandler.class);

    protected final TagConfig config;

    protected final TagAttribute name;

    protected final TagAttribute mode;

    protected final TagAttribute value;

    protected final TagAttribute[] vars;

    public WidgetTypeTagHandler(TagConfig config) {
        super(config);
        this.config = config;
        name = getRequiredAttribute("name");
        mode = getRequiredAttribute("mode");
        value = getAttribute("value");
        vars = tag.getAttributes().getAll();
    }

    public final void apply(FaceletContext ctx, UIComponent parent)
            throws IOException, FacesException, ELException {
        WebLayoutManager layoutService;
        try {
            layoutService = Framework.getService(WebLayoutManager.class);
        } catch (Exception e) {
            throw new FacesException(e);
        }

        // build handler
        Map<String, Serializable> properties = new HashMap<String, Serializable>();
        for (TagAttribute var : vars) {
            String localName = var.getLocalName();
            if ("mode" != localName && "type" != localName) {
                properties.put(localName, var.getValue());
            }
        }
        String typeValue = name.getValue(ctx);
        String modeValue = mode.getValue(ctx);
        String valueName = value.getValue();
        if (ComponentTagUtils.isValueReference(valueName)) {
            valueName = valueName.substring(2, valueName.length() - 1);
        }
        Widget widget = layoutService.createWidget(ctx, typeValue, modeValue,
                valueName, properties, null);
        WidgetTagHandler.applyWidgetHandler(ctx, parent, config, widget, value,
                true);
    }
}
