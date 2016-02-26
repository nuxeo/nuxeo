/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.event.ComponentSystemEventListener;
import javax.faces.event.ListenerFor;
import javax.faces.event.PostAddToViewEvent;

import org.apache.commons.lang.StringEscapeUtils;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.nuxeo.runtime.api.Framework;

import com.sun.faces.renderkit.html_basic.MessagesRenderer;

/**
 * Handles rendering of {@link javax.faces.application.FacesMessage} through jQuery Ambiance plugin.
 *
 * @since 5.7.3
 */
@ListenerFor(systemEventClass = PostAddToViewEvent.class)
public class NXMessagesRenderer extends MessagesRenderer implements ComponentSystemEventListener {

    public static final String RENDERER_TYPE = "javax.faces.NXMessages";

    @Override
    public void encodeEnd(FacesContext context, UIComponent component) throws IOException {
        rendererParamsNotNull(context, component);

        if (!shouldEncode(component)) {
            return;
        }

        // If id is user specified, we must render
        boolean mustRender = shouldWriteIdAttribute(component);

        UIMessages messages = (UIMessages) component;
        ResponseWriter writer = context.getResponseWriter();
        assert(writer != null);

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
        Iterator<?> messageIter = getMessageIter(context, clientId, component);

        assert(messageIter != null);

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
            String summary = (null != (summary = curMessage.getSummary())) ? summary : "";
            // Default to summary if we have no detail
            String detail = (null != (detail = curMessage.getDetail())) ? detail : summary;

            String severityStyleClass = null;
            String errorType = "default";
            long timeout = 5;
            if (curMessage.getSeverity() == FacesMessage.SEVERITY_INFO) {
                severityStyleClass = (String) component.getAttributes().get("infoClass");
                errorType = "info";
            } else if (curMessage.getSeverity() == FacesMessage.SEVERITY_WARN) {
                severityStyleClass = (String) component.getAttributes().get("warnClass");
                errorType = "warn";
            } else if (curMessage.getSeverity() == FacesMessage.SEVERITY_ERROR) {
                severityStyleClass = (String) component.getAttributes().get("errorClass");
                errorType = "error";
                timeout = 0;
            } else if (curMessage.getSeverity() == FacesMessage.SEVERITY_FATAL) {
                severityStyleClass = (String) component.getAttributes().get("fatalClass");
                errorType = "fatal";
                timeout = 0;
            }

            // ensure message stays visible when running tests
            if (Framework.getProperty("org.nuxeo.ecm.tester.name") != null) {
                timeout = 0;
            }

            writer.startElement("script", messages);
            writer.writeAttribute("type", "text/javascript", null);
            String message = "";
            String scriptContent = new StringBuilder().append("jQuery(document).ready(function() {\n")
                                                      .append("  jQuery.ambiance({\n")
                                                      .append("    message: \"")
                                                      .append(message)
                                                      .append("\",\n")
                                                      .append("    title: \"")
                                                      .append(StringEscapeUtils.escapeJavaScript(summary))
                                                      .append("\",\n")
                                                      .append("    type: \"")
                                                      .append(errorType)
                                                      .append("\",\n")
                                                      .append("    className: \"")
                                                      .append(severityStyleClass)
                                                      .append("\",\n")
                                                      .append("    timeout: \"")
                                                      .append(timeout)
                                                      .append("\"")
                                                      .append("  })\n")
                                                      .append("});\n")
                                                      .toString();
            if (showDetail) {
                message = String.format(scriptContent,
                        StringEscapeUtils.escapeJavaScript(StringEscapeUtils.escapeHtml(detail)),
                        StringEscapeUtils.escapeJavaScript(StringEscapeUtils.escapeHtml(summary)), errorType,
                        severityStyleClass, timeout);
            } else {
                message = String.format(scriptContent, "",
                        StringEscapeUtils.escapeJavaScript(StringEscapeUtils.escapeHtml(summary)), errorType,
                        severityStyleClass, timeout);
            }
            writer.writeText(message, null);
            writer.endElement("script");
        }
    }

    /*
     * When this method is called, we know that there is a component with a script renderer somewhere in the view. We
     * need to make it so that when an element with a name given by the value of the optional "target" component
     * attribute is encountered, this component can be called upon to render itself. This method will add the component
     * (associated with this Renderer) to a facet in the view only if a "target" component attribute is set.
     */
    public void processEvent(ComponentSystemEvent event) throws AbortProcessingException {
        UIComponent component = event.getComponent();
        if (ComponentUtils.isRelocated(component)) {
            return;
        }
        String target = verifyTarget((String) component.getAttributes().get("target"));
        if (target != null) {
            ComponentUtils.relocate(component, target, null);
        }
    }

    protected String verifyTarget(String toVerify) {
        return ComponentUtils.verifyTarget(toVerify, toVerify);
    }

}
