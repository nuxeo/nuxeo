/*
 * (C) Copyright 2010-2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     tdelprat
 */
package org.nuxeo.ecm.admin;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Contexts;
import org.nuxeo.ecm.platform.audit.api.AuditReader;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.web.common.session.NuxeoHttpSessionMonitor;
import org.nuxeo.ecm.platform.web.common.session.SessionInfo;
import org.nuxeo.runtime.api.Framework;

/**
 * Seam Bean to export some stats about user's activity
 *
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 */
@Name("usersActivityInfo")
@Scope(CONVERSATION)
public class UsersActivityManager implements Serializable {

    private static final long serialVersionUID = 1L;

    protected static final Log log = LogFactory.getLog(UsersActivityManager.class);

    protected String selectedAuditTimeRange;

    protected String selectedAuditCategory;

    protected String selectedHttpSessionsTimeRange;

    protected int currentAuditPage = 1;

    protected static int pageSize = 25;

    // *********************************
    // Audit Management

    public List<SelectItem> getAuditTimeRanges() {
        List<SelectItem> ranges = new ArrayList<SelectItem>();

        for (int i = 1; i < 13; i++) {
            ranges.add(new SelectItem(i + "h", "label.timerange." + i + "h"));
        }
        for (int i = 1; i < 8; i++) {
            ranges.add(new SelectItem(i * 24 + "h", "label.timerange." + i
                    + "d"));
        }
        for (int i = 2; i < 6; i++) {
            ranges.add(new SelectItem(24 * 7 * i + "h", "label.timerange." + i
                    + "w"));
        }
        return ranges;
    }

    public List<SelectItem> getAuditCategories() {
        List<SelectItem> ranges = new ArrayList<SelectItem>();

        ranges.add(new SelectItem("NuxeoAuthentication", "label.audit.auth"));
        ranges.add(new SelectItem("eventDocumentCategory", "label.audit.doc"));
        ranges.add(new SelectItem("eventLifeCycleCategory", "label.audit.lifecycle"));
        ranges.add(new SelectItem("all", "label.audit.all"));
        return ranges;
    }

    public String getSelectedAuditTimeRange() {
        if (selectedAuditTimeRange == null) {
            selectedAuditTimeRange = "1h";
        }
        return selectedAuditTimeRange;
    }

    public void setSelectedAuditTimeRange(String dateRange) {
        selectedAuditTimeRange = dateRange;
        currentAuditPage = 1;
        Contexts.getEventContext().remove("userLoginEvents");
    }

    public String getSelectedAuditCategory() {
        if (selectedAuditCategory == null) {
            selectedAuditCategory = "all";
        }
        return selectedAuditCategory;
    }

    public void setSelectedAuditCategory(String category) {
        selectedAuditCategory = category;
        currentAuditPage = 1;
        Contexts.getEventContext().remove("userLoginEvents");
    }

    public int getCurrentAuditPage() {
        return currentAuditPage;
    }

    public void nextAuditPage() {
        currentAuditPage += 1;
        Contexts.getEventContext().remove("userLoginEvents");
    }

    public void prevAuditPage() {
        currentAuditPage -= 1;
        if (currentAuditPage <= 0) {
            currentAuditPage = 1;
        }
        Contexts.getEventContext().remove("userLoginEvents");
    }

    @Factory(value = "userLoginEvents", scope = ScopeType.EVENT)
    public List<LogEntry> getLoginInfo() throws Exception {

        AuditReader reader = Framework.getService(AuditReader.class);

        String[] cat = {getSelectedAuditCategory()};
        if (getSelectedAuditCategory().equals("all")) {
            cat = new String[0];
        }
        return reader.queryLogsByPage(new String[0], selectedAuditTimeRange,
                cat, null, currentAuditPage, pageSize);
    }

    // **********************
    // User's Http Sessions

    public List<SelectItem> getHttpSessionsTimeRanges() {
        List<SelectItem> ranges = new ArrayList<SelectItem>();

        ranges.add(new SelectItem(5*60 + "s", "label.timerange." + 5 + "m"));
        ranges.add(new SelectItem(10*60 + "s", "label.timerange." + 10 + "m"));
        ranges.add(new SelectItem(20*60 + "s", "label.timerange." + 20 + "m"));
        ranges.add(new SelectItem(30*60 + "s", "label.timerange." + 30 + "m"));
        ranges.add(new SelectItem(60*60 + "s", "label.timerange." + 1 + "h"));
        ranges.add(new SelectItem(2*60*60 + "s", "label.timerange." + 2 + "h"));
        ranges.add(new SelectItem(4*60*60 + "s", "label.timerange." + 4 + "h"));
        ranges.add(new SelectItem("all", "label.timerange.all"));

        return ranges;
    }

    public String getSelectedHttpSessionsTimeRange() {
        if (selectedHttpSessionsTimeRange == null) {
            selectedHttpSessionsTimeRange = "1800s";
        }
        return selectedHttpSessionsTimeRange;
    }

    public void setSelectedHttpSessionsTimeRange(String dateRange) {
        selectedHttpSessionsTimeRange = dateRange;
        Contexts.getEventContext().remove("userHttpSessions");
    }

    @Factory(value = "nbActiveUserHttpSessions", scope = ScopeType.EVENT)
    public int getUserSessionsCount() {
        return NuxeoHttpSessionMonitor.instance().getSortedSessions().size();
    }

    @Factory(value = "nbUserRequests", scope = ScopeType.EVENT)
    public long getUserRequestCount() {
        return NuxeoHttpSessionMonitor.instance().getGlobalRequestCounter();
    }

    @Factory(value = "userHttpSessions", scope = ScopeType.EVENT)
    public List<SessionInfo> getUserSessions() {
        if (getSelectedHttpSessionsTimeRange().equals("all")) {
            return NuxeoHttpSessionMonitor.instance().getSortedSessions();
        } else {
            long maxInactivity = Long.parseLong(selectedHttpSessionsTimeRange.replace("s", ""));
            return NuxeoHttpSessionMonitor.instance().getSortedSessions(maxInactivity);
        }
    }

}
