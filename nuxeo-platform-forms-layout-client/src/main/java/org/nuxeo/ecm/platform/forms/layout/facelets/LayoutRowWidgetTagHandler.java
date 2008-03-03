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
 * $Id: LayoutRowWidgetTagHandler.java 30553 2008-02-24 15:51:31Z atchertchian $
 */

package org.nuxeo.ecm.platform.forms.layout.facelets;

import java.io.IOException;

import javax.el.ELException;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.sun.facelets.FaceletContext;
import com.sun.facelets.FaceletException;
import com.sun.facelets.tag.TagConfig;
import com.sun.facelets.tag.TagHandler;

/**
 * Layout widget recursion tag handler.
 * <p>
 * Uses facelet template features to iterate over a layout row widgets and apply
 * next handlers as many times as needed.
 * <p>
 * Only works when used inside a tag using the {@link LayoutRowTagHandler}.
 *
 * @see LayoutTagHandler#apply(FaceletContext, UIComponent, String)
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class LayoutRowWidgetTagHandler extends TagHandler {

    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(LayoutRowWidgetTagHandler.class);

    int widgetCounter = 0;

    public LayoutRowWidgetTagHandler(TagConfig config) {
        super(config);
    }

    // XXX same handler is used in different threads => synchronize it since
    // some member fields are not supposed to be shared
    public synchronized void apply(FaceletContext ctx, UIComponent parent)
            throws IOException, FacesException, FaceletException, ELException {
        // XXX same handler is used in different threads => reset counter
        // before use
        widgetCounter = 0;
        while (ctx.includeDefinition(parent,
                TemplateClientHelper.generateWidgetNumber(widgetCounter))) {
            nextHandler.apply(ctx, parent);
            widgetCounter++;
        }
    }
}
