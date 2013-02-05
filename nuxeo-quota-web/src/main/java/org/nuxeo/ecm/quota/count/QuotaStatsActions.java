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

import static org.jboss.seam.ScopeType.STATELESS;
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
import org.nuxeo.ecm.quota.size.QuotaAwareDocument;
import org.nuxeo.ecm.quota.size.QuotaAwareDocumentFactory;
import org.nuxeo.ecm.quota.size.QuotaDisplayValue;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
@Name("quotaStatsActions")
@Scope(STATELESS)
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

    protected QuotaStatsService quotaStatsService;

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

    public void activateQuota() throws ClientException {
        if (getQuotaDoc() == null) {
            DocumentModel doc = navigationContext.getCurrentDocument();
            QuotaAwareDocument qa = QuotaAwareDocumentFactory.make(doc, true);
            navigationContext.resetCurrentContext();
            navigationContext.navigateToDocument(qa.getDoc());
        }
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
        return new QuotaSlidervalueInfo(Math.log(10 * 1024), 10, "KB");
    }

    public QuotaSlidervalueInfo getMaxQuotaSliderValue() throws Exception {
        long maxQuotaSize = getQuotaStatsService().getQuotaFromParent(
                navigationContext.getCurrentDocument(), documentManager);
        if (maxQuotaSize > 0) {
            return new QuotaSlidervalueInfo(new QuotaDisplayValue(maxQuotaSize));
        }
        return new QuotaSlidervalueInfo(27.70, 999, "GB");
    }

    QuotaStatsService getQuotaStatsService() {
        if (quotaStatsService == null) {
            quotaStatsService = Framework.getLocalService(QuotaStatsService.class);
        }
        return quotaStatsService;
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
