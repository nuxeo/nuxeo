/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Thierry Delprat
 */
package org.nuxeo.apidoc.browse;

import static org.nuxeo.apidoc.snapshot.DistributionSnapshot.PROP_RELEASED;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.sql.Date;
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.naming.NamingException;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.apidoc.documentation.DocumentationService;
import org.nuxeo.apidoc.export.ArchiveFile;
import org.nuxeo.apidoc.listener.AttributesExtractorStater;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.snapshot.DistributionSnapshotDesc;
import org.nuxeo.apidoc.snapshot.SnapshotFilter;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.apidoc.snapshot.SnapshotManagerComponent;
import org.nuxeo.apidoc.snapshot.SnapshotResolverHelper;
import org.nuxeo.apidoc.worker.ExtractXmlAttributesWorker;
import org.nuxeo.common.Environment;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.query.QueryFilter;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.webengine.forms.FormData;
import org.nuxeo.ecm.webengine.model.Resource;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

@Path("/distribution")
// needed for 5.4.1
@WebObject(type = "distribution")
public class Distribution extends ModuleRoot {

    public static final String DIST_ID = "distId";

    protected static final Log log = LogFactory.getLog(Distribution.class);

    protected static final Pattern VERSION_REGEX = Pattern.compile("^(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?(?:-.*)?$",
            Pattern.CASE_INSENSITIVE);

    // handle errors
    @Override
    public Object handleError(Throwable t) {
        if (t instanceof WebResourceNotFoundException) {
            return Response.status(404).entity(getTemplate("error/error_404.ftl")).type("text/html").build();
        } else {
            return super.handleError(t);
        }
    }

    protected SnapshotManager getSnapshotManager() {
        return Framework.getService(SnapshotManager.class);
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

    @Path("latest")
    public Resource getLatest() {
        List<DistributionSnapshot> snaps = listPersistedDistributions();
        Optional<DistributionSnapshot> distribution = snaps.stream()
                                                           .filter(snap -> snap.getName()
                                                                               .toLowerCase()
                                                                               .startsWith("nuxeo platform"))
                                                           .findFirst();

        String latest = "current";
        if (distribution.isPresent()) {
            latest = distribution.get().getKey();
        }
        return ctx.newObject("redirectWO", "latest", latest);
    }

    @Path("{distributionId}")
    public Resource viewDistribution(@PathParam("distributionId") String distributionId) {
        if (distributionId == null || "".equals(distributionId)) {
            return this;
        }

        List<DistributionSnapshot> snaps = getSnapshotManager().listPersistentSnapshots((ctx.getCoreSession()));
        if (distributionId.matches(VERSION_REGEX.toString())) {
            String finalDistributionId = distributionId;
            String distribution = snaps.stream()
                                       .filter(s -> s.getVersion().equals(finalDistributionId))
                                       .findFirst()
                                       .map(DistributionSnapshot::getKey)
                                       .orElse("current");

            return ctx.newObject("redirectWO", finalDistributionId, distribution);
        }

        String orgDistributionId = distributionId;
        Boolean embeddedMode = Boolean.FALSE;
        if ("adm".equals(distributionId)) {
            embeddedMode = Boolean.TRUE;
        } else {

            snaps.add(getSnapshotManager().getRuntimeSnapshot());
            distributionId = SnapshotResolverHelper.findBestMatch(snaps, distributionId);
        }
        if (distributionId == null || "".equals(distributionId)) {
            distributionId = "current";
        }

        if (!orgDistributionId.equals(distributionId)) {
            return ctx.newObject("redirectWO", orgDistributionId, distributionId);
        }

        ctx.setProperty("embeddedMode", embeddedMode);
        ctx.setProperty("distribution", getSnapshotManager().getSnapshot(distributionId, ctx.getCoreSession()));
        ctx.setProperty(DIST_ID, distributionId);
        return ctx.newObject("apibrowser", distributionId, embeddedMode);
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

    public List<DistributionSnapshot> listPersistedDistributions() {
        SnapshotManager sm = getSnapshotManager();
        return sm.listPersistentSnapshots(ctx.getCoreSession())
                 .stream()
                 .sorted((o1, o2) -> {
                     Matcher m1 = VERSION_REGEX.matcher(o1.getVersion());
                     Matcher m2 = VERSION_REGEX.matcher(o2.getVersion());

                     if (m1.matches() && m2.matches()) {
                         for (int i = 0; i < 3; i++) {
                             String s1 = m1.group(i + 1);
                             int c1 = s1 != null ? Integer.parseInt(s1) : 0;
                             String s2 = m2.group(i + 1);
                             int c2 = s2 != null ? Integer.parseInt(s2) : 0;

                             if (c1 != c2 || i == 2) {
                                 return Integer.compare(c2, c1);
                             }
                         }
                     }
                     log.info(String.format("Comparing version using String between %s - %s", o1.getVersion(),
                             o2.getVersion()));
                     return o2.getVersion().compareTo(o1.getVersion());
                 })
                 .filter(s -> !s.isHidden())
                 .collect(Collectors.toList());
    }

    public Map<String, DistributionSnapshot> getPersistedDistributions() {
        return getSnapshotManager().getPersistentSnapshots(ctx.getCoreSession());
    }

    public DistributionSnapshot getCurrentDistribution() {
        String distId = (String) ctx.getProperty(DIST_ID);
        DistributionSnapshot currentDistribution = (DistributionSnapshot) ctx.getProperty("currentDistribution");
        if (currentDistribution == null || !currentDistribution.getKey().equals(distId)) {
            currentDistribution = getSnapshotManager().getSnapshot(distId, ctx.getCoreSession());
            ctx.setProperty("currentDistribution", currentDistribution);
        }
        return currentDistribution;
    }

    @POST
    @Path("save")
    @Produces("text/html")
    public Object doSave() throws NamingException, NotSupportedException, SystemException, RollbackException,
            HeuristicMixedException, HeuristicRollbackException, ParseException {
        if (!canAddDocumentation()) {
            return null;
        }
        FormData formData = getContext().getForm();
        String distribLabel = formData.getString("name");

        log.info("Start Snapshot...");
        boolean startedTx = false;
        UserTransaction tx = TransactionHelper.lookupUserTransaction();
        if (tx != null && !TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            tx.begin();
            startedTx = true;
        }

        Map<String, Serializable> otherProperties = readFormData(formData);
        try {
            getSnapshotManager().persistRuntimeSnapshot(getContext().getCoreSession(), distribLabel, otherProperties);

        } catch (NuxeoException e) {
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
        log.debug("Path => " + redirectUrl);
        return getView("saved");
    }

    protected Map<String, Serializable> readFormData(FormData formData) {
        Map<String, Serializable> properties = new HashMap<>();

        // Release date
        String released = formData.getString("released");
        if (StringUtils.isNotBlank(released)) {
            LocalDate date = LocalDate.parse(released);
            Instant instant = date.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
            properties.put(PROP_RELEASED, Date.from(instant));
        }

        return properties;
    }

    @POST
    @Path("saveExtended")
    @Produces("text/html")
    public Object doSaveExtended() throws NamingException, NotSupportedException, SystemException, SecurityException,
            RollbackException, HeuristicMixedException, HeuristicRollbackException {
        if (!canAddDocumentation()) {
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

        Map<String, Serializable> otherProperties = readFormData(formData);

        log.info("Start Snapshot...");
        boolean startedTx = false;
        UserTransaction tx = TransactionHelper.lookupUserTransaction();
        if (tx != null && !TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            tx.begin();
            startedTx = true;
        }
        try {
            getSnapshotManager().persistRuntimeSnapshot(getContext().getCoreSession(), distribLabel, otherProperties,
                    filter);
        } catch (NuxeoException e) {
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

    public String getDocumentationInfo() {
        DocumentationService ds = Framework.getService(DocumentationService.class);
        return ds.getDocumentationStats(getContext().getCoreSession());
    }

    protected File getExportTmpFile() {
        File tmpFile = new File(Environment.getDefault().getTemp(), "export.zip");
        if (tmpFile.exists()) {
            tmpFile.delete();
        }
        tmpFile.deleteOnExit();
        return tmpFile;
    }

    @GET
    @Path("downloadDoc")
    public Response downloadDoc() throws IOException {
        DocumentationService ds = Framework.getService(DocumentationService.class);
        File tmp = getExportTmpFile();
        tmp.createNewFile();
        OutputStream out = new FileOutputStream(tmp);
        ds.exportDocumentation(getContext().getCoreSession(), out);
        out.flush();
        out.close();
        ArchiveFile aFile = new ArchiveFile(tmp.getAbsolutePath());
        return Response.ok(aFile)
                       .header("Content-Disposition", "attachment;filename=" + "nuxeo-documentation.zip")
                       .type("application/zip")
                       .build();
    }

    @GET
    @Path("download/{distributionId}")
    public Response downloadDistrib(@PathParam("distributionId") String distribId) throws IOException {
        File tmp = getExportTmpFile();
        tmp.createNewFile();
        OutputStream out = new FileOutputStream(tmp);
        getSnapshotManager().exportSnapshot(getContext().getCoreSession(), distribId, out);
        out.close();
        String fName = "nuxeo-distribution-" + distribId + ".zip";
        fName = fName.replace(" ", "_");
        ArchiveFile aFile = new ArchiveFile(tmp.getAbsolutePath());
        return Response.ok(aFile)
                       .header("Content-Disposition", "attachment;filename=" + fName)
                       .type("application/zip")
                       .build();
    }

    /**
     * Use to allow authorized users to upload distribution even in site mode
     *
     * @since 8.3
     */
    @GET
    @Path("_admin")
    public Object getForms() {
        NuxeoPrincipal principal = getContext().getPrincipal();
        if (SecurityHelper.canEditDocumentation(principal)) {
            return getView("forms").arg("hideNav", Boolean.TRUE);
        } else {
            return Response.status(401).build();
        }
    }

    @POST
    @Path("uploadDistrib")
    @Produces("text/html")
    public Object uploadDistrib() throws IOException {
        if (!canAddDocumentation()) {
            return null;
        }
        Blob blob = getContext().getForm().getFirstBlob();

        getSnapshotManager().importSnapshot(getContext().getCoreSession(), blob.getStream());
        getSnapshotManager().readPersistentSnapshots(getContext().getCoreSession());

        return getView("index");
    }

    @POST
    @Path("uploadDistribTmp")
    @Produces("text/html")
    public Object uploadDistribTmp() throws IOException {
        if (!canAddDocumentation()) {
            return null;
        }
        Blob blob = getContext().getForm().getFirstBlob();
        if (blob == null || blob.getLength() == 0) {
            return null;
        }
        DocumentModel snap = getSnapshotManager().importTmpSnapshot(getContext().getCoreSession(), blob.getStream());
        if (snap == null) {
            log.error("Unable to import archive");
            return null;
        }
        DistributionSnapshot snapObject = snap.getAdapter(DistributionSnapshot.class);
        return getView("uploadEdit").arg("tmpSnap", snap).arg("snapObject", snapObject);
    }

    @POST
    @Path("uploadDistribTmpValid")
    @Produces("text/html")
    public Object uploadDistribTmpValid() {
        if (!canAddDocumentation()) {
            return null;
        }

        FormData formData = getContext().getForm();
        String name = formData.getString("name");
        String version = formData.getString("version");
        String pathSegment = formData.getString("pathSegment");
        String title = formData.getString("title");

        getSnapshotManager().validateImportedSnapshot(getContext().getCoreSession(), name, version, pathSegment, title);
        getSnapshotManager().readPersistentSnapshots(getContext().getCoreSession());
        return getView("importDone");
    }

    @POST
    @Path("uploadDoc")
    @Produces("text/html")
    public Object uploadDoc() throws IOException {
        if (!canAddDocumentation()) {
            return null;
        }

        Blob blob = getContext().getForm().getFirstBlob();
        if (blob == null || blob.getLength() == 0) {
            return null;
        }

        DocumentationService ds = Framework.getService(DocumentationService.class);
        ds.importDocumentation(getContext().getCoreSession(), blob.getStream());

        log.info("Documents imported.");

        return getView("docImportDone");
    }

    @GET
    @Path("_reindex")
    @Produces("text/plain")
    public Object reindex() {
        NuxeoPrincipal nxPrincipal = getContext().getPrincipal();
        if (!nxPrincipal.isAdministrator()) {
            return Response.status(404).build();
        }

        CoreSession coreSession = getContext().getCoreSession();
        String query = String.format(
                "SELECT ecm:uuid FROM Document WHERE ecm:primaryType in ('%s') AND ecm:isProxy = 0 AND ecm:isTrashed = 0",
                StringUtils.join(AttributesExtractorStater.DOC_TYPES, "','"));

        try (IterableQueryResult it = coreSession.queryAndFetch(query, NXQL.NXQL, QueryFilter.EMPTY);) {
            for (Map<String, Serializable> map : it) {
                String id = (String) map.get(NXQL.ECM_UUID);
                Work work = new ExtractXmlAttributesWorker(coreSession.getRepositoryName(), nxPrincipal.getName(), id);
                Framework.getService(WorkManager.class).schedule(work);
            }
        }

        return Response.ok().build();
    }

    public boolean isEmbeddedMode() {
        Boolean embed = (Boolean) getContext().getProperty("embeddedMode", Boolean.FALSE);
        return embed != null && embed;
    }

    public boolean isEditor() {
        if (isEmbeddedMode() || isSiteMode()) {
            return false;
        }
        NuxeoPrincipal principal = getContext().getPrincipal();
        return SecurityHelper.canEditDocumentation(principal);
    }

    public boolean canAddDocumentation() {
        NuxeoPrincipal principal = getContext().getPrincipal();
        return !isEmbeddedMode() && SecurityHelper.canEditDocumentation(principal);
    }

    public static boolean showCurrentDistribution() {
        return !(Framework.isBooleanPropertyTrue("org.nuxeo.apidoc.hide.current.distribution") || isSiteMode());
    }

    public static boolean showSeamComponent() {
        return !(Framework.isBooleanPropertyTrue("org.nuxeo.apidoc.hide.seam.components") || isSiteMode());
    }

    public static boolean isSiteMode() {
        return Framework.isBooleanPropertyTrue("org.nuxeo.apidoc.site.mode");
    }
}
