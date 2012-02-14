/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thomas Roger
 */

package org.nuxeo.dam.webapp.chainselect;

import javax.faces.context.FacesContext;

import org.nuxeo.ecm.platform.ui.web.directory.ChainSelect;

/**
 * Temporary helper class to cleanup any ChainSelect given its id until we have
 * a correct ChainSelect behavior like all our other components.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class ChainSelectCleaner {

    public static final String ASSET_COVERAGE_CHAIN_SELECT_ID = "navigation:assetView:nxl_dublincore:nxw_coverage_2:nxw_coverage_2_editselect";

    public static final String ASSET_SUBJECTS_CHAIN_SELECT_ID = "navigation:assetView:nxl_dublincore:nxw_subjects_2:nxw_subjects_2_editselect";

    public static final String IMPORT_COVERAGE_CHAIN_SELECT_ID = "importset_form:nxl_importset:nxl_importset_left:nxw_coverage:nxw_coverage_editselect";

    public static final String IMPORT_SUBJECTS_CHAIN_SELECT_ID = "importset_form:nxl_importset:nxl_importset_right:nxw_subjects:nxw_subjects_editselect";

    public static final String BULK_EDIT_COVERAGE_CHAIN_SELECT_ID = "bulk_edit_form:nxl_bulk_edit:nxw_coverage_1:nxw_coverage_1_editselect";

    public static final String BULK_EDIT_SUBJECTS_CHAIN_SELECT_ID = "bulk_edit_form:nxl_bulk_edit:nxw_subjects_1:nxw_subjects_1_editselect";

    private ChainSelectCleaner() {
        // Helper class
    }

    public static void cleanup(String chainSelectId) {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null) {
            ChainSelect chainSelect = (ChainSelect) facesContext.getViewRoot().findComponent(
                    chainSelectId);
            if (chainSelect != null) {
                chainSelect.setComponentValue(null);
            }
        }
    }

}
