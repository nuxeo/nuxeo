/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.apidoc.browse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import javax.transaction.UserTransaction;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.commons.io.filefilter.OrFileFilter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.apidoc.documentation.DocumentationService;
import org.nuxeo.apidoc.export.ArchiveFile;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.snapshot.DistributionSnapshotDesc;
import org.nuxeo.apidoc.snapshot.SnapshotFilter;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.apidoc.snapshot.SnapshotManagerComponent;
import org.nuxeo.apidoc.snapshot.SnapshotResolverHelper;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.webengine.forms.FormData;
import org.nuxeo.ecm.webengine.model.Resource;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

@Path("/distribution")
// needed for 5.4.1
@WebObject(type = "distribution")
public class Distribution extends ModuleRoot {

    public static final String DIST_ID = "distId";

    protected static final Log log = LogFactory.getLog(Distribution.class);

    protected SnapshotManager getSnapshotManager() {
        return Framework.getLocalService(SnapshotManager.class);
    }

    public String getNavigationPoint() {
        String currentUrl = getContext().getURL();
        String navPoint = "somewhere";

        if (currentUrl.contains("/listBundles")) {
            navPoint = "listBundles";
        } else if (currentUrl.contains("/listSeamComponents")) {
            navPoint = "listSeamComponents";
        } else if (currentUrl.contains("/viewSeamComponent")) {
            navPoint = "viewSeamComponent";
        } else if (currentUrl.contains("/listComponents")) {
            navPoint = "listComponents";
        } else if (currentUrl.contains("/listServices")) {
            navPoint = "listServices";
        } else if (currentUrl.contains("/listExtensionPoints")) {
            navPoint = "listExtensionPoints";
        } else if (currentUrl.contains("/listContributions")) {
            navPoint = "listContributions";
        } else if (currentUrl.contains("/listBundleGroups")) {
            navPoint = "listBundleGroups";
        } else if (currentUrl.contains("/viewBundleGroup")) {
            navPoint = "viewBundleGroup";
        } else if (currentUrl.contains("/viewComponent")) {
            navPoint = "viewComponent";
        } else if (currentUrl.contains("/viewService")) {
            navPoint = "viewService";
        } else if (currentUrl.contains("/viewExtensionPoint")) {
            navPoint = "viewExtensionPoint";
        } else if (currentUrl.contains("/viewContribution")) {
            navPoint = "viewContribution";
        } else if (currentUrl.contains("/viewBundle")) {
            navPoint = "viewBundle";
        } else if (currentUrl.contains("/listOperations")) {
            navPoint = "listOperations";
        } else if (currentUrl.contains("/viewOperation")) {
            navPoint = "viewOperation";
        } else if (currentUrl.contains("/doc")) {
            navPoint = "documentation";
        }
        return navPoint;
    }

    @GET
    @Produces("text/html")
    public Object doGet() {
        return getView("index").arg("hideNav", Boolean.TRUE);
    }

    @Path("{distributionId}")
    public Resource viewDistribution(@PathParam("distributionId")
    String distributionId) {
        try {
            if (distributionId == null || "".equals(distributionId)) {
                return this;
            }
            String orgDistributionId = distributionId;
            Boolean embeddedMode = Boolean.FALSE;
            if ("adm".equals(distributionId)) {
                embeddedMode = Boolean.TRUE;
            } else {
                List<DistributionSnapshot> snaps = getSnapshotManager().listPersistentSnapshots(
                        (ctx.getCoreSession()));
                snaps.add(getSnapshotManager().getRuntimeSnapshot());
                distributionId = SnapshotResolverHelper.findBestMatch(snaps,
                        distributionId);
            }
            if (distributionId == null || "".equals(distributionId)) {
                distributionId = "current";
            }

            if (!orgDistributionId.equals(distributionId)) {
                return ctx.newObject("redirectWO", orgDistributionId,
                        distributionId);
            }

            ctx.setProperty("embeddedMode", embeddedMode);
            ctx.setProperty(
                    "distribution",
                    getSnapshotManager().getSnapshot(distributionId,
                            ctx.getCoreSession()));
            ctx.setProperty(DIST_ID, distributionId);
            return ctx.newObject("apibrowser", distributionId, embeddedMode);
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
    }

    public List<DistributionSnapshotDesc> getAvailableDistributions() {
        return getSnapshotManager().getAvailableDistributions(
                ctx.getCoreSession());
    }

    public String getRuntimeDistributionName() {
        return SnapshotManagerComponent.RUNTIME;
    }

    public DistributionSnapshot getRuntimeDistribution() {
        return getSnapshotManager().getRuntimeSnapshot();
    }

    public List<DistributionSnapshot> listPersistedDistributions() {
        return getSnapshotManager().listPersistentSnapshots(
                ctx.getCoreSession());
    }

    public Map<String, DistributionSnapshot> getPersistedDistributions() {
        return getSnapshotManager().getPersistentSnapshots(ctx.getCoreSession());
    }

    public DistributionSnapshot getCurrentDistribution() {
        String distId = (String) ctx.getProperty(DIST_ID);
        DistributionSnapshot currentDistribution = (DistributionSnapshot) ctx.getProperty("currentDistribution");
        if (currentDistribution == null
                || !currentDistribution.getKey().equals(distId)) {
            currentDistribution = getSnapshotManager().getSnapshot(distId,
                    ctx.getCoreSession());
            ctx.setProperty("currentDistribution", currentDistribution);
        }
        return currentDistribution;
    }

    @POST
    @Path("save")
    @Produces("text/html")
    public Object doSave() throws Exception {
        if (!isEditor()) {
            return null;
        }
        FormData formData = getContext().getForm();
        String distribLabel = formData.getString("name");

        log.info("Start Snapshot...");
        boolean startedTx = false;
        UserTransaction tx = TransactionHelper.lookupUserTransaction();
        if (tx != null
                && !TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            tx.begin();
            startedTx = true;
        }
        try {
            getSnapshotManager().persistRuntimeSnapshot(
                    getContext().getCoreSession(), distribLabel);
        } catch (Exception e) {
            log.error("Error during storage", e);
            if (tx != null) {
                tx.rollback();
            }
            return getView("savedKO").arg("message", e.getMessage());
        }
        log.info("Snapshot saved.");
        if (tx != null && startedTx) {
            tx.commit();
        }

        String redirectUrl = getContext().getBaseURL() + getPath();
        log.error("Path => " + redirectUrl);
        return getView("saved");
    }

    @POST
    @Path("saveExtended")
    @Produces("text/html")
    public Object doSaveExtended() throws Exception {
        if (!isEditor()) {
            return null;
        }

        FormData formData = getContext().getForm();

        String distribLabel = formData.getString("name");
        String bundleList = formData.getString("bundles");
        String pkgList = formData.getString("packages");
        SnapshotFilter filter = new SnapshotFilter(distribLabel);

        if (bundleList != null) {
            String[] bundles = bundleList.split("\n");
            for (String bundleId : bundles) {
                filter.addBundlePrefix(bundleId);
            }
        }

        if (pkgList != null) {
            String[] packages = pkgList.split("\\r?\\n");
            for (String pkg : packages) {
                filter.addPackagesPrefix(pkg);
            }
        }

        log.info("Start Snapshot...");
        boolean startedTx = false;
        UserTransaction tx = TransactionHelper.lookupUserTransaction();
        if (tx != null
                && !TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            tx.begin();
            startedTx = true;
        }
        try {
            getSnapshotManager().persistRuntimeSnapshot(
                    getContext().getCoreSession(), distribLabel, filter);
        } catch (Exception e) {
            log.error("Error during storage", e);
            if (tx != null) {
                tx.rollback();
            }
            return getView("savedKO").arg("message", e.getMessage());
        }
        log.info("Snapshot saved.");
        if (tx != null && startedTx) {
            tx.commit();
        }
        return getView("saved");
    }

    public String getDocumentationInfo() throws Exception {
        DocumentationService ds = Framework.getService(DocumentationService.class);
        return ds.getDocumentationStats(getContext().getCoreSession());
    }

    protected File getExportTmpFile() {
        String fPath = System.getProperty("java.io.tmpdir") + "/export.zip";
        File tmpFile = new File(fPath);
        if (tmpFile.exists()) {
            tmpFile.delete();
            tmpFile = new File(fPath);
        }
        tmpFile.deleteOnExit();
        return tmpFile;
    }

    @GET
    @Path("downloadDoc")
    public Response downloadDoc() throws Exception {
        DocumentationService ds = Framework.getService(DocumentationService.class);
        File tmp = getExportTmpFile();
        tmp.createNewFile();
        OutputStream out = new FileOutputStream(tmp);
        ds.exportDocumentation(getContext().getCoreSession(), out);
        out.flush();
        out.close();
        ArchiveFile aFile = new ArchiveFile(tmp.getAbsolutePath());
        return Response.ok(aFile).header("Content-Disposition",
                "attachment;filename=" + "nuxeo-documentation.zip").type(
                "application/zip").build();
    }

    @GET
    @Path("download/{distributionId}")
    public Response downloadDistrib(@PathParam("distributionId")
    String distribId) throws Exception {
        File tmp = getExportTmpFile();
        tmp.createNewFile();
        OutputStream out = new FileOutputStream(tmp);
        getSnapshotManager().exportSnapshot(getContext().getCoreSession(),
                distribId, out);
        out.close();
        String fName = "nuxeo-distribution-" + distribId + ".zip";
        fName = fName.replace(" ", "_");
        ArchiveFile aFile = new ArchiveFile(tmp.getAbsolutePath());
        return Response.ok(aFile).header("Content-Disposition",
                "attachment;filename=" + fName).type("application/zip").build();
    }

    @POST
    @Path("uploadDistrib")
    @Produces("text/html")
    public Object uploadDistrib() throws Exception {
        if (!isEditor()) {
            return null;
        }
        Blob blob = getContext().getForm().getFirstBlob();

        getSnapshotManager().importSnapshot(getContext().getCoreSession(),
                blob.getStream());
        getSnapshotManager().readPersistentSnapshots(
                getContext().getCoreSession());

        return getView("index");
    }

    @POST
    @Path("uploadDoc")
    @Produces("text/html")
    // @Guard(value=SecurityConstants.Write_Group,type=GroupGuard.class)
    public Object uploadDoc() throws Exception {
        if (!isEditor()) {
            return null;
        }
        UserTransaction tx = TransactionHelper.lookupUserTransaction();
        if (tx != null) {
            tx.begin();
        }
        Blob blob = getContext().getForm().getFirstBlob();

        DocumentationService ds = Framework.getService(DocumentationService.class);
        ds.importDocumentation(getContext().getCoreSession(), blob.getStream());

        log.info("Documents imported.");
        if (tx != null) {
            tx.commit();
        }
        return getView("index");
    }

    public boolean isEmbeddedMode() {
        Boolean embed = (Boolean) getContext().getProperty("embeddedMode",
                Boolean.FALSE);
        return embed == null ? false : embed.booleanValue();
    }

    public boolean isEditor() {
        if (isEmbeddedMode()) {
            return false;
        }
        NuxeoPrincipal principal = (NuxeoPrincipal) getContext().getPrincipal();
        return SecurityHelper.canEditDocumentation(principal);
    }

}
