/*
 * (C) Copyright 2006-2013 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: WidgetTypeHandler.java 27477 2007-11-20 19:55:44Z jcarsique $
 */

package org.nuxeo.ecm.platform.forms.layout.facelets;

import java.io.IOException;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletHandler;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagHandler;

import org.nuxeo.ecm.platform.forms.layout.api.Widget;
import org.nuxeo.ecm.platform.forms.layout.api.exceptions.WidgetException;

/**
 * Widget type handler interface.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public abstract class WidgetTypeHandler extends TagHandler {

    public WidgetTypeHandler(TagConfig config) {
        super(config);
    }

    /**
     * Returns the facelet handler that will be applied for given widget and template in this context.
     *
     * @param ctx the facelet context in which this handler will be applied.
     * @param parent parent component in the JSF tree
     * @param widget the widget giving properties the handler will take into account.
     * @throws WidgetException
     * @throws IOException
     */
    public abstract void apply(FaceletContext ctx, UIComponent parent, Widget widget) throws WidgetException,
            IOException;

    /**
     * Returns the facelet handler used for dev mode.
     *
     * @since 6.0
     */
    public abstract FaceletHandler getDevFaceletHandler(TagConfig tagConfig, Widget widget) throws WidgetException;

    /**
     * Returns a property value given its name.
     *
     * @return property with this name.
     */
    public abstract String getProperty(String name);

    /**
     * Set properties
     */
    public abstract void setProperties(Map<String, String> properties);

    public abstract void setWidget(Widget widget);

}
