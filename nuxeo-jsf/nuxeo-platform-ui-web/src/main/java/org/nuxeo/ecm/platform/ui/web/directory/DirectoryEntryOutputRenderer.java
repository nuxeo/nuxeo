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
 * $Id: DirectoryEntryOutputRenderer.java 29611 2008-01-24 16:51:03Z gracinet $
 */

package org.nuxeo.ecm.platform.ui.web.directory;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.render.Renderer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.i18n.I18NUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.DirectoryException;

import com.sun.faces.renderkit.Attribute;
import com.sun.faces.renderkit.AttributeManager;
import com.sun.faces.renderkit.RenderKitUtils;

/**
 * Renderer for directory entry.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class DirectoryEntryOutputRenderer extends Renderer {

    private static final Log log = LogFactory.getLog(DirectoryHelper.class);

    private static final Attribute[] OUTPUT_ATTRIBUTES = AttributeManager.getAttributes(AttributeManager.Key.OUTPUTTEXT);

    @Override
    public void encodeBegin(FacesContext context, UIComponent component) throws IOException {
        String toWrite = getEntryLabel(context, component);

        ResponseWriter writer = context.getResponseWriter();
        boolean isOutput = false;

        String style = (String) component.getAttributes().get("style");
        String styleClass = (String) component.getAttributes().get("styleClass");
        String dir = (String) component.getAttributes().get("dir");
        String lang = (String) component.getAttributes().get("lang");
        String title = (String) component.getAttributes().get("title");
        Map<String, Object> passthroughAttributes = component.getPassThroughAttributes(false);
        boolean hasPassthroughAttributes = null != passthroughAttributes && !passthroughAttributes.isEmpty();

        boolean renderSpan = styleClass != null || style != null || dir != null || lang != null || title != null
                || hasPassthroughAttributes;
        if (renderSpan) {
            writer.startElement("span", component);
            if (null != styleClass) {
                writer.writeAttribute("class", styleClass, "styleClass");
            }
            // style is rendered as a passthru attribute
            RenderKitUtils.renderPassThruAttributes(context, writer, component, OUTPUT_ATTRIBUTES);

        }
        if (toWrite != null) {
            writer.write(toWrite);
        }

        if (renderSpan) {
            writer.endElement("span");
        }

    }

    @SuppressWarnings("deprecation")
    protected String getEntryLabel(FacesContext context, UIComponent component) {
        DirectoryEntryOutputComponent dirComponent = (DirectoryEntryOutputComponent) component;
        String entryId = (String) dirComponent.getValue();
        if (entryId == null) {
            // BBB
            entryId = dirComponent.getEntryId();
        }
        String directoryName = dirComponent.getDirectoryName();
        String toWrite = null;
        if (directoryName != null) {
            // get the entry information
            String keySeparator = (String) dirComponent.getAttributes().get("keySeparator");
            String schema;
            try {
                schema = DirectoryHelper.getDirectoryService().getDirectorySchema(directoryName);
            } catch (DirectoryException de) {
                log.error("Unable to get directory schema for " + directoryName, de);
                schema = keySeparator != null ? "xvocabulary" : "vocabulary";
            }
            if (keySeparator != null && entryId != null) {
                entryId = entryId.substring(entryId.lastIndexOf(keySeparator) + 1, entryId.length());
            }
            DocumentModel entry = DirectoryHelper.getEntry(directoryName, entryId);

            if (entry != null) {
                Boolean displayIdAndLabel = dirComponent.getDisplayIdAndLabel();
                if (displayIdAndLabel == null) {
                    displayIdAndLabel = Boolean.FALSE; // unboxed later
                }
                Boolean translate = dirComponent.getLocalize();

                String label;
                try {
                    label = (String) entry.getProperty(schema, "label");
                } catch (ClientException e) {
                    label = null;
                }
                String display = (String) dirComponent.getAttributes().get("display");
                if (label == null || "".equals(label)) {
                    label = entryId;
                }
                if (Boolean.TRUE.equals(translate)) {
                    label = translate(context, label);
                }
                toWrite = DirectoryHelper.getOptionValue(entryId, label, display, displayIdAndLabel.booleanValue(), " ");
            }
        }
        if (toWrite == null) {
            // default rendering: the entry id itself
            toWrite = entryId;
        }
        return toWrite;
    }

    protected static String translate(FacesContext context, String label) {
        String bundleName = context.getApplication().getMessageBundle();
        Locale locale = context.getViewRoot().getLocale();
        label = I18NUtils.getMessageString(bundleName, label, null, locale);
        return label;
    }

}
