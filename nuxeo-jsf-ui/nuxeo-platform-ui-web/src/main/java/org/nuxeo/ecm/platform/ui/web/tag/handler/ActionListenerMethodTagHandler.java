/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id$
 */

package org.nuxeo.ecm.platform.ui.web.tag.handler;

import java.io.IOException;

import javax.el.ELException;
import javax.faces.FacesException;
import javax.faces.component.ActionSource;
import javax.faces.component.UIComponent;
import javax.faces.event.ActionEvent;
import javax.faces.event.ActionListener;
import javax.faces.event.MethodExpressionActionListener;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletException;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagException;
import javax.faces.view.facelets.TagHandler;

import org.nuxeo.ecm.platform.ui.web.binding.MetaMethodExpression;

/**
 * Action listener method handler: makes it possible to add an action listener method to any action source component
 * parent.
 * <p>
 * This is useful when declaring several action listeners on the same parent component, and when the order of calls
 * needs to be respected: the action listener method declared on a component is the first one called. So this method
 * makes it possible to add other action listeners before it, without having to declare a class (when using the
 * f:actionListener tag).
 *
 * @author Anahide Tchertchian
 */
public class ActionListenerMethodTagHandler extends TagHandler {

    protected final TagAttribute value;

    @SuppressWarnings("rawtypes")
    public static final Class[] ACTION_LISTENER_SIG = { ActionEvent.class };

    public ActionListenerMethodTagHandler(TagConfig config) {
        super(config);
        value = getRequiredAttribute("value");
    }

    @Override
    public void apply(FaceletContext ctx, UIComponent parent)
            throws IOException, FacesException, FaceletException, ELException {
        if (parent instanceof ActionSource) {
            // only process if parent was just created
            if (parent.getParent() == null) {
                ActionSource src = (ActionSource) parent;
                ActionListener listener = new MethodExpressionActionListener(
                        new MetaMethodExpression(value.getMethodExpression(ctx, null, ACTION_LISTENER_SIG),
                                ctx.getFunctionMapper(), ctx.getVariableMapper()));
                src.addActionListener(listener);
            }
        } else {
            throw new TagException(tag, "Parent is not of type ActionSource, type is: " + parent);
        }
    }

}
