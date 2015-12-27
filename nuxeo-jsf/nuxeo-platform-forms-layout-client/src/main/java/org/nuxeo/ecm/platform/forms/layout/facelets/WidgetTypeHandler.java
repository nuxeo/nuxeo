/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
