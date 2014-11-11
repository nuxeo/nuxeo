/*
 * (C) Copyright 2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * Contributors:
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.automation.jbpm;

import java.util.Locale;
import java.util.Map;

import org.nuxeo.common.utils.i18n.I18NUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.platform.types.adapter.TypeInfo;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.api.DocumentViewCodecManager;

import com.google.common.collect.Maps;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class AbstractWorkflowOperation {

    protected String getI18nLabel(String label, Locale locale) {
        if (label == null) {
            label = "";
        }
        return I18NUtils.getMessageString("messages", label, null, locale);
    }

    protected String getDocumentLink(
            DocumentViewCodecManager documentViewCodecManager,
            DocumentModel doc, boolean includeWorkflowTab)
            throws ClientException {
        String viewId = getDefaultViewFor(doc);
        Map<String, String> parameters = Maps.newHashMap();
        if (includeWorkflowTab) {
            parameters.put("tabId", "TAB_CONTENT_JBPM");
        }
        DocumentView docView = new DocumentViewImpl(new DocumentLocationImpl(
                doc), viewId, parameters);
        return documentViewCodecManager.getUrlFromDocumentView("docpath",
                docView, false, null);
    }

    protected String getDefaultViewFor(DocumentModel doc) {
        TypeInfo type = doc.getAdapter(TypeInfo.class);
        if (type == null) {
            return null;
        }
        return type.getDefaultView();
    }

}
