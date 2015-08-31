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
package org.nuxeo.ecm.platform.ui.web.tag.handler;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletHandler;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagHandler;

/**
 * Composite facelet handler which is also a tag handler.
 *
 * @since 7.4
 */
public final class CompositeTagHandler extends TagHandler {

    private final FaceletHandler[] children;

    private final int len;

    public CompositeTagHandler(TagConfig config, FaceletHandler[] children) {
        super(config);
        this.children = children;
        this.len = children.length;
    }

    public void apply(FaceletContext ctx, UIComponent parent) throws IOException {
        for (int i = 0; i < len; i++) {
            this.children[i].apply(ctx, parent);
        }
    }

    public FaceletHandler[] getHandlers() {
        return this.children;
    }

}