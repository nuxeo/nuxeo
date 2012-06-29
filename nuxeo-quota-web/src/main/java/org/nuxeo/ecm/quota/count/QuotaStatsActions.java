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

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

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
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
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

    private static final long serialVersionUID = -1L;

    @In(create = true)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected transient NavigationContext navigationContext;

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

        try {
            Long quotaValue = Long.parseLong(strValue);
        } catch (NumberFormatException e) {
            FacesMessage message = new FacesMessage(
                    FacesMessage.SEVERITY_ERROR, ComponentUtils.translate(
                            context, "wrong format"), null);
            // also add global message
            context.addMessage(null, message);
            throw new ValidatorException(message);
        }

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
}
