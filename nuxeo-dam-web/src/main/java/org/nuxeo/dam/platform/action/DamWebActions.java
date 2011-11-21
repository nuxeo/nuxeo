/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo
 */

package org.nuxeo.dam.platform.action;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.ScopeType.EVENT;
import static org.jboss.seam.annotations.Install.FRAMEWORK;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;

/**
 * @author <a href="mailto:cbaican@nuxeo.com">Catalin Baican</a>
 */
@Name("damWebActions")
@Scope(CONVERSATION)
@Install(precedence = FRAMEWORK)
public class DamWebActions implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(DamWebActions.class);

    public static final String DAM_VIEW_ASSET_ACTION_LIST_CATEGORY = "DAM_VIEW_ASSET_ACTION_LIST";

    @In(create = true)
    protected transient WebActions webActions;

    protected boolean showList = false;

    protected boolean showThumbnail = true;

    protected boolean showBigThumbnail = false;

    @Factory(value = "assetActionsList", scope = EVENT)
    public List<Action> getAssetActionsList() {
        return webActions.getActionsList(DAM_VIEW_ASSET_ACTION_LIST_CATEGORY);
    }

    public Action getCurrentTabAction() {
        return webActions.getCurrentTabAction(DAM_VIEW_ASSET_ACTION_LIST_CATEGORY);
    }

    public void showListLink() {
        if (showList) {
            return;
        }
        showList = true;
        showThumbnail = false;
        showBigThumbnail = false;
    }

    public void showThumbnailLink() {
        if (showThumbnail) {
            return;
        }
        showThumbnail = true;
        showList = false;
        showBigThumbnail = false;
    }

    public void showBigThumbnailLink() {
        if (showBigThumbnail) {
            return;
        }
        showBigThumbnail = true;
        showList = false;
        showThumbnail = false;
    }

    public boolean getShowList() {
        return showList;
    }

    public void setShowList(boolean showList) {
        this.showList = showList;
    }

    public boolean getShowThumbnail() {
        return showThumbnail;
    }

    public void setShowThumbnail(boolean showThumbnail) {
        this.showThumbnail = showThumbnail;
    }

    public boolean isShowBigThumbnail() {
        return showBigThumbnail;
    }

    public void setShowBigThumbnail(boolean showBigThumbnail) {
        this.showBigThumbnail = showBigThumbnail;
    }
}
