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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.admin;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.faces.model.SelectItem;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Contexts;
import org.nuxeo.ecm.admin.repo.RepoStat;
import org.nuxeo.ecm.admin.repo.RepoStatInfo;
import org.nuxeo.ecm.admin.runtime.RuntimeInstrospection;
import org.nuxeo.ecm.admin.runtime.SimplifiedServerInfo;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.platform.audit.api.AuditReader;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.runtime.api.Framework;

/**
 * Seam Bean to export System info.
 *
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 */
@Name("systemInfo")
@Scope(CONVERSATION)
public class SystemInfoManager implements Serializable {

    private static final long serialVersionUID = 1L;

    protected String selectedTimeRange;

    protected int currentAuditPage = 1;

    protected static int pageSize = 25;

    protected List<Repository> repositories;

    protected String currentRepositoryName;

    protected RepoStat runningStat;

    protected RepoStatInfo statResult;

    // *********************************
    // Host info Management

    public String getHostInfo() {

        StringBuilder sb = new StringBuilder();

        sb.append("\nOS : ");
        sb.append(System.getProperty("os.name"));
        sb.append(" (");
        sb.append(System.getProperty("os.arch"));
        sb.append(")");

        sb.append("\n");

        sb.append("\nCPU(s) : ");
        sb.append(Runtime.getRuntime().availableProcessors());

        sb.append("\n");

        sb.append("\nJVM : ");
        sb.append(System.getProperty("java.runtime.name"));
        sb.append(" ");
        sb.append(System.getProperty("java.vm.version"));
        sb.append(" (");
        sb.append(System.getProperty("java.vendor"));
        sb.append(")");

        sb.append("\n");

        sb.append("\nPlatform language : ");
        sb.append(System.getProperty("user.language"));
        sb.append("  ");
        sb.append(System.getenv("LANG"));

        sb.append("\n");

        sb.append("\nJava Memory:");
        sb.append("\n  Heap size  : ");
        sb.append(Runtime.getRuntime().totalMemory() / (1024 * 1024));
        sb.append(" MB");
        sb.append("\n  Used       : ");
        sb.append((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())
                / (1024 * 1024));
        sb.append(" MB");
        sb.append("\n  Free       : ");
        sb.append(Runtime.getRuntime().freeMemory() / (1024 * 1024));
        sb.append(" MB");
        sb.append("\n  Max size   : ");
        sb.append(Runtime.getRuntime().maxMemory() / (1024 * 1024));
        sb.append(" MB");

        return sb.toString();
    }

    @Factory(value = "nuxeoServerInfo", scope = ScopeType.EVENT)
    public SimplifiedServerInfo getNuxeoServerInfo() {
        return RuntimeInstrospection.getInfo();
    }

    // *********************************
    // Repo settings Management

    public boolean isMultiRepo() throws Exception {
        return listAvailableRepositories().size() > 1;
    }

    public List<Repository> listAvailableRepositories() throws Exception {
        if (repositories == null) {
            repositories = new ArrayList<Repository>();
            RepositoryManager rm = Framework.getService(RepositoryManager.class);
            Collection<Repository> repos = rm.getRepositories();
            repositories.addAll(repos);
            currentRepositoryName = rm.getDefaultRepository().getName();
        }
        return repositories;
    }

    public String getCurrentRepositoryName() throws Exception {
        if (currentRepositoryName == null) {
            listAvailableRepositories();
        }
        return currentRepositoryName;
    }

    public void setCurrentRepositoryName(String name) throws Exception {
        currentRepositoryName = name;
    }

    public int getOpenSessionNumber() {
        return CoreInstance.getInstance().getSessions().length;
    }

    // *********************************
    // Repo stats Management

    public void startRepoStats() throws Exception {
        if (runningStat != null) {
            return;
        }

        statResult = null;
        runningStat = new RepoStat(getCurrentRepositoryName(), 5, true);
        runningStat.run(new PathRef("/"));
    }

    public void checkReady() {
        isStatInfoAvailable();
    }

    public boolean isStatInfoInProgress() {
        if (isStatInfoAvailable()) {
            return false;
        }
        if (runningStat == null) {
            return false;
        }
        return true;
    }

    public boolean isStatInfoAvailable() {
        if (statResult != null) {
            return true;
        }
        if (runningStat != null) {
            if (!runningStat.isRunning()) {
                statResult = runningStat.getInfo();
                Contexts.getEventContext().remove("repoStatResult");
                runningStat = null;
                return true;
            }
        }
        return false;
    }

    @Factory(value = "repoStatResult", scope = ScopeType.EVENT)
    public RepoStatInfo getStatInfo() {
        return statResult;
    }

    public String getRepoUsage() throws Exception {
        StringBuilder sb = new StringBuilder();

        int nbSessions = CoreInstance.getInstance().getSessions().length;

        sb.append("Number of open repository session : ");
        sb.append(nbSessions);

        RepoStat stat = new RepoStat("default", 5, true);
        stat.run(new PathRef("/"));
        Thread.sleep(100);

        do {
            Thread.sleep(1000);
        } while (stat.isRunning());

        sb.append(stat.getInfo().toString());

        return sb.toString();
    }

    // *********************************
    // Audit Management

    public List<SelectItem> getTimeRanges() {
        List<SelectItem> ranges = new ArrayList<SelectItem>();

        for (int i = 1; i < 13; i++) {
            ranges.add(new SelectItem(i + "h", "label.timerange." + i + "h"));
        }
        for (int i = 1; i < 8; i++) {
            ranges.add(new SelectItem((i * 24) + "h", "label.timerange." + i
                    + "d"));
        }
        for (int i = 2; i < 6; i++) {
            ranges.add(new SelectItem((24 * 7) * i + "h", "label.timerange."
                    + i + "w"));
        }
        return ranges;
    }

    public String getSelectedTimeRange() {
        if (selectedTimeRange == null) {
            selectedTimeRange = "1h";
        }
        return selectedTimeRange;
    }

    public void setSelectedTimeRange(String dateRange) {
        selectedTimeRange = dateRange;
        currentAuditPage = 1;
        Contexts.getEventContext().remove("userLoginEvents");
    }

    public int getCurrentAuditPage() {
        return currentAuditPage;
    }

    public void nextPage() {
        currentAuditPage += 1;
        Contexts.getEventContext().remove("userLoginEvents");
    }

    public void prevPage() {
        currentAuditPage -= 1;
        if (currentAuditPage <= 0) {
            currentAuditPage = 1;
        }
        Contexts.getEventContext().remove("userLoginEvents");
    }

    @Factory(value = "userLoginEvents", scope = ScopeType.EVENT)
    public List<LogEntry> getLoginInfo() throws Exception {
        String[] events = { "loginSuccess", "loginFailed", "logout" };

        AuditReader reader = Framework.getService(AuditReader.class);

        return reader.queryLogsByPage(events, selectedTimeRange,
                "NuxeoAuthentication", null, currentAuditPage, pageSize);
    }

}
