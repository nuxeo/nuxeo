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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.apidoc.documentation.DocumentationService;
import org.nuxeo.apidoc.export.ArchiveFile;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.snapshot.DistributionSnapshotDesc;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.apidoc.snapshot.SnapshotManagerComponent;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.webengine.model.Resource;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 *
 */
@WebObject(type = "distribution")
public class Distribution extends ModuleRoot{

    protected static final Log log = LogFactory.getLog(Distribution.class);

    protected SnapshotManager getSnapshotManager() {
        return Framework.getLocalService(SnapshotManager.class);
    }

    public String getNavigationPoint() {
        String currentUrl = getContext().getURL();
        String navPoint = "somewhere";

        if (currentUrl.contains("/listBundles")) {
            navPoint = "listBundles";
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
        } else if (currentUrl.contains("/doc")) {
            navPoint = "documentation";
        }

        return navPoint;
    }

    @GET
    @Produces("text/html")
    public Object doGet() {
        return getView("index");
    }

    @Path(value = "{distributionId}")
    public Resource viewDistribution(@PathParam("distributionId") String distributionId) {
        try {
            if (distributionId==null || "".equals(distributionId)) {
                return this;
            }
            Boolean embeddedMode = false;
            if ("adm".equals(distributionId)) {
                embeddedMode = true;
            }
            ctx.setProperty("embeddedMode",embeddedMode);
            ctx.setProperty("distribution", getSnapshotManager().getSnapshot(distributionId,ctx.getCoreSession()));
            ctx.setProperty("distId", distributionId);
            return ctx.newObject("apibrowser", distributionId, embeddedMode);
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
    }

    public List<DistributionSnapshotDesc> getAvailableDistributions() {
        return getSnapshotManager().getAvailableDistributions(ctx.getCoreSession());
    }

    public String getRuntimeDistributionName() {
        return SnapshotManagerComponent.RUNTIME;
    }

    public DistributionSnapshot getRuntimeDistribution() {
        return getSnapshotManager().getRuntimeSnapshot();
    }

    public Map<String,DistributionSnapshot> getPersistedDistributions() {
        return getSnapshotManager().getPersistentSnapshots(ctx.getCoreSession());
    }

    public DistributionSnapshot getCurrentDistribution() {
        String distId = (String) ctx.getProperty("distId");
        if (distId==null) {
            return null;
        } else {
            return getSnapshotManager().getSnapshot(distId,ctx.getCoreSession());
        }
    }

    @POST
    @Path(value = "save")
    @Produces("text/html")
    //@Guard(value=SecurityConstants.Write_Group,type=GroupGuard.class)
    public Object doSave() throws Exception {
        if (!isEditor()) {
            return null;
        }
        log.info("Start Snapshot...");
        UserTransaction tx = TransactionHelper.lookupUserTransaction();
        if (tx!=null) {
            tx.begin();
        }
        try {
            getSnapshotManager().persistRuntimeSnapshot(getContext().getCoreSession());
        }
        catch (Exception e) {
            log.error("Error during storage", e);
            if (tx!=null) {
                tx.rollback();
            }
            return getView("index");
        }
        log.info("Snapshot saved.");
        if (tx!=null) {
            tx.commit();
        }
        return getView("index");
    }

    public String getDocumentationInfo() throws Exception {
        DocumentationService ds = Framework.getService(DocumentationService.class);
        return ds.getDocumentationStats(getContext().getCoreSession());
    }

    @GET
    @Path(value = "downloadDoc")
    public Response downloadDoc() throws Exception {
        DocumentationService ds = Framework.getService(DocumentationService.class);
        File tmp = new File("/tmp/test.zip");
        //tmp = File.createTempFile("export", ".zip");
        tmp.createNewFile();
        OutputStream out = new FileOutputStream(tmp);
        ds.exportDocumentation(getContext().getCoreSession(), out);
        out.flush();
        out.close();
        ArchiveFile aFile = new ArchiveFile(tmp.getAbsolutePath());
        return Response.ok(aFile).header("Content-Disposition",
                 "attachment;filename=" + "nuxeo-documentation.zip").type("application/zip").build();
    }

    @GET
    @Path(value = "download/{distributionId}")
    public Response downloadDistrib(@PathParam("distributionId") String distribId) throws Exception {
        File tmp = new File("/tmp/test.zip");
        //tmp = File.createTempFile("export", ".zip");
        tmp.createNewFile();
        OutputStream out = new FileOutputStream(tmp);

        getSnapshotManager().exportSnapshot(getContext().getCoreSession(), distribId, out);
        out.close();
        ArchiveFile aFile = new ArchiveFile(tmp.getAbsolutePath());
        return Response.ok(aFile).header("Content-Disposition",
                 "attachment;filename=" + "nuxeo-documentation.zip").type("application/zip").build();
        //return Response.ok(aFile).header("Content-Disposition",
        //        "attachment;filename=" + "nuxeo-documentation.zip").build();

    }

    @POST
    @Path(value = "uploadDistrib")
    @Produces("text/html")
    public Object uploadDistrib() throws Exception {
        if (!isEditor()) {
            return null;
        }
        Blob blob = getContext().getForm().getFirstBlob();

        getSnapshotManager().importSnapshot(getContext().getCoreSession(), blob.getStream());
        getSnapshotManager().readPersistentSnapshots(getContext().getCoreSession());

        return getView("index");
    }

    @POST
    @Path(value = "uploadDoc")
    @Produces("text/html")
    //@Guard(value=SecurityConstants.Write_Group,type=GroupGuard.class)
    public Object uploadDoc() throws Exception {
        if (!isEditor()) {
            return null;
        }
        UserTransaction tx = TransactionHelper.lookupUserTransaction();
        if (tx!=null) {
            tx.begin();
        }
        Blob blob = getContext().getForm().getFirstBlob();

        DocumentationService ds = Framework.getService(DocumentationService.class);
        ds.importDocumentation(getContext().getCoreSession(), blob.getStream());

        log.info("Documents imported.");
        if (tx!=null) {
            tx.commit();
        }
        return getView("index");
    }

    public boolean isEmbeddedMode() {
        return (Boolean) getContext().getProperty("embeddedMode", false);
    }

    public boolean isEditor() {
        if (isEmbeddedMode()) {
            return false;
        }
        NuxeoPrincipal principal = (NuxeoPrincipal) getContext().getPrincipal();
        return SecurityHelper.canEditDocumentation(principal);
    }

}
