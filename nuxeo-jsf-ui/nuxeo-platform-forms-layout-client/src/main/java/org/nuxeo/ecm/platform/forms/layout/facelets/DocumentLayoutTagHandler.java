/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id: DocumentLayoutTagHandler.java 26053 2007-10-16 01:45:43Z atchertchian $
 */

package org.nuxeo.ecm.platform.forms.layout.facelets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.el.ELException;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import javax.faces.view.facelets.CompositeFaceletHandler;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletHandler;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagAttributes;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagHandler;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.forms.layout.api.BuiltinModes;
import org.nuxeo.ecm.platform.types.adapter.TypeInfo;
import org.nuxeo.ecm.platform.ui.web.tag.handler.TagConfigFactory;

/**
 * Document layout tag handler.
 * <p>
 * Computes layouts in given facelet context, for given mode and document attributes.
 * <p>
 * Document must be resolved at the component tree construction so it cannot be bound to an iteration value.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class DocumentLayoutTagHandler extends TagHandler {

    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(DocumentLayoutTagHandler.class);

    protected final TagConfig config;

    protected final TagAttribute mode;

    protected final TagAttribute documentMode;

    protected final TagAttribute documentModeFallback;

    protected final TagAttribute value;

    protected final TagAttribute template;

    /**
     * @since 5.4.2
     */
    protected final TagAttribute defaultLayout;

    /**
     * @since 5.4.2
     */
    protected final TagAttribute includeAnyMode;

    protected final TagAttribute[] vars;

    protected final String[] reservedVarsArray = { "id", "name", "mode", "documentMode", "value", "template",
            "defaultLayout", "includeAnyMode" };

    public DocumentLayoutTagHandler(TagConfig config) {
        super(config);
        this.config = config;
        mode = getRequiredAttribute("mode");
        documentMode = getAttribute("documentMode");
        documentModeFallback = getAttribute("documentModeFallback");
        value = getRequiredAttribute("value");
        template = getAttribute("template");
        defaultLayout = getAttribute("defaultLayout");
        includeAnyMode = getAttribute("includeAnyMode");
        vars = tag.getAttributes().getAll();
    }

    /**
     * If resolved document has layouts, apply each of them.
     */
    @Override
    public void apply(FaceletContext ctx, UIComponent parent) throws IOException, FacesException, ELException {
        Object document = value.getObject(ctx, DocumentModel.class);
        if (!(document instanceof DocumentModel)) {
            return;
        }

        TypeInfo typeInfo = ((DocumentModel) document).getAdapter(TypeInfo.class);
        if (typeInfo == null) {
            return;
        }
        String modeValue = mode.getValue(ctx);
        String documentModeValue = null;
        if (documentMode != null) {
            documentModeValue = documentMode.getValue(ctx);
        }
        String documentModeFallbackValue = null;
        if (documentModeFallback != null) {
            documentModeFallbackValue = documentModeFallback.getValue(ctx);
        }
        boolean useAnyMode = true;
        if (includeAnyMode != null) {
            useAnyMode = includeAnyMode.getBoolean(ctx);
        }
        String defaultMode = documentModeFallbackValue;
        if (StringUtils.isBlank(defaultMode) && useAnyMode) {
            defaultMode = BuiltinModes.ANY;
        }
        String[] layoutNames = typeInfo.getLayouts(documentModeValue == null ? modeValue : documentModeValue,
                defaultMode);
        if (layoutNames == null || layoutNames.length == 0) {
            // fallback on default layout
            if (defaultLayout != null) {
                layoutNames = new String[] { defaultLayout.getValue() };
            } else {
                // no layout => do nothing
                return;
            }
        }

        FaceletHandlerHelper helper = new FaceletHandlerHelper(config);
        TagAttribute modeAttr = helper.createAttribute("mode", modeValue);
        List<FaceletHandler> handlers = new ArrayList<>();
        FaceletHandler leaf = nextHandler;
        for (String layoutName : layoutNames) {
            TagAttributes attributes = FaceletHandlerHelper.getTagAttributes(
                    helper.createAttribute("name", layoutName), modeAttr, value);
            if (template != null) {
                attributes = FaceletHandlerHelper.addTagAttribute(attributes, template);
            }
            // add other variables put on original tag
            List<String> reservedVars = Arrays.asList(reservedVarsArray);
            for (TagAttribute var : vars) {
                String localName = var.getLocalName();
                if (!reservedVars.contains(localName)) {
                    attributes = FaceletHandlerHelper.addTagAttribute(attributes, var);
                }
            }
            TagConfig tagConfig = TagConfigFactory.createTagConfig(config, null, attributes, leaf);
            handlers.add(new LayoutTagHandler(tagConfig));
        }
        CompositeFaceletHandler composite = new CompositeFaceletHandler(handlers.toArray(new FaceletHandler[0]));
        composite.apply(ctx, parent);
    }
}
