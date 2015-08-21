/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.platform.forms.layout.facelets.library;

import javax.faces.FacesException;
import javax.faces.view.facelets.TagAttributes;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagHandler;

import org.nuxeo.ecm.platform.forms.layout.api.service.LayoutStore;
import org.nuxeo.ecm.platform.forms.layout.facelets.FaceletHandlerHelper;
import org.nuxeo.ecm.platform.forms.layout.facelets.WidgetTypeTagHandler;
import org.nuxeo.ecm.platform.forms.layout.service.WebLayoutManager;
import org.nuxeo.ecm.platform.ui.web.tag.handler.TagConfigFactory;
import org.nuxeo.runtime.api.Framework;

import com.sun.faces.facelets.tag.AbstractTagLibrary;

/**
 * Tag library implementation to register tags for registered widget types on the fly.
 *
 * @since 8.1
 */
public class RuntimeWidgetTypeTagLibrary extends AbstractTagLibrary {

    public final static String Namespace = "http://nuxeo.org/nxforms/runtime/widgettype";

    public RuntimeWidgetTypeTagLibrary() {
        this(Namespace);
    }

    public RuntimeWidgetTypeTagLibrary(String namespace) {
        super(namespace);
    }

    @Override
    public boolean containsTagHandler(String ns, String localName) {
        if (getNamespace().equals(ns)) {
            LayoutStore service = Framework.getService(LayoutStore.class);
            return service.getWidgetType(getWidgetTypeCategory(localName), getWidgetTypeName(localName)) != null;
        }
        return false;
    }

    @Override
    public TagHandler createTagHandler(String ns, String localName, TagConfig tag) throws FacesException {
        FaceletHandlerHelper helper = new FaceletHandlerHelper(tag);
        TagAttributes attributes = tag.getTag().getAttributes();
        attributes = FaceletHandlerHelper.addTagAttribute(attributes,
                helper.createAttribute("name", getWidgetTypeName(localName)));
        attributes = FaceletHandlerHelper.addTagAttribute(attributes,
                helper.createAttribute("category", getWidgetTypeCategory(localName)));
        TagConfig config = TagConfigFactory.createTagConfig(tag, tag.getTagId(), attributes, tag.getNextHandler());
        WidgetTypeTagHandler h = new WidgetTypeTagHandler(config);
        return h;
    }

    protected String getWidgetTypeName(String tagLocalName) {
        // XXX maybe handle category by parsing local name
        return tagLocalName;
    }

    protected String getWidgetTypeCategory(String tagLocalName) {
        // XXX maybe handle category by parsing local name
        return WebLayoutManager.JSF_CATEGORY;
    }

}
