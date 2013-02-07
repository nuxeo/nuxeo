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
import java.util.HashMap;
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
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.quota.QuotaStatsService;
import org.nuxeo.ecm.quota.QuotaStatsUpdater;
import org.nuxeo.ecm.quota.size.QuotaAware;
import org.nuxeo.ecm.quota.size.QuotaDisplayValue;
import org.nuxeo.launcher.config.ConfigurationException;
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

    public static final String QUOTA_STATS_USERWORKSPACES = "quota.active.userworkspaces";

    public static final String QUOTA_MAX_SIZE_USERWORKSPACES = "quota.maxSize.userworkspaces";

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

    @Create
    public void initialize() {
        initQuotaActivatedOnUserWorkspaces();
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
        return quotaStatsService.getProgressStatus(updaterName);
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

    public QuotaSlidervalueInfo getMinQuotaSliderValue() throws Exception {
        return new QuotaSlidervalueInfo(Math.log(100 * 1024), 100, "KB");
    }

    public QuotaSlidervalueInfo getMaxQuotaSliderValue() throws Exception {
        long maxQuotaSize = getQuotaStatsService().getQuotaFromParent(
                navigationContext.getCurrentDocument(), documentManager);
        if (maxQuotaSize > 0) {
            return new QuotaSlidervalueInfo(new QuotaDisplayValue(maxQuotaSize));
        }
        return new QuotaSlidervalueInfo(27.70, 999, "GB");
    }

    /**
     * @throws ClientException
     * @since 5.7
     */
    public void saveQuotaActivatedOnUsersWorkspaces() throws ClientException {
        Map<String, String> customParameters = new HashMap<String, String>();
        customParameters.put(QUOTA_STATS_USERWORKSPACES,
                Boolean.toString(isActivateQuotaOnUsersWorkspaces()));
        customParameters.put(QUOTA_MAX_SIZE_USERWORKSPACES,
                Long.toString(getMaxQuotaOnUsersWorkspaces()));
        try {
            getConfigurationGenerator().saveFilteredConfiguration(
                    customParameters);
        } catch (ConfigurationException e) {
            log.error(e, e);
        }
        if (isActivateQuotaOnUsersWorkspaces()) {
            getQuotaStatsService().launchSetMaxQuotaOnUserWorkspaces(
                    getMaxQuotaOnUsersWorkspaces(),
                    documentManager.getRootDocument(), documentManager);
        }
    }

    /**
     * @since 5.7
     */
    public void initQuotaActivatedOnUserWorkspaces() {
        if (getConfigurationGenerator().init()) {
            setActivateQuotaOnUsersWorkspaces((Boolean.parseBoolean((String) setupConfigGenerator.getUserConfig().get(
                    QUOTA_STATS_USERWORKSPACES))));
            String mx = (String) setupConfigGenerator.getUserConfig().get(
                    QUOTA_MAX_SIZE_USERWORKSPACES);
            if (mx == null || mx.equals("")) {
                mx = "-1";
            }
            setMaxQuotaOnUsersWorkspaces(Long.parseLong(mx));
        }
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

    ConfigurationGenerator getConfigurationGenerator() {
        if (setupConfigGenerator == null) {
            setupConfigGenerator = new ConfigurationGenerator();
        }
        return setupConfigGenerator;
    }

    class QuotaSlidervalueInfo {

        double size;

        float valueInUnit;

        String unit;

        QuotaSlidervalueInfo(QuotaDisplayValue maxSizeDisplayValue) {
            this.size = Math.log(maxSizeDisplayValue.getValue());
            valueInUnit = maxSizeDisplayValue.getValueInUnit();
            unit = maxSizeDisplayValue.getUnit();
        }

        QuotaSlidervalueInfo(double maxSize, float valueInUnit, String unit) {
            this.size = maxSize;
            this.valueInUnit = valueInUnit;
            this.unit = unit;
        }

        public double getSize() {
            return size;
        }

        public float getValueInUnit() {
            return valueInUnit;
        }

        public String getUnit() {
            return unit;
        }
    }
}
