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
 * $Id$
 */

package org.nuxeo.ecm.webapp.navigation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;

/**
 * @author Florent BONNET
 *
 */
public class TreeManagerService extends DefaultComponent {

    public static final ComponentName NAME =
        new ComponentName("org.nuxeo.ecm.ui.util.treemanagermanagment.TreeManagerService");

    private static final Log log = LogFactory.getLog(TreeManagerService.class);

    private static TreeManagerPluginExtension treePlugin;
    private static DocumentFilter treeFilter;

    public TreeManagerService() {
        setupTreeFilter(null);
    }

    private static void setupTreeFilter(Extension extension) {
        if (treePlugin == null) {
            // init default filter
            // show section // don't show files
            treeFilter = new DocumentFilterImpl(true, false);
            return;
        }

        // Built-in filter
        String filterClass = treePlugin.getFilterClassName();
        if (filterClass == null || filterClass.equals("")) {
            if (treePlugin.getExcludedTypes() == null
                    || treePlugin.getExcludedTypes().isEmpty()) {
                treeFilter = new DocumentFilterImpl(treePlugin.getShowSection(),
                        treePlugin.getShowFiles());
            } else {
                treeFilter = new TypeBasedDocumentFilter(treePlugin.getShowSection(),
                        treePlugin.getShowFiles(), treePlugin.getExcludedTypes());
            }
            return;
        }

        // custom filter
        try {
            treeFilter = (DocumentFilter) extension.getContext().loadClass(filterClass).newInstance();
        } catch (Throwable e) {
            treeFilter = new DocumentFilterImpl(treePlugin.getShowSection(), treePlugin.getShowFiles());
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
     * Use getDocumentFilter() instead.
     *
     * @return
     */
    @Deprecated
    public static Boolean showFiles() {
        return treePlugin.getShowFiles();
    }

    /**
     * Use getDocumentFilter() instead.
     *
     * @return
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

}
