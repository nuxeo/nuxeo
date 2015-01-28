/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * Handler that manages a defaultValue attribute set on the tag, to default to this value when value is null or empty. *
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
    public void onComponentCreated(FaceletContext ctx, UIComponent c, UIComponent parent) {
        if (ValueHolder.class.isAssignableFrom(c.getClass())) {
            ValueExpression dve = c.getValueExpression("defaultValue");
            if (dve != null) {
                c.setValueExpression("value", new DefaultValueExpression(c.getValueExpression("value"), dve));
            }
        }
    }

}
