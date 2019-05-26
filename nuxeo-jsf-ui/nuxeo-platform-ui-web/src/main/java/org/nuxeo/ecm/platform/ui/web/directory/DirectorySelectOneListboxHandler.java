/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.ui.web.directory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlSelectOneListbox;
import javax.faces.view.facelets.ComponentConfig;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletHandler;
import javax.faces.view.facelets.MetaRuleset;
import javax.faces.view.facelets.MetaTagHandler;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagAttributes;
import javax.faces.view.facelets.TagConfig;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.ui.web.tag.handler.GenericHtmlComponentHandler;
import org.nuxeo.ecm.platform.ui.web.tag.handler.TagConfigFactory;

import com.sun.faces.facelets.tag.TagAttributeImpl;
import com.sun.faces.facelets.tag.TagAttributesImpl;

/**
 * Legacy class for nxdir:selectOneListbox tag
 *
 * @since 6.0
 */
public class DirectorySelectOneListboxHandler extends MetaTagHandler {

    private static final Log log = LogFactory.getLog(DirectorySelectOneListboxHandler.class);

    protected enum DeprecatedPropertyKeys {
        displayValueOnly;
    }

    static final List<String> deprecatedProps = new ArrayList<>();
    static {
        for (DeprecatedPropertyKeys item : DeprecatedPropertyKeys.values()) {
            deprecatedProps.add(item.name());
        }
    }

    protected enum OptionPropertyKeys {
        directoryName, localize, displayIdAndLabel, ordering, caseSensistive,
        //
        displayObsoleteEntries, notDisplayDefaultOption, filter;
    }

    static List<String> optionProps = new ArrayList<>();
    static {
        for (OptionPropertyKeys item : OptionPropertyKeys.values()) {
            optionProps.add(item.name());
        }
    }

    protected final TagConfig tagConfig;

    protected List<TagAttribute> select;

    protected List<TagAttribute> options;

    public DirectorySelectOneListboxHandler(TagConfig config) {
        super(config);
        tagConfig = config;
        initAttributes(tag.getAttributes().getAll());
    }

    protected void initAttributes(TagAttribute[] attrs) {
        select = new ArrayList<>();
        options = new ArrayList<>();
        boolean orderingFound = false;
        if (attrs != null) {
            for (TagAttribute attr : attrs) {
                String name = attr.getLocalName();
                if (StringUtils.equals(name, OptionPropertyKeys.ordering.name())) {
                    orderingFound = true;
                }
                if (optionProps.contains(name)) {
                    options.add(attr);
                } else {
                    if (deprecatedProps.contains(name)) {
                        log.error(String.format("Property %s is not taken into account "
                                + "anymore on tag nxdir:selectOneListbox", name));
                    }
                    select.add(attr);
                }
            }
        }
        // add default needed values for lookup to work ok
        options.add(getTagAttribute("var", "item"));
        options.add(getTagAttribute("itemValue", "#{item.id}"));
        // Check if the attribute "ordering" has been defined, otherwise, the default value is added
        if (!orderingFound) {
            options.add(getTagAttribute(OptionPropertyKeys.ordering.name(), "label"));
        }
    }

    protected TagAttribute getTagAttribute(String name, String value) {
        return new TagAttributeImpl(tagConfig.getTag().getLocation(), tagConfig.getTag().getNamespace(), name, name,
                value);
    }

    @Override
    public void apply(FaceletContext ctx, UIComponent parent) throws IOException {
        // generate component handlers to be used instead
        TagAttributes optionAttributes = new TagAttributesImpl(options.toArray(new TagAttribute[] {}));
        ComponentConfig optionsConfig = TagConfigFactory.createComponentConfig(tagConfig, tagConfig.getTagId(),
                optionAttributes, new FaceletHandler() {
                    @Override
                    public void apply(FaceletContext ctx, UIComponent parent) throws IOException {
                        // do nothing
                    }
                }, UIDirectorySelectItems.COMPONENT_TYPE, null);
        FaceletHandler optionsHandler = new GenericHtmlComponentHandler(optionsConfig);
        TagAttributes selectAttributes = new TagAttributesImpl(select.toArray(new TagAttribute[] {}));
        ComponentConfig selectConfig = TagConfigFactory.createComponentConfig(tagConfig, tagConfig.getTagId(),
                selectAttributes, optionsHandler, getSelectComponentType(), null);
        new GenericHtmlComponentHandler(selectConfig).apply(ctx, parent);
    }

    protected String getSelectComponentType() {
        return HtmlSelectOneListbox.COMPONENT_TYPE;
    }

    @Override
    @SuppressWarnings("rawtypes")
    protected MetaRuleset createMetaRuleset(Class type) {
        return null;
    }

}
