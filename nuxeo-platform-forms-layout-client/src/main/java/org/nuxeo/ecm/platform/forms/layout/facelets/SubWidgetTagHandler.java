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
 * $Id: SubWidgetTagHandler.java 30553 2008-02-24 15:51:31Z atchertchian $
 */

package org.nuxeo.ecm.platform.forms.layout.facelets;

import java.io.IOException;

import javax.el.ELException;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.sun.facelets.FaceletContext;
import com.sun.facelets.tag.TagConfig;
import com.sun.facelets.tag.TagHandler;

/**
 * SubWidget tag handler.
 * <p>
 * <p>
 * Uses facelet template features to iterate over a widget subwidgets and apply
 * next handlers as many times as needed.
 * <p>
 * Only works when used inside a tag using the {@link WidgetTagHandler}.
 *
 * @see WidgetTagHandler#apply(FaceletContext, UIComponent, String)
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 */
public class SubWidgetTagHandler extends TagHandler {

    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(SubWidgetTagHandler.class);

    int subWidgetCounter = 0;

    public SubWidgetTagHandler(TagConfig config) {
        super(config);
    }

    // XXX same handler is used in different threads => synchronize it since
    // some member fields are not supposed to be shared
    public synchronized void apply(FaceletContext ctx, UIComponent parent)
            throws IOException, FacesException, ELException {
        // XXX same handler is used in different threads => reset counter
        // before use
        subWidgetCounter = 0;
        while (ctx.includeDefinition(parent,
                TemplateClientHelper.generateSubWidgetNumber(subWidgetCounter))) {
            nextHandler.apply(ctx, parent);
            subWidgetCounter++;
        }
    }

}
