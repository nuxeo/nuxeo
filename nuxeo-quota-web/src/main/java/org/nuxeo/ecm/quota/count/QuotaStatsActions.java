/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.quota.count;

import static org.jboss.seam.annotations.Install.FRAMEWORK;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.Work.State;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.quota.QuotaStatsService;
import org.nuxeo.ecm.quota.QuotaStatsUpdater;
import org.nuxeo.ecm.quota.size.QuotaAware;
import org.nuxeo.ecm.quota.size.QuotaDisplayValue;
import org.nuxeo.launcher.config.ConfigurationGenerator;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
@Name("quotaStatsActions")
@Scope(ScopeType.CONVERSATION)
@Install(precedence = FRAMEWORK)
public class QuotaStatsActions implements Serializable {

    protected Log log = LogFactory.getLog(QuotaStatsActions.class);

    private static final long serialVersionUID = -1L;

    @In(create = true)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true)
    protected Map<String, String> messages;

    private transient ConfigurationGenerator setupConfigGenerator;

    protected QuotaStatsService quotaStatsService;

    protected boolean activateQuotaOnUsersWorkspaces;

    protected long maxQuotaOnUsersWorkspaces = -1;

    protected WorkManager workManager;

    @Create
    public void initialize() {
        try {
            initQuotaActivatedOnUserWorkspaces();
        } catch (ClientException e) {
            log.error(e);
        }
    }

    public List<QuotaStatsUpdater> getQuotaStatsUpdaters() {
        QuotaStatsService quotaStatsService = Framework.getLocalService(QuotaStatsService.class);
        return quotaStatsService.getQuotaStatsUpdaters();
    }

    public void launchInitialComputation(String updaterName) {
        launchInitialComputation(updaterName,
                documentManager.getRepositoryName());
    }

    public void launchInitialComputation(String updaterName,
            String repositoryName) {
        QuotaStatsService quotaStatsService = Framework.getLocalService(QuotaStatsService.class);
        quotaStatsService.launchInitialStatisticsComputation(updaterName,
                repositoryName);
    }

    public String getStatus(String updaterName) {
        QuotaStatsService quotaStatsService = Framework.getLocalService(QuotaStatsService.class);
        return quotaStatsService.getProgressStatus(updaterName,
                documentManager.getRepositoryName());
    }

    @Factory(value = "currentQuotaDoc", scope = ScopeType.EVENT)
    public QuotaAware getQuotaDoc() {
        DocumentModel doc = navigationContext.getCurrentDocument();
        return doc.getAdapter(QuotaAware.class);
    }

    public void validateQuotaSize(FacesContext context, UIComponent component,
            Object value) {
        String strValue = value.toString();
        Long quotaValue = -1L;
        boolean quotaAllowed = true;
        try {
            quotaValue = Long.parseLong(strValue);
        } catch (NumberFormatException e) {
            FacesMessage message = new FacesMessage(
                    FacesMessage.SEVERITY_ERROR, messages.get("wrong format"),
                    null);
            // also add global message
            context.addMessage(null, message);
            throw new ValidatorException(message);
        }

        try {
            quotaAllowed = getQuotaStatsService().canSetMaxQuota(quotaValue,
                    navigationContext.getCurrentDocument(), documentManager);
        } catch (ClientException e) {
            log.error(e);
        }
        if (quotaAllowed) {
            return;
        }
        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                messages.get("label.quotaException.QuotaCanNotBeSet"), null);
        // also add global message
        context.addMessage(null, message);
        throw new ValidatorException(message);
    }

    public QuotaDisplayValue formatQuota(long value, long max) {
        QuotaDisplayValue qdv = new QuotaDisplayValue(value, max);
        return qdv;
    }

    public double getMinQuotaSliderValue(long totalSize) throws Exception {
        long minSize = 100 * 1024;
        // 11.528
        if (totalSize > minSize) {
            return Math.log(totalSize + minSize);
        } else {
            return Math.log(minSize);
        }
    }

    public long getMinQuotaSliderValue() throws Exception {
        return 102400;// 100KB
    }

    public long getMaxQuotaSliderValue() throws Exception {
        long maxQuotaSize = -1L;
        DocumentModel doc = navigationContext.getCurrentDocument();
        if (doc != null) {
            maxQuotaSize = getQuotaStatsService().getQuotaFromParent(doc,
                    documentManager);
        }
        return maxQuotaSize > 0 ? maxQuotaSize : 1072668082176L; // 999GB
    }

    /**
     * @throws ClientException
     * @since 5.7
     */
    public void saveQuotaActivatedOnUsersWorkspaces() throws ClientException {
        long maxSize = -1;
        if (isActivateQuotaOnUsersWorkspaces()) {
            maxSize = getMaxQuotaOnUsersWorkspaces();
        }
        getQuotaStatsService().activateQuotaOnUserWorkspaces(maxSize,
                documentManager);
        getQuotaStatsService().launchSetMaxQuotaOnUserWorkspaces(maxSize,
                documentManager.getRootDocument(), documentManager);
    }

    /**
     * @throws ClientException
     * @since 5.7
     */
    public void initQuotaActivatedOnUserWorkspaces() throws ClientException {
        long quota = getQuotaStatsService().getQuotaSetOnUserWorkspaces(
                documentManager);
        setActivateQuotaOnUsersWorkspaces(quota == -1 ? false : true);
        setMaxQuotaOnUsersWorkspaces(quota);
    }

    public boolean workQueuesInProgess() {
        WorkManager workManager = getWorkManager();
        int running = workManager.getQueueSize("quota", State.RUNNING);
        int scheduled = workManager.getQueueSize("quota", State.SCHEDULED);
        return running + scheduled > 0;
    }

    public boolean isQuotaSetOnCurrentDocument() {
        DocumentModel doc = navigationContext.getCurrentDocument();
        // the quota info set on the userworkspaces root should be ignored
        if ("UserWorkspacesRoot".equals(doc.getType())) {
            return true;
        }
        QuotaAware qa = doc.getAdapter(QuotaAware.class);
        if (qa == null) {
            return false;
        }
        long maxSize = qa.getMaxQuota();
        return maxSize > 0;
    }

    public boolean isActivateQuotaOnUsersWorkspaces() {
        return activateQuotaOnUsersWorkspaces;
    }

    public void setActivateQuotaOnUsersWorkspaces(
            boolean activateQuotaOnUsersWorkspaces) {
        this.activateQuotaOnUsersWorkspaces = activateQuotaOnUsersWorkspaces;
    }

    public long getMaxQuotaOnUsersWorkspaces() {
        return maxQuotaOnUsersWorkspaces;
    }

    public void setMaxQuotaOnUsersWorkspaces(long maxQuotaOnUsersWorkspaces) {
        this.maxQuotaOnUsersWorkspaces = maxQuotaOnUsersWorkspaces;
    }

    QuotaStatsService getQuotaStatsService() {
        if (quotaStatsService == null) {
            quotaStatsService = Framework.getLocalService(QuotaStatsService.class);
        }
        return quotaStatsService;
    }

    protected WorkManager getWorkManager() {
        if (workManager == null) {
            workManager = Framework.getLocalService(WorkManager.class);
        }
        return workManager;
    }
}
