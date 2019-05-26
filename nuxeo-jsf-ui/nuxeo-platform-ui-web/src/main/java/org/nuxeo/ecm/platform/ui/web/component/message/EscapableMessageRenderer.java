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
 *     Andre Justo
 */
package org.nuxeo.ecm.platform.ui.web.component.message;

import com.sun.faces.renderkit.html_basic.MessageRenderer;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.context.ResponseWriterWrapper;
import javax.faces.render.FacesRenderer;
import java.io.IOException;

/**
 * Custom MessageRenderer that allows the use of HTML in <h:message>
 *
 * @since 7.3
 */
@FacesRenderer(componentFamily="javax.faces.Message", rendererType="javax.faces.Message")
public class EscapableMessageRenderer extends MessageRenderer {

    @Override
    public void encodeEnd(FacesContext context, UIComponent component) throws IOException {
        final ResponseWriter originalResponseWriter = context.getResponseWriter();

        try {
            context.setResponseWriter(new ResponseWriterWrapper() {

                @Override
                public ResponseWriter getWrapped() {
                    return originalResponseWriter;
                }

                @Override
                public void writeText(Object text, UIComponent component, String property) throws IOException {
                    String string = String.valueOf(text);
                    String escape = (String) component.getAttributes().get("escape");
                    if (escape != null && !Boolean.valueOf(escape)) {
                        super.write(string);
                    } else {
                        super.writeText(string, component, property);
                    }
                }
            });

            super.encodeEnd(context, component); // Now, render it!
        } finally {
            context.setResponseWriter(originalResponseWriter); // Restore original writer.
        }
    }
}
