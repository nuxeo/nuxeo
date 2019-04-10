/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Mariana Cedica
 */
package org.nuxeo.ecm.quota.automation;

import java.io.StringWriter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.codehaus.jackson.map.ObjectMapper;
import org.nuxeo.common.utils.i18n.I18NUtils;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.quota.size.QuotaAware;
import org.nuxeo.ecm.quota.size.QuotaDisplayValue;
import org.nuxeo.ecm.quota.size.QuotaInfo;

/**
 * Returns a json representation of the quota info to be displayed in a pie chart
 *
 * @since 5.7
 */
@Operation(id = GetQuotaStatisticsOperation.ID, category = "Quotas", label = "Get Quota statistics", description = "Returns the Quota Infos (innerSize, totalSize and maxQuota) for a DocumentModel")
public class GetQuotaStatisticsOperation {

    public static final String ID = "Quotas.GetStatistics";

    @Context
    protected CoreSession session;

    @Param(name = "documentRef", required = true)
    protected DocumentRef documentRef;

    @Param(name = "language", required = false)
    protected String language;

    @OperationMethod()
    public Blob run() throws Exception {
        Locale locale = language != null && !language.isEmpty() ? new Locale(language) : Locale.ENGLISH;
        DocumentModel doc = session.getDocument(documentRef);
        QuotaAware qa = doc.getAdapter(QuotaAware.class);
        if (qa == null) {
            throw new ClientException("Quota not activated on doc");
        }
        String string = toJSON(qa.getQuotaInfo(), locale);
        return Blobs.createBlob(string, "application/json");
    }

    public String toJSON(QuotaInfo quotaInfo, Locale locale) {
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(2);
        List<QuotaStat> stats = new ArrayList<QuotaStat>();
        long liveSize = quotaInfo.getTotalSize().getValue()
                - (quotaInfo.getTrashSize().getValue() + quotaInfo.getSizeVersions().getValue());
        QuotaDisplayValue live = new QuotaDisplayValue(liveSize);
        stats.add(new QuotaStat(quotaInfo.getLiveSize().getValue(), getI18nLabel("label.quota.liveSize", locale) + ":"
                + nf.format(live.getValueInUnit()) + " " + getI18nLabel(live.getUnit(), locale)));
        stats.add(new QuotaStat(quotaInfo.getTrashSize().getValue(), getI18nLabel("label.quota.trashSize", locale)
                + ":" + nf.format(quotaInfo.getTrashSize().getValueInUnit()) + " "
                + getI18nLabel(quotaInfo.getTrashSize().getUnit(), locale)));
        stats.add(new QuotaStat(quotaInfo.getSizeVersions().getValue(),
                getI18nLabel("label.quota.versionsSize", locale) + ":"
                        + nf.format(quotaInfo.getSizeVersions().getValueInUnit()) + " "
                        + getI18nLabel(quotaInfo.getSizeVersions().getUnit(), locale)));
        ObjectMapper mapper = new ObjectMapper();
        StringWriter writer = new StringWriter();
        try {
            mapper.writeValue(writer, stats);
        } catch (Exception e) {
            throw new ClientRuntimeException(e);
        }
        return writer.toString();
    }

    protected String getI18nLabel(String label, Locale locale) {
        if (label == null) {
            label = "";
        }
        return I18NUtils.getMessageString("messages", label, null, locale);
    }

    class QuotaStat {
        private String label;

        private long data;

        QuotaStat(long data, String label) {
            this.data = data;
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public long getData() {
            return data;
        }
    }
}