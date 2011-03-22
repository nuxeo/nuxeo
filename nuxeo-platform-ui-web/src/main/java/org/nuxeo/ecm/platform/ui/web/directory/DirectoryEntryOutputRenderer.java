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

import java.util.Locale;

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

/**
 * Renderer for directory entry.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class DirectoryEntryOutputRenderer extends Renderer {

    private static final Log log = LogFactory.getLog(DirectoryHelper.class);

    @Override
    public void encodeBegin(FacesContext context, UIComponent component) {
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
            String keySeparator = (String) dirComponent.getAttributes().get(
                    "keySeparator");
            String schema;
            try {
                schema = DirectoryHelper.getDirectoryService().getDirectorySchema(
                        directoryName);
            } catch (DirectoryException de) {
                log.error(
                        "Unable to get directory schema for " + directoryName,
                        de);
                schema = keySeparator != null ? "xvocabulary" : "vocabulary";
            }
            if (keySeparator != null && entryId != null) {
                entryId = entryId.substring(
                        entryId.lastIndexOf(keySeparator) + 1, entryId.length());
            }
            DocumentModel entry = DirectoryHelper.getEntry(directoryName,
                    entryId);

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
                String display = (String) dirComponent.getAttributes().get(
                        "display");
                if (label == null || "".equals(label)) {
                    label = entryId;
                }
                if (Boolean.TRUE.equals(translate)) {
                    label = translate(context, label);
                }
                toWrite = DirectoryHelper.getOptionValue(entryId, label,
                        display, displayIdAndLabel.booleanValue(), " ");
            }
        }
        if (toWrite == null) {
            // default rendering: the entry id itself
            toWrite = entryId;
        }
        try {
            if (toWrite != null) {
                ResponseWriter writer = context.getResponseWriter();
                writer.writeText(toWrite, null);
                writer.flush();
            }
        } catch (Exception e) {
            log.error("IOException trying to write on the response", e);
        }
    }

    protected static String translate(FacesContext context, String label) {
        String bundleName = context.getApplication().getMessageBundle();
        Locale locale = context.getViewRoot().getLocale();
        label = I18NUtils.getMessageString(bundleName, label, null, locale);
        return label;
    }

}
