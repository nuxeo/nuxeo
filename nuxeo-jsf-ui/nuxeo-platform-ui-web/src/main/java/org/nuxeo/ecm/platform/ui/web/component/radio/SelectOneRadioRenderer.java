/*
 * (C) Copyright 2014 JBoss RichFaces and others.
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

package org.nuxeo.ecm.platform.ui.web.component.radio;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlSelectOneRadio;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.richfaces.renderkit.InputRendererBase;

/**
 * Renderer for a specific radio component, skipping the original rendering, that will be handled by related
 * {@link UIRadio} component(s).
 *
 * @since 6.0
 */
public class SelectOneRadioRenderer extends InputRendererBase {

    public static final String RENDERER_TYPE = SelectOneRadioRenderer.class.getName();

    @Override
    protected void doEncodeEnd(ResponseWriter writer, FacesContext context, UIComponent component) throws IOException {
        // do nothing
    }

    @Override
    protected void doDecode(FacesContext context, UIComponent component) {
        if (component instanceof HtmlSelectOneRadio) {
            HtmlSelectOneRadio select = (HtmlSelectOneRadio) component;
            if (select.isDisabled() || select.isReadonly() || !select.isRendered()) {
                return;
            }
        } else if (!component.isRendered()) {
            return;
        }
        super.doDecode(context, component);
    }
}
