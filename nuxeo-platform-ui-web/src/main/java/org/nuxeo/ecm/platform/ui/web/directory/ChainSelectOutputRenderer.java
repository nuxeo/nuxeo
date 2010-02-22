/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: ChainSelectOutputRenderer.java 23042 2007-07-27 13:18:01Z glefter $
 */

package org.nuxeo.ecm.platform.ui.web.directory;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.render.Renderer;

import org.apache.commons.lang.StringUtils;

/**
 * Renderer for directory entry.
 *
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 *
 */
public class ChainSelectOutputRenderer extends Renderer {

    @Override
    public void encodeBegin(FacesContext context, UIComponent component)
            throws IOException {
        ChainSelectOutputComponent comp = (ChainSelectOutputComponent) component;
        Object rawValues = comp.getValue();
        if (rawValues == null) {
            return;
        }

        String[] values;
        if (rawValues instanceof String[]) {
            if (comp.getHandleMultipleValues()) {
                // treat rawValues as separate entries (potentially holding
                // several keys)
                values = (String[]) rawValues;
            } else {
                // treat rawValues as labels to be concatenated on the same
                // entry
                String concat = StringUtils.join(((String[]) rawValues),
                        comp.getKeySeparator());
                values = new String[] { concat };
            }
        } else {
            values = new String[] { rawValues.toString() };
        }
        String entrySeparator = comp.getEntrySeparator();
        String cssStyle = comp.getCssStyle();
        String cssStyleClass = comp.getCssStyleClass();

        ResponseWriter writer = context.getResponseWriter();

        for (String value : values) {
            writer.startElement("div", comp);
            if (cssStyle != null) {
                writer.writeAttribute("style", cssStyle, "style");
            }
            if (cssStyleClass != null) {
                writer.writeAttribute("class", cssStyleClass, "class");
            }

            Selection sel = comp.createSelection(value);
            String[] labels = sel.getLabels();
            String label = StringUtils.join(labels, comp.getKeySeparator());
            writer.writeText(label, null);
            writer.writeText(entrySeparator, null);

            writer.endElement("div");
        }
        writer.flush();
    }

}
