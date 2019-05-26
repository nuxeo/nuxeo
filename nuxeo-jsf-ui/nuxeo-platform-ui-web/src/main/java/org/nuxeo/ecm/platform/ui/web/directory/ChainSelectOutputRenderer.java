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

import org.apache.commons.lang3.StringUtils;

/**
 * Renderer for directory entry.
 *
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 */
public class ChainSelectOutputRenderer extends Renderer {

    @Override
    public void encodeBegin(FacesContext context, UIComponent component) throws IOException {
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
                String concat = StringUtils.join(((String[]) rawValues), comp.getKeySeparator());
                values = new String[] { concat };
            }
        } else {
            values = new String[] { rawValues.toString() };
        }
        String entrySeparator = comp.getEntrySeparator();
        String cssStyle = comp.getCssStyle();
        String cssStyleClass = comp.getCssStyleClass();

        @SuppressWarnings("resource")
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
