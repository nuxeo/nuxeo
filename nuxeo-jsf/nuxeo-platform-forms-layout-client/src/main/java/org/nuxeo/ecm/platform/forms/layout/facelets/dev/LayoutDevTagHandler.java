/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.forms.layout.facelets.dev;

import java.io.IOException;

import javax.el.ELException;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletException;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagAttributes;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagHandler;

import org.nuxeo.ecm.platform.forms.layout.api.BuiltinModes;
import org.nuxeo.ecm.platform.forms.layout.api.Layout;
import org.nuxeo.ecm.platform.forms.layout.facelets.FaceletHandlerHelper;
import org.nuxeo.ecm.platform.ui.web.tag.handler.TagConfigFactory;

import com.sun.faces.facelets.tag.ui.DecorateHandler;

/**
 * Dev tag handler for layouts, retrieving the template on the layout
 * definition (or its type definition) using the template defined in mode
 * {@link BuiltinModes#DEV}.
 * <p>
 * When no template is defined, this handler is skipped.
 *
 * @since 6.0
 */
public class LayoutDevTagHandler extends TagHandler {

    protected final TagConfig config;

    protected final TagAttribute layout;

    public LayoutDevTagHandler(TagConfig config) {
        super(config);
        this.config = config;
        this.layout = getRequiredAttribute("layout");
    }

    @Override
    public void apply(FaceletContext ctx, UIComponent parent)
            throws IOException, FacesException, FaceletException, ELException {
        Layout layoutInstance = (Layout) layout.getObject(ctx, Layout.class);
        FaceletHandlerHelper helper = new FaceletHandlerHelper(ctx, config);
        TagAttribute templateAttr = helper.createAttribute("template",
                layoutInstance.getDevTemplate());
        TagAttributes attributes = FaceletHandlerHelper.getTagAttributes(templateAttr);
        String widgetTagConfigId = layoutInstance.getTagConfigId();
        TagConfig config = TagConfigFactory.createTagConfig(this.config,
                widgetTagConfigId, attributes, nextHandler);
        DecorateHandler includeHandler = new DecorateHandler(config);
        includeHandler.apply(ctx, parent);
    }

}
