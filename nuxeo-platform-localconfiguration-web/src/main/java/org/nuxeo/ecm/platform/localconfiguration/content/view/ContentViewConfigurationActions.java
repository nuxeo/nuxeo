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
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.localconfiguration.content.view;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.nuxeo.ecm.core.schema.FacetNames.FOLDERISH;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.platform.contentview.jsf.ContentView;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewService;
import org.nuxeo.ecm.platform.ui.web.directory.SelectItemComparator;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;
import org.nuxeo.runtime.api.Framework;

@Name("contentViewConfigurationActions")
@Scope(CONVERSATION)
@Install(precedence = Install.FRAMEWORK)
public class ContentViewConfigurationActions implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(ContentViewConfigurationActions.class);

    @In(create = true)
    protected ContentViewService contentViewService;

    @In(create = true)
    protected ResourcesAccessor resourcesAccessor;

    public List<SelectItem> getAvailableDocTypes() {
        List<SelectItem> items = new ArrayList<SelectItem>();
        SchemaManager schemaManager;
        try {
            schemaManager = Framework.getService(SchemaManager.class);
        } catch (Exception e) {
            log.error("Exception in retrieving document types : ", e);
            return null;
        }
        for (String typeName : schemaManager.getDocumentTypeNamesForFacet(FOLDERISH)) {
            SelectItem item;
            Map<String, String> messages = resourcesAccessor.getMessages();
            if (messages.containsKey(typeName)) {
                item = new SelectItem(typeName, messages.get(typeName));
            } else {
                item = new SelectItem(typeName);
            }
            items.add(item);
        }
        Collections.sort(items, new SelectItemComparator("label", true));
        return items;
    }

    public List<SelectItem> getAvailableContentViews() throws ClientException {
        List<SelectItem> items = new ArrayList<SelectItem>();
        for (String cvName : contentViewService.getContentViewNames()) { // TODO : use flag ?
            ContentView contentView = contentViewService.getContentView(cvName);
            String title = contentView.getTitle();
            SelectItem item;
            if (title == null) {
                item = new SelectItem(cvName);
            } else {
                if (contentView.getTranslateTitle()) {
                    title = resourcesAccessor.getMessages().get(title);
                }
                item = new SelectItem(cvName, title);
            }
            items.add(item);
        }
        Collections.sort(items, new SelectItemComparator("label", true));
        return items;
    }

}
