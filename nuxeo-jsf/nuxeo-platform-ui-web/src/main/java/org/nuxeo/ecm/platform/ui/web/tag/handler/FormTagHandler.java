/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.ui.web.tag.handler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.component.UIForm;
import javax.faces.component.html.HtmlForm;
import javax.faces.view.facelets.ComponentConfig;
import javax.faces.view.facelets.ComponentHandler;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.MetaRuleset;
import javax.faces.view.facelets.MetaTagHandler;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagConfig;

import com.sun.faces.facelets.tag.TagAttributeImpl;
import com.sun.faces.facelets.tag.TagAttributesImpl;

/**
 * Tag handler generating a form (or not) depending on attributes values, useful when handling widgets that may need to
 * be surrounded by a form (or not) depending on their configuration.
 *
 * @since 8.2
 */
public class FormTagHandler extends MetaTagHandler {

    protected final TagConfig config;

    protected final TagAttribute id;

    protected final TagAttribute skip;

    protected final TagAttribute disableDoubleClickShield;

    protected final TagAttribute useAjaxForm;

    protected final TagAttribute disableMultipartForm;

    protected final TagAttribute onsubmit;

    protected final TagAttribute styleClass;

    public FormTagHandler(TagConfig config) {
        super(config);
        this.config = config;

        id = getAttribute("id");
        skip = getAttribute("skip");
        disableDoubleClickShield = getAttribute("disableDoubleClickShield");
        useAjaxForm = getAttribute("useAjaxForm");
        disableMultipartForm = getAttribute("disableMultipartForm");
        onsubmit = getAttribute("onsubmit");
        styleClass = getAttribute("styleClass");
    }

    @Override
    public void apply(FaceletContext ctx, UIComponent parent) throws IOException {
        // resolve addForm early
        boolean doAdd = true;
        if (skip != null) {
            doAdd = !skip.getBoolean(ctx);
        }

        if (doAdd) {
            // wrap in a Form component handler
            List<TagAttribute> attrs = new ArrayList<TagAttribute>();
            attrs.addAll(copyAttributes(id, disableDoubleClickShield, onsubmit, styleClass));

            // resolve ajax and multipart behaviors early too
            boolean useMultiPart = true;
            if (useAjaxForm != null && useAjaxForm.getBoolean(ctx)) {
                useMultiPart = false;
            }
            if (disableMultipartForm != null && disableMultipartForm.getBoolean(ctx)) {
                useMultiPart = false;
            }
            if (useMultiPart) {
                attrs.add(createAttribute("enctype", "multipart/form-data"));
            }
            ComponentConfig cconfig = TagConfigFactory.createComponentConfig(config, tagId,
                    new TagAttributesImpl(attrs.toArray(new TagAttribute[] {})), nextHandler, HtmlForm.COMPONENT_TYPE,
                    UIForm.COMPONENT_TYPE);
            new ComponentHandler(cconfig).apply(ctx, parent);
        } else {
            // apply next directly
            this.nextHandler.apply(ctx, parent);
        }

    }

    protected TagAttribute copyAttribute(TagAttribute attribute) {
        return new TagAttributeImpl(config.getTag().getLocation(), "", attribute.getLocalName(),
                attribute.getLocalName(), attribute.getValue());
    }

    protected List<TagAttribute> copyAttributes(TagAttribute... attributes) {
        List<TagAttribute> res = new ArrayList<TagAttribute>();
        if (attributes != null) {
            for (TagAttribute attr : attributes) {
                if (attr != null) {
                    res.add(copyAttribute(attr));
                }
            }
        }
        return res;
    }

    protected TagAttribute createAttribute(String name, String value) {
        return new TagAttributeImpl(config.getTag().getLocation(), "", name, name, value);
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected MetaRuleset createMetaRuleset(Class type) {
        return null;
    }

}
