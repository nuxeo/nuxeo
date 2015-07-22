/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package org.nuxeo.theme.migration.tag.handler;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagConfig;

import org.nuxeo.ecm.platform.ui.web.tag.handler.TagConfigFactory;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.logging.DeprecationLogger;
import org.nuxeo.theme.styling.service.ThemeStylingService;

import com.sun.faces.facelets.tag.TagAttributeImpl;
import com.sun.faces.facelets.tag.TagAttributesImpl;
import com.sun.faces.facelets.tag.TagHandlerImpl;
import com.sun.faces.facelets.tag.ui.CompositionHandler;

/**
 * Theme composition migration handler.
 * <p>
 * Includes the new page template for compat and issues a warning in dev mode.
 *
 * @author Anahide Tchertchian
 */
public class ThemeMigrationCompositionHandler extends TagHandlerImpl {

    protected static final String NEG_PROP = "jsfThemeCompatTemplate";

    protected static final String TEMPLATE = "/pages/workspace_page.xhtml";

    protected final TagConfig config;

    public ThemeMigrationCompositionHandler(TagConfig config) {
        super(config);
        this.config = config;
    }

    @Override
    public void apply(FaceletContext ctx, UIComponent parent) throws IOException {
        String template = TEMPLATE;
        ThemeStylingService s = Framework.getService(ThemeStylingService.class);
        if (s != null) {
            template = s.negotiate(NEG_PROP, ctx.getFacesContext());
        }
        DeprecationLogger.log(String.format(
                "Tag nxthemes:composition is deprecated, will use a composition of template at %s for %s", template,
                tag.getLocation()), "7.4");
        TagAttributeImpl tAttr = getTagAttribute("template", template);
        TagAttributesImpl attributes = new TagAttributesImpl(new TagAttribute[] { tAttr });
        TagConfig cconfig = TagConfigFactory.createTagConfig(config, tagId, attributes, nextHandler);
        new CompositionHandler(cconfig).apply(ctx, parent);
    }

    protected TagAttributeImpl getTagAttribute(String name, String value) {
        return new TagAttributeImpl(tag.getLocation(), "", name, name, value);
    }

}
