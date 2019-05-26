/*
 * (C) Copyright 2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: GenericHtmlComponentHandler.java 28610 2008-01-09 17:13:52Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.tag.handler;

import javax.el.ValueExpression;
import javax.faces.component.UIComponent;
import javax.faces.component.ValueHolder;
import javax.faces.view.facelets.ComponentConfig;
import javax.faces.view.facelets.FaceletContext;

import org.nuxeo.ecm.platform.ui.web.binding.DefaultValueExpression;

import com.sun.faces.facelets.tag.jsf.html.HtmlComponentHandler;

/**
 * Generic HTML component handler.
 * <p>
 * Handler that manages a defaultValue attribute set on the tag, to default to this value when value is null or empty.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class GenericHtmlComponentHandler extends HtmlComponentHandler {

    public GenericHtmlComponentHandler(ComponentConfig config) {
        super(config);
    }

    /**
     * Overrides the "value" value expression to handle default value mapping.
     */
    @Override
    public void onComponentCreated(FaceletContext ctx, UIComponent c, UIComponent parent) {
        if (c instanceof ValueHolder) {
            ValueExpression dve = c.getValueExpression("defaultValue");
            if (dve != null) {
                c.setValueExpression("value", new DefaultValueExpression(c.getValueExpression("value"), dve));
            }
        }
    }

}
