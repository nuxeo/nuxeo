/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
