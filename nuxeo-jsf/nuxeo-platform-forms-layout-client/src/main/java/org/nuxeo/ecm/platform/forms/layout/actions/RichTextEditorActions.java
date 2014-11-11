/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Quentin Lamerand <qlamerand@nuxeo.com>
 */

package org.nuxeo.ecm.platform.forms.layout.actions;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.annotations.Install.FRAMEWORK;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.runtime.api.Framework;

/**
 * Seam component used for HTML conversion in rich text with MIME type widget
 *
 * @author <a href="mailto:qlamerand@nuxeo.com">Quentin Lamerand</a>
 * @author <a href="mailto:bdelbosc@nuxeo.com">Benoit Delbosc</a>
 * @since 5.5
 */
@Name("richTextEditorActions")
@Scope(CONVERSATION)
@Install(precedence = FRAMEWORK)
// TODO: move elsewhere, this is not related to layouts and widgets management
// and could be moved to a JSF function instead of adding a dependency to Seam
public class RichTextEditorActions implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(RichTextEditorActions.class);

    public String convertToHtml(String text, String mimeType) {
        BlobHolder bh = new SimpleBlobHolder(new StringBlob(text, mimeType,
                "UTF-8"));
        Map<String, Serializable> parameters = new HashMap<String, Serializable>();
        parameters.put("bodyContentOnly", Boolean.TRUE);
        try {
            bh = Framework.getLocalService(ConversionService.class).convertToMimeType(
                    "text/html", bh, parameters);
            text = bh.getBlob().getString();
        } catch (Exception e) {
            log.error("Failed to convert to HTML.", e);
        }
        return text;
    }

}
