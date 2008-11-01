/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Florent Bonnet
 *     Florent Guillaume
 */

package org.nuxeo.ecm.webapp.navigation;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.search.api.client.querymodel.QueryModelService;
import org.nuxeo.ecm.core.search.api.client.querymodel.descriptor.QueryModelDescriptor;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;

/**
 * @author Florent Bonnet
 * @author Florent Guillaume
 */
public class TreeManagerService extends DefaultComponent {

    public static final ComponentName NAME =
        new ComponentName("org.nuxeo.ecm.ui.util.treemanagermanagment.TreeManagerService");

    private static final Log log = LogFactory.getLog(TreeManagerService.class);

    private static TreeManagerPluginExtension treePlugin;

    private static DocumentFilter treeFilter;

    /**
     * Leaf filter. If not null and it accepts, then the doc is assumed to be a
     * leaf and have no children.
     */
    private static DocumentFilter leafFilter;

    private static QueryModelDescriptor queryModelDescriptor;

    public TreeManagerService() {
        setupTreeFilter(null);
    }

    private static void setupTreeFilter(Extension extension) {
        if (treePlugin == null) {
            // init default filter
            // show section // don't show files
            treeFilter = new DocumentFilterImpl(true, false);
            leafFilter = null;
            return;
        }

        // Built-in filter
        String filterClass = treePlugin.getFilterClassName();
        if (filterClass != null && !filterClass.equals("")) {
            // custom filter
            try {
                treeFilter = (DocumentFilter) extension.getContext().loadClass(
                        filterClass).newInstance();
            } catch (Throwable e) {
                log.error(e);
                treeFilter = new DocumentFilterImpl(
                        treePlugin.getShowSection(), treePlugin.getShowFiles());
            }
        } else {
            // standard filter: sections, files, excluded types
            List<String> excludedTypes = treePlugin.getExcludedTypes();
            if (excludedTypes == null || excludedTypes.isEmpty()) {
                treeFilter = new DocumentFilterImpl(
                        treePlugin.getShowSection(), treePlugin.getShowFiles());
            } else {
                treeFilter = new TypeBasedDocumentFilter(
                        treePlugin.getShowSection(), treePlugin.getShowFiles(),
                        excludedTypes);
            }
        }

        String leafFilterClass = treePlugin.getLeafFilterClassName();
        if (leafFilterClass != null && !leafFilterClass.equals("")) {
            try {
                leafFilter = (DocumentFilter) extension.getContext().loadClass(
                        leafFilterClass).newInstance();
            } catch (Throwable e) {
                log.error(e);
            }
        }

        String queryModelName = treePlugin.getQueryModelName();
        if (queryModelName == null) {
            queryModelDescriptor = null;
        } else {
            QueryModelService service = (QueryModelService) Framework.getRuntime().getComponent(
                    QueryModelService.NAME);
            queryModelDescriptor = service.getQueryModelDescriptor(queryModelName);
        }
    }

    @Override
    public void registerExtension(Extension extension) throws Exception {
        Object[] contribs = extension.getContributions();
        for (Object contrib : contribs) {
            log.info("registering TreeManager Plugin ... ");
            TreeManagerPluginExtension treeManagerPlugin = (TreeManagerPluginExtension) contrib;
            register(extension, treeManagerPlugin);
        }
    }

    private void register(Extension extension, TreeManagerPluginExtension pluginExtension) {
        treePlugin = pluginExtension;
        setupTreeFilter(extension);
    }

    @Override
    public void unregisterExtension(Extension extension) throws Exception {
        treePlugin = null;
    }

    /**
     * @deprecated use getDocumentFilter() instead
     */
    @Deprecated
    public static Boolean showFiles() {
        return treePlugin.getShowFiles();
    }

    /**
     * @deprecated use getDocumentFilter() instead
     */
    @Deprecated
    public static Boolean showSection() {
        return treePlugin.getShowSection();
    }

    public static DocumentFilter getDocumentFilter() {
        if (treeFilter == null) {
            setupTreeFilter(null);
        }
        return treeFilter;
    }

    public static DocumentFilter getLeafFilter() {
        if (treeFilter == null) { // not a typo
            setupTreeFilter(null);
        }
        return leafFilter;
    }

    public static QueryModelDescriptor getQueryModelDescriptor() {
        if (treeFilter == null) { // not a typo
            setupTreeFilter(null);
        }
        return queryModelDescriptor;
    }

}
