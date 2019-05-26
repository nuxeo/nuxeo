/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.forms.layout.facelets.dev;

import java.io.IOException;

import javax.el.ELException;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import javax.faces.view.facelets.ComponentHandler;
import javax.faces.view.facelets.CompositeFaceletHandler;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletException;
import javax.faces.view.facelets.FaceletHandler;
import javax.faces.view.facelets.TagAttributes;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagHandler;

import org.nuxeo.ecm.platform.forms.layout.facelets.FaceletHandlerHelper;

/**
 * Dev tag container, displaying a div and decorating the dev handler for dev mode rendering, and displaying the
 * original handler (layout or widget handler) after that.
 *
 * @since 6.0
 */
public class DevTagHandler extends TagHandler {

    protected final TagConfig config;

    protected final String refId;

    protected final FaceletHandler originalHandler;

    protected final FaceletHandler devHandler;

    protected static final String PANEL_COMPONENT_TYPE = "org.richfaces.OutputPanel";

    public DevTagHandler(TagConfig config, String refId, FaceletHandler originalHandler, FaceletHandler devHandler) {
        super(config);
        this.refId = refId;
        this.config = config;
        this.originalHandler = originalHandler;
        this.devHandler = devHandler;
    }

    @Override
    public void apply(FaceletContext ctx, UIComponent parent) throws IOException, FacesException, FaceletException,
            ELException {
        FaceletHandlerHelper helper = new FaceletHandlerHelper(config);
        TagAttributes devAttrs = FaceletHandlerHelper.getTagAttributes(
                helper.createAttribute("id", FaceletHandlerHelper.generateDevContainerId(ctx, refId)),
                helper.createAttribute("styleClass", "displayN nxlDevContainer"),
                helper.createAttribute("layout", "block"));
        ComponentHandler dComp = helper.getHtmlComponentHandler(config.getTagId(), devAttrs, devHandler,
                PANEL_COMPONENT_TYPE, null);
        FaceletHandler nextHandler = new CompositeFaceletHandler(new FaceletHandler[] {
                helper.getDisableDevModeTagHandler(config.getTagId(), dComp), originalHandler });
        TagAttributes cAttrs = FaceletHandlerHelper.getTagAttributes(
                helper.createAttribute("id", FaceletHandlerHelper.generateDevRegionId(ctx, refId)),
                helper.createAttribute("styleClass", "nxlDevRegion"), helper.createAttribute("layout", "block"));
        ComponentHandler cComp = helper.getHtmlComponentHandler(config.getTagId(), cAttrs, nextHandler,
                PANEL_COMPONENT_TYPE, null);
        cComp.apply(ctx, parent);
    }

}
