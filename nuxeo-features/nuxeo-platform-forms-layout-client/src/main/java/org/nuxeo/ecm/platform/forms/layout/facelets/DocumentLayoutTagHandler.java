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
 * $Id: DocumentLayoutTagHandler.java 26053 2007-10-16 01:45:43Z atchertchian $
 */

package org.nuxeo.ecm.platform.forms.layout.facelets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.el.ELException;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.types.adapter.TypeInfo;

import com.sun.facelets.FaceletContext;
import com.sun.facelets.FaceletException;
import com.sun.facelets.FaceletHandler;
import com.sun.facelets.tag.CompositeFaceletHandler;
import com.sun.facelets.tag.TagAttribute;
import com.sun.facelets.tag.TagAttributes;
import com.sun.facelets.tag.TagConfig;
import com.sun.facelets.tag.TagHandler;

/**
 * Document layout tag handler.
 * <p>
 * Computes layouts in given facelet context, for given mode and document
 * attributes.
 * <p>
 * Document must be resolved at the component tree construction so it cannot be
 * bound to an iteration value.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class DocumentLayoutTagHandler extends TagHandler {

    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(DocumentLayoutTagHandler.class);

    protected final TagConfig config;

    protected final TagAttribute mode;

    protected final TagAttribute value;

    public DocumentLayoutTagHandler(TagConfig config) {
        super(config);
        this.config = config;
        mode = getRequiredAttribute("mode");
        value = getRequiredAttribute("value");
    }

    /**
     * If resolved document has layouts, apply each of them.
     */
    public void apply(FaceletContext ctx, UIComponent parent)
            throws IOException, FacesException, FaceletException, ELException {
        Object document = value.getObject(ctx, DocumentModel.class);
        if (!(document instanceof DocumentModel)) {
            return;
        }

        TypeInfo typeInfo = ((DocumentModel) document).getAdapter(TypeInfo.class);
        if (typeInfo == null) {
            return;
        }
        String modeValue = mode.getValue(ctx);
        String[] layoutNames = typeInfo.getLayouts(modeValue);
        if (layoutNames == null || layoutNames.length == 0) {
            return;
        }

        FaceletHandlerHelper helper = new FaceletHandlerHelper(ctx, config);
        TagAttribute modeAttr = helper.createAttribute("mode", modeValue);
        List<FaceletHandler> handlers = new ArrayList<FaceletHandler>();
        FaceletHandler leaf = new LeafFaceletHandler();
        for (String layoutName : layoutNames) {
            TagAttributes attributes = helper.getTagAttributes(
                    helper.createAttribute("name", layoutName), modeAttr,
                    value);
            TagConfig tagConfig = TagConfigFactory.createTagConfig(config,
                    attributes, leaf);
            handlers.add(new LayoutTagHandler(tagConfig));
        }
        CompositeFaceletHandler composite = new CompositeFaceletHandler(
                handlers.toArray(new FaceletHandler[]{}));
        composite.apply(ctx, parent);
    }
}
