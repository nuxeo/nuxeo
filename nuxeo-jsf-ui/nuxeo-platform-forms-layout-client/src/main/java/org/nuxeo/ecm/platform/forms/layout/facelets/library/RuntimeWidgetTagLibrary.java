/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.forms.layout.facelets.library;

import javax.faces.FacesException;
import javax.faces.view.facelets.TagAttributes;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagHandler;

import org.nuxeo.ecm.platform.forms.layout.api.service.LayoutStore;
import org.nuxeo.ecm.platform.forms.layout.facelets.FaceletHandlerHelper;
import org.nuxeo.ecm.platform.forms.layout.facelets.WidgetTagHandler;
import org.nuxeo.ecm.platform.forms.layout.service.WebLayoutManager;
import org.nuxeo.ecm.platform.ui.web.tag.handler.TagConfigFactory;
import org.nuxeo.runtime.api.Framework;

import com.sun.faces.facelets.tag.AbstractTagLibrary;

/**
 * Tag library implementation to register tags for registered widget types on the fly.
 *
 * @since 8.1
 */
public class RuntimeWidgetTagLibrary extends AbstractTagLibrary {

    public final static String Namespace = "http://nuxeo.org/nxforms/runtime/widget";

    public RuntimeWidgetTagLibrary() {
        this(Namespace);
    }

    public RuntimeWidgetTagLibrary(String namespace) {
        super(namespace);
    }

    @Override
    public boolean containsTagHandler(String ns, String localName) {
        if (getNamespace().equals(ns)) {
            LayoutStore service = Framework.getService(LayoutStore.class);
            return service.getWidgetDefinition(getWidgetCategory(localName), getWidgetName(localName)) != null;
        }
        return false;
    }

    @Override
    public TagHandler createTagHandler(String ns, String localName, TagConfig tag) throws FacesException {
        FaceletHandlerHelper helper = new FaceletHandlerHelper(tag);
        TagAttributes attributes = tag.getTag().getAttributes();
        attributes = FaceletHandlerHelper.addTagAttribute(attributes,
                helper.createAttribute("name", getWidgetName(localName)));
        attributes = FaceletHandlerHelper.addTagAttribute(attributes,
                helper.createAttribute("category", getWidgetCategory(localName)));
        TagConfig config = TagConfigFactory.createTagConfig(tag, tag.getTagId(), attributes, tag.getNextHandler());
        WidgetTagHandler h = new WidgetTagHandler(config);
        return h;
    }

    protected String getWidgetName(String tagLocalName) {
        // XXX maybe handle category by parsing local name
        return tagLocalName;
    }

    protected String getWidgetCategory(String tagLocalName) {
        // XXX maybe handle category by parsing local name
        return WebLayoutManager.JSF_CATEGORY;
    }

}
