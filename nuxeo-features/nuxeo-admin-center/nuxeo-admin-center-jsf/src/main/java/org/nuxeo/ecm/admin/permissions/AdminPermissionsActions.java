/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.admin.permissions;

import java.io.Serializable;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.contentview.jsf.ContentView;
import org.nuxeo.ecm.platform.contentview.seam.ContentViewActions;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 7.4
 */
@Name("adminPermissionsActions")
@Scope(ScopeType.CONVERSATION)
@Install(precedence = Install.FRAMEWORK)
public class AdminPermissionsActions implements Serializable {

    private static final long serialVersionUID = 1L;

    public final static String PERMISSIONS_PURGE_CONTENT_VIEW = "PERMISSIONS_PURGE";

    public static final String CATEGORY_PURGE_WORK = "permissionsPurge";

    public static final String ACE_STATUS_ALL = "all";

    @In(create = true)
    protected ContentViewActions contentViewActions;

    protected String purgeWorkId;

    protected String selectedACEStatus = ACE_STATUS_ALL;

    public String getSelectedACEStatus() {
        return selectedACEStatus;
    }

    public void setSelectedACEStatus(String selectedACEStatus) {
        this.selectedACEStatus = selectedACEStatus;
    }

    public String getACEStatusFixedPart() {
        switch (selectedACEStatus) {
        case ACE_STATUS_ALL:
            return null;
        case "0":
            return "AND ecm:acl/*1/status = 0";
        case "1":
            return "AND (ecm:acl/*1/status IS NULL OR ecm:acl/*1/status = 1)";
        case "2":
            return "AND ecm:acl/*1/status = 2";
        default:
            return null;
        }
    }

    public void doPurge() {
        ContentView contentView = contentViewActions.getContentView(PERMISSIONS_PURGE_CONTENT_VIEW);
        DocumentModel searchDocumentModel = contentView.getSearchDocumentModel();
        PermissionsPurgeWork work = new PermissionsPurgeWork(searchDocumentModel);
        purgeWorkId = work.launch();
    }

    public void cancelPurge() {
        ContentView contentView = contentViewActions.getContentView(PERMISSIONS_PURGE_CONTENT_VIEW);
        contentView.resetSearchDocumentModel();
        purgeWorkId = null;
    }

    public boolean canStartPurge() {
        WorkManager workManager = Framework.getService(WorkManager.class);
        return workManager.getMetrics(PermissionsPurgeWork.CATEGORY).getRunning().intValue() <= 0;
    }

    public PurgeWorkStatus getPurgeStatus() {
        if (purgeWorkId == null) {
            return null;
        }

        return PermissionsPurgeWork.getStatus(purgeWorkId);
    }

}
