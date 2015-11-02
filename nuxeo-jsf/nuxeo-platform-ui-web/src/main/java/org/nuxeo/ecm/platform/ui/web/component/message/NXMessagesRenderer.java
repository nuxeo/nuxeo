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
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.event.ComponentSystemEventListener;
import javax.faces.event.ListenerFor;
import javax.faces.event.PostAddToViewEvent;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
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

    private static final String COMP_KEY = NXMessagesRenderer.class.getName() + "_COMPOSITE_COMPONENT";

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

            String scriptContent = "jQuery(document).ready(function() {\n" + "  jQuery.ambiance({\n" + "    "
                    + "message: \"%s\",\n" + "    title: \"%s\",\n" + "    type: \"%s\",\n" + "    className: \"%s\",\n"
                    + "    timeout: \"%d\"" + "  })\n" + "});\n";
            String formattedScriptContent;
            if (showDetail) {
                formattedScriptContent = String.format(scriptContent, StringEscapeUtils.escapeJavaScript(detail),
                        StringEscapeUtils.escapeJavaScript(summary), errorType, severityStyleClass, timeout);
            } else {
                formattedScriptContent = String.format(scriptContent, "", StringEscapeUtils.escapeJavaScript(summary),
                        errorType, severityStyleClass, timeout);
            }
            writer.writeText(formattedScriptContent, null);
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
        FacesContext context = FacesContext.getCurrentInstance();

        String target = verifyTarget((String) component.getAttributes().get("target"));
        if (target != null) {
            // We're checking for a composite component here as if the resource
            // is relocated, it may still require it's composite component context
            // in order to properly render. Store it for later use by
            // encodeBegin() and encodeEnd().
            UIComponent cc = UIComponent.getCurrentCompositeComponent(context);
            if (cc != null) {
                component.getAttributes().put(COMP_KEY, cc.getClientId(context));
            }
            context.getViewRoot().addComponentResource(context, component, target);

        }
    }

    protected String verifyTarget(String toVerify) {
        if (StringUtils.isBlank(toVerify)) {
            return null;
        }
        FacesContext context = FacesContext.getCurrentInstance();
        boolean ajaxRequest = context.getPartialViewContext().isAjaxRequest();
        if (ajaxRequest) {
            // ease up ajax re-rendering in case of js scripts parsing defer
            return null;
        }
        return toVerify;
    }

}
