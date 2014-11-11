/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat
 *     Florent Guillaume
 */
package org.nuxeo.ecm.admin;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.IOException;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Contexts;
import org.nuxeo.ecm.admin.repo.RepoStat;
import org.nuxeo.ecm.admin.repo.RepoStatInfo;
import org.nuxeo.ecm.admin.runtime.PlatformVersionHelper;
import org.nuxeo.ecm.admin.runtime.RuntimeInstrospection;
import org.nuxeo.ecm.admin.runtime.SimplifiedServerInfo;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.storage.sql.BinaryManagerStatus;
import org.nuxeo.ecm.core.storage.sql.management.RepositoryStatus;
import org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants;
import org.nuxeo.ecm.platform.ui.web.util.BaseURL;
import org.nuxeo.runtime.api.Framework;

/**
 * Seam Bean to export System info.
 */
@Name("systemInfo")
@Scope(CONVERSATION)
public class SystemInfoManager implements Serializable {

    private static final Log log = LogFactory.getLog(SystemInfoManager.class);

    protected static final String RESTART_PATH = "site/connectClient/restartView";

    private static final long serialVersionUID = 1L;

    protected List<Repository> repositories;

    protected String currentRepositoryName;

    protected RepoStat runningStat;

    protected RepoStatInfo statResult;

    protected volatile boolean binaryManagerStatusInvalidation;

    protected volatile BinaryManagerStatus binaryManagerStatus;

    protected boolean binariesGCDelete;

    protected boolean binariesWereDeleted;

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
        sb.append(System.getProperty("java.runtime.version"));
        sb.append(" - build ");
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

    public String getUptime() {
        RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
        long ut = bean.getUptime();
        long uts = ut / 1000;

        StringBuffer sb = new StringBuffer("Nuxeo Server UpTime : ");
        long nbDays = uts / (24 * 3600);
        if (nbDays > 0) {
            sb.append(nbDays + " days, ");
            uts = uts % (24 * 3600);
        }
        long nbHours = uts / 3600;
        sb.append(nbHours + " h ");
        uts = uts % 3600;

        long nbMin = uts / 60;
        sb.append(nbMin + " m ");
        uts = uts % 60;

        sb.append(uts + " s  ");

        return sb.toString();
    }

    @Factory(value = "nuxeoPlatformIdentifier", scope = ScopeType.APPLICATION)
    public String getNuxeoPlatformIdentifier() {
        return PlatformVersionHelper.getPlatformFilter();
    }

    @Factory(value = "nuxeoServerInfo", scope = ScopeType.EVENT)
    public SimplifiedServerInfo getNuxeoServerInfo() {
        return RuntimeInstrospection.getInfo();
    }

    public boolean isBundleDeployed(String bundleId) {
        return RuntimeInstrospection.getBundleIds().contains(bundleId);
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
        return CoreInstance.getInstance().getNumberOfSessions();
    }

    public int getActiveSessionNumber() {
        RepositoryStatus status = new RepositoryStatus();
        return status.getActiveSessionsCount();
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

    public boolean isStatInfoInProgress() {
        if (isStatInfoAvailable()) {
            return false;
        }
        return runningStat != null;
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

        int nbSessions = CoreInstance.getInstance().getNumberOfSessions();

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
    // Server restart

    public String restartServer() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        request.setAttribute(NXAuthConstants.DISABLE_REDIRECT_REQUEST_KEY,
                Boolean.TRUE);
        String restartUrl = BaseURL.getBaseURL(request) + RESTART_PATH;
        try {
            context.getExternalContext().redirect(restartUrl);
        } catch (IOException e) {
            log.error("Error while redirecting to restart page", e);
        }
        return null;
    }

    // *********************************
    // Binaries GC

    public void setBinariesGCDelete(boolean binariesGCDelete) {
        this.binariesGCDelete = binariesGCDelete;
    }

    public boolean getBinariesGCDelete() {
        return binariesGCDelete;
    }

    public boolean getBinariesWereDeleted() {
        return binariesWereDeleted;
    }

    public void startBinariesGC() {
        if (isBinariesGCInProgress()) {
            return;
        }
        binaryManagerStatus = null;
        binaryManagerStatusInvalidation = false;
        Runnable gcTask = new BinariesGCTask(binariesGCDelete);
        Thread t = new Thread(gcTask, "NuxeoBinariesGCUI");
        t.setDaemon(true);
        t.start();
        binariesWereDeleted = binariesGCDelete;
        binariesGCDelete = false;
    }

    public boolean isBinariesGCInProgress() {
        return new RepositoryStatus().isBinariesGCInProgress();
    }

    public boolean isBinaryManagerStatusAvailable() {
        if (binaryManagerStatusInvalidation) {
            // invalidate Seam value in context
            Contexts.getEventContext().remove("binaryManagerStatus");
            binaryManagerStatusInvalidation = false;
        }
        return binaryManagerStatus != null;
    }

    @Factory(value = "binaryManagerStatus", scope = ScopeType.EVENT)
    public BinaryManagerStatus getBinaryManagerStatus() {
        return binaryManagerStatus;

    }

    public class BinariesGCTask implements Runnable {
        public boolean delete;

        public BinariesGCTask(boolean delete) {
            this.delete = delete;
        }

        @Override
        public void run() {
            try {
                binaryManagerStatus = new RepositoryStatus().gcBinaries(delete);
                binaryManagerStatusInvalidation = true;
            } catch (RuntimeException e) {
                log.error("Error while executing BinariesGCTask", e);
            }
        }
    }

}
