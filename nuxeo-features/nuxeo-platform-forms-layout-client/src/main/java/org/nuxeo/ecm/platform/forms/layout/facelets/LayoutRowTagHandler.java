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
 * $Id: LayoutRowTagHandler.java 30553 2008-02-24 15:51:31Z atchertchian $
 */

package org.nuxeo.ecm.platform.forms.layout.facelets;

import java.io.IOException;

import javax.el.ELException;
import javax.el.VariableMapper;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.sun.facelets.FaceletContext;
import com.sun.facelets.FaceletException;
import com.sun.facelets.el.VariableMapperWrapper;
import com.sun.facelets.tag.TagConfig;
import com.sun.facelets.tag.TagHandler;

/**
 * Layout row recursion tag handler.
 * <p>
 * Uses facelet template features to iterate over the layout rows and apply next
 * handlers as many times as needed.
 * <p>
 * Only works when used inside a tag using the {@link LayoutTagHandler} template
 * client.
 *
 * @see LayoutTagHandler#apply(FaceletContext, UIComponent, String)
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class LayoutRowTagHandler extends TagHandler {

    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(LayoutRowTagHandler.class);

    int rowCounter = 0;

    public LayoutRowTagHandler(TagConfig config) {
        super(config);
    }

    // XXX same handler is used in different threads => synchronize it since
    // some member fields are not supposed to be shared
    public synchronized void apply(FaceletContext ctx, UIComponent parent)
            throws IOException, FacesException, FaceletException, ELException {
        // XXX same handler is used in different threads => reset counter
        // before use
        rowCounter = 0;

        VariableMapper orig = ctx.getVariableMapper();
        ctx.setVariableMapper(new VariableMapperWrapper(orig));
        try {
            while (ctx.includeDefinition(parent,
                    TemplateClientHelper.generateRowNumber(rowCounter))) {
                nextHandler.apply(ctx, parent);
                rowCounter++;
                // log.debug("row thread="
                // + String.valueOf(Thread.currentThread().getId())
                // + ", handler=" + this.hashCode() + ", row=" + rowCounter);
            }
        } finally {
            ctx.setVariableMapper(orig);
        }
        // log.debug("row thread="
        // + String.valueOf(Thread.currentThread().getId()) + ", handler="
        // + this.hashCode() + ", row counter reset");
    }

}
