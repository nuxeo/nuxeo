/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.platform.ui.web.component.message;

import java.io.IOException;
import java.util.Iterator;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIMessages;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.nuxeo.runtime.api.Framework;

import com.sun.faces.renderkit.html_basic.MessagesRenderer;

/**
 * Handles rendering of {@link javax.faces.application.FacesMessage} through
 * jQuery Ambiance plugin.
 *
 * @since 5.7.3
 */
public class NXMessagesRenderer extends MessagesRenderer {

    public static final String RENDERER_TYPE = "javax.faces.NXMessages";

    @Override
    public void encodeEnd(FacesContext context, UIComponent component)
            throws IOException {
        rendererParamsNotNull(context, component);

        if (!shouldEncode(component)) {
            return;
        }

        // If id is user specified, we must render
        boolean mustRender = shouldWriteIdAttribute(component);

        UIMessages messages = (UIMessages) component;
        ResponseWriter writer = context.getResponseWriter();
        assert (writer != null);

        String clientId = ((UIMessages) component).getFor();
        // if no clientId was included
        if (clientId == null) {
            // and the author explicitly only wants global messages
            if (messages.isGlobalOnly()) {
                // make it so only global messages get displayed.
                clientId = "";
            }
        }

        // "for" attribute optional for Messages
        Iterator messageIter = getMessageIter(context, clientId, component);

        assert (messageIter != null);

        if (!messageIter.hasNext()) {
            if (mustRender) {
                // no message to render, but must render anyway
                // but if we're writing the dev stage messages,
                // only write it if messages exist
                if ("javax_faces_developmentstage_messages".equals(component.getId())) {
                    return;
                }
                writer.startElement("div", component);
                writeIdAttributeIfNecessary(context, writer, component);
                writer.endElement("div");
            } // otherwise, return without rendering
            return;
        }

        boolean showDetail = messages.isShowDetail();

        while (messageIter.hasNext()) {
            FacesMessage curMessage = (FacesMessage) messageIter.next();
            if (curMessage.isRendered() && !messages.isRedisplay()) {
                continue;
            }
            curMessage.rendered();

            // make sure we have a non-null value for summary and
            // detail.
            String summary = (null != (summary = curMessage.getSummary())) ? summary
                    : "";
            // Default to summary if we have no detail
            String detail = (null != (detail = curMessage.getDetail())) ? detail
                    : summary;

            String severityStyleClass = null;
            String errorType = "default";
            long timeout = 5;
            if (curMessage.getSeverity() == FacesMessage.SEVERITY_INFO) {
                severityStyleClass = (String) component.getAttributes().get(
                        "infoClass");
                errorType = "info";
            } else if (curMessage.getSeverity() == FacesMessage.SEVERITY_WARN) {
                severityStyleClass = (String) component.getAttributes().get(
                        "warnClass");
                errorType = "warn";
            } else if (curMessage.getSeverity() == FacesMessage.SEVERITY_ERROR) {
                severityStyleClass = (String) component.getAttributes().get(
                        "errorClass");
                errorType = "error";
                timeout = 0;
            } else if (curMessage.getSeverity() == FacesMessage.SEVERITY_FATAL) {
                severityStyleClass = (String) component.getAttributes().get(
                        "fatalClass");
                errorType = "fatal";
                timeout = 0;
            }

            // ensure message stays visible when running tests
            if (Framework.getProperty("org.nuxeo.ecm.tester.name") != null) {
                timeout = 0;
            }

            writer.startElement("script", messages);
            writer.writeAttribute("type", "text/javascript", null);

            String scriptContent = "jQuery(document).ready(function() {\n"
                    + "  jQuery.ambiance({\n" + "    " + "message: \"%s\",\n"
                    + "    title: \"%s\",\n" + "    type: \"%s\",\n"
                    + "    className: \"%s\",\n" + "    timeout: \"%d\""
                    + "  })\n" + "});\n";
            String formattedScriptContent;
            if (showDetail) {
                formattedScriptContent = String.format(scriptContent, detail,
                        summary, errorType, severityStyleClass, timeout);
            } else {
                formattedScriptContent = String.format(scriptContent, "",
                        summary, errorType, severityStyleClass, timeout);
            }
            writer.writeText(formattedScriptContent, null);
            writer.endElement("script");

        }
    }
}
