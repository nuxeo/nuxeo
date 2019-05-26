/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Quentin Lamerand <qlamerand@nuxeo.com>
 */

package org.nuxeo.ecm.platform.forms.layout.actions;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.annotations.Install.FRAMEWORK;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionException;
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
        BlobHolder bh = new SimpleBlobHolder(Blobs.createBlob(text, mimeType, "UTF-8"));
        Map<String, Serializable> parameters = new HashMap<>();
        parameters.put("bodyContentOnly", Boolean.TRUE);
        try {
            bh = Framework.getService(ConversionService.class).convertToMimeType("text/html", bh, parameters);
            text = bh.getBlob().getString();
        } catch (ConversionException | IOException e) {
            log.error("Failed to convert to HTML.", e);
        }
        return text;
    }

}
