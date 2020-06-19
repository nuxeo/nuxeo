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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.apidoc.export.ArchiveFile;
import org.nuxeo.apidoc.introspection.RuntimeSnapshot;
import org.nuxeo.apidoc.listener.AttributesExtractorStater;
import org.nuxeo.apidoc.plugin.Plugin;
import org.nuxeo.apidoc.security.SecurityHelper;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.snapshot.DistributionSnapshotDesc;
import org.nuxeo.apidoc.snapshot.SnapshotFilter;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
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
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.ecm.webengine.forms.FormData;
import org.nuxeo.ecm.webengine.model.Resource;
import org.nuxeo.ecm.webengine.model.Template;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

@Path("/distribution")
// needed for 5.4.1
@WebObject(type = "distribution")
public class Distribution extends ModuleRoot {

    private static final Logger log = LogManager.getLogger(Distribution.class);

    public static final String DIST_ID = "distId";

    protected static final String DIST = "distribution";

    public static final String VIEW_INDEX = "index";

    public static final String VIEW_ADMIN = "_admin";

    protected static final Pattern VERSION_REGEX = Pattern.compile("^(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?(?:-.*)?$",
            Pattern.CASE_INSENSITIVE);

    @Override
    public Object handleError(Throwable t) {
        if (t instanceof WebResourceNotFoundException) {
            return show404();
        } else {
            return super.handleError(t);
        }
    }

    /**
     * Displays a customized 404 page.
     *
     * @since 11.2
     */
    public Object show404() {
        return Response.status(404).entity(getTemplate("views/error404/error_404.ftl")).type("text/html").build();
    }

    protected static SnapshotManager getSnapshotManager() {
        return Framework.getService(SnapshotManager.class);
    }

    public String getNavigationPoint() {
        String url = getContext().getURL();
        String point = null;
        if (ApiBrowserConstants.check(url, ApiBrowserConstants.LIST_BUNDLEGROUPS)
                || ApiBrowserConstants.check(url, ApiBrowserConstants.VIEW_BUNDLEGROUP)) {
            point = ApiBrowserConstants.LIST_BUNDLEGROUPS;
        } else if (ApiBrowserConstants.check(url, ApiBrowserConstants.LIST_BUNDLES)
                || ApiBrowserConstants.check(url, ApiBrowserConstants.VIEW_BUNDLE)) {
            point = ApiBrowserConstants.LIST_BUNDLES;
        } else if (ApiBrowserConstants.check(url, ApiBrowserConstants.LIST_COMPONENTS)
                || ApiBrowserConstants.check(url, ApiBrowserConstants.VIEW_COMPONENT)) {
            point = ApiBrowserConstants.LIST_COMPONENTS;
        } else if (ApiBrowserConstants.check(url, ApiBrowserConstants.LIST_SERVICES)
                || ApiBrowserConstants.check(url, ApiBrowserConstants.VIEW_SERVICE)) {
            point = ApiBrowserConstants.LIST_SERVICES;
        } else if (ApiBrowserConstants.check(url, ApiBrowserConstants.LIST_EXTENSIONPOINTS)
                || ApiBrowserConstants.check(url, ApiBrowserConstants.VIEW_EXTENSIONPOINT)) {
            point = ApiBrowserConstants.LIST_EXTENSIONPOINTS;
        } else if (ApiBrowserConstants.check(url, ApiBrowserConstants.LIST_CONTRIBUTIONS)
                || ApiBrowserConstants.check(url, ApiBrowserConstants.VIEW_CONTRIBUTION)) {
            point = ApiBrowserConstants.LIST_CONTRIBUTIONS;
        } else if (ApiBrowserConstants.check(url, ApiBrowserConstants.LIST_OPERATIONS)
                || ApiBrowserConstants.check(url, ApiBrowserConstants.VIEW_OPERATION)) {
            point = ApiBrowserConstants.LIST_OPERATIONS;
        } else if (ApiBrowserConstants.check(url, ApiBrowserConstants.LIST_PACKAGES)
                || ApiBrowserConstants.check(url, ApiBrowserConstants.VIEW_PACKAGE)) {
            point = ApiBrowserConstants.LIST_PACKAGES;
        } else if (ApiBrowserConstants.check(url, ApiBrowserConstants.VIEW_DOCUMENTATION)) {
            point = ApiBrowserConstants.VIEW_DOCUMENTATION;
        }
        if (point == null) {
            // check plugins
            List<Plugin<?>> plugins = getSnapshotManager().getPlugins();
            for (Plugin<?> plugin : plugins) {
                point = plugin.getView(url);
                if (point != null) {
                    break;
                }
            }
        }

        return point;
    }

    @GET
    @Produces("text/html")
    public Object doGet() {
        return getView(VIEW_INDEX).arg("hideNav", Boolean.TRUE);
    }

    @Path(SnapshotManager.DISTRIBUTION_ALIAS_LATEST)
    public Resource getLatest() {
        return listPersistedDistributions().stream()
                                           .filter(snap -> snap.getName().toLowerCase().startsWith("nuxeo platform"))
                                           .findFirst()
                                           .map(distribution -> ctx.newObject(RedirectResource.TYPE,
                                                   SnapshotManager.DISTRIBUTION_ALIAS_LATEST, distribution.getKey()))
                                           .orElseGet(() -> ctx.newObject(Resource404.TYPE));
    }

    @Path("{distributionId}")
    public Resource viewDistribution(@PathParam("distributionId") String distributionId) {
        if (StringUtils.isBlank(distributionId)) {
            return this;
        }
        if (isSiteMode() && RuntimeSnapshot.LIVE_ALIASES.contains(distributionId)) {
            return ctx.newObject(Resource404.TYPE);
        }

        List<DistributionSnapshot> snaps = getSnapshotManager().listPersistentSnapshots((ctx.getCoreSession()));
        if (distributionId.matches(VERSION_REGEX.toString())) {
            String finalDistributionId = distributionId;
            return snaps.stream()
                        .filter(s -> s.getVersion().equals(finalDistributionId))
                        .findFirst()
                        .map(distribution -> ctx.newObject(RedirectResource.TYPE, finalDistributionId,
                                distribution.getKey()))
                        .orElseGet(() -> ctx.newObject(Resource404.TYPE));
        }

        boolean showRuntimeSnapshot = showRuntimeSnapshot();
        String orgDistributionId = distributionId;
        Boolean embeddedMode = Boolean.FALSE;
        if (SnapshotManager.DISTRIBUTION_ALIAS_ADM.equals(distributionId)) {
            if (!showRuntimeSnapshot) {
                return ctx.newObject(Resource404.TYPE);
            }
            embeddedMode = Boolean.TRUE;
        } else {
            if (showRuntimeSnapshot) {
                snaps.add(getRuntimeDistribution());
            }
            distributionId = SnapshotResolverHelper.findBestMatch(snaps, distributionId);
        }
        if (StringUtils.isBlank(distributionId)) {
            return ctx.newObject(Resource404.TYPE);
        }
        if (!orgDistributionId.equals(distributionId)) {
            return ctx.newObject(RedirectResource.TYPE, orgDistributionId, distributionId);
        }

        ctx.setProperty(ApiBrowserConstants.EMBEDDED_MODE_MARKER, embeddedMode);
        ctx.setProperty(DIST, getSnapshotManager().getSnapshot(distributionId, ctx.getCoreSession()));
        ctx.setProperty(DIST_ID, distributionId);
        return ctx.newObject(ApiBrowser.TYPE, distributionId, embeddedMode);
    }

    public List<DistributionSnapshotDesc> getAvailableDistributions() {
        return getSnapshotManager().getAvailableDistributions(ctx.getCoreSession());
    }

    public DistributionSnapshot getRuntimeDistribution() {
        return getSnapshotManager().getRuntimeSnapshot();
    }

    public List<DistributionSnapshot> listPersistedDistributions() {
        SnapshotManager sm = getSnapshotManager();
        return sm.listPersistentSnapshots(ctx.getCoreSession()).stream().sorted((o1, o2) -> {
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
            log.info(String.format("Comparing version using String between %s - %s", o1.getVersion(), o2.getVersion()));
            return o2.getVersion().compareTo(o1.getVersion());
        }).filter(s -> !s.isHidden()).collect(Collectors.toList());
    }

    public Map<String, DistributionSnapshot> getPersistedDistributions() {
        return getSnapshotManager().getPersistentSnapshots(ctx.getCoreSession());
    }

    public DistributionSnapshot getCurrentDistribution() {
        return (DistributionSnapshot) ctx.getProperty(DIST);
    }

    @POST
    @Path("save")
    @Produces("text/html")
    public Object doSave() throws NamingException, NotSupportedException, SystemException, RollbackException,
            HeuristicMixedException, HeuristicRollbackException, ParseException {
        return performSave(null);
    }

    @POST
    @Path("saveExtended")
    @Produces("text/html")
    public Object doSaveExtended() throws NamingException, NotSupportedException, SystemException, SecurityException,
            RollbackException, HeuristicMixedException, HeuristicRollbackException {
        FormData formData = getContext().getForm();

        String distribLabel = formData.getString("name");
        String bundleList = formData.getString("bundles");
        String javaPkgList = formData.getString("javaPackages");
        String nxPkgList = formData.getString("nxPackages");
        SnapshotFilter filter = new SnapshotFilter(distribLabel);

        if (bundleList != null) {
            Arrays.stream(bundleList.split("\n"))
                  .filter(StringUtils::isNotBlank)
                  .forEach(bid -> filter.addBundlePrefix(bid));
        }
        if (javaPkgList != null) {
            Arrays.stream(javaPkgList.split("\n"))
                  .filter(StringUtils::isNotBlank)
                  .forEach(pkg -> filter.addPackagesPrefix(pkg));
        }
        if (nxPkgList != null) {
            Arrays.stream(nxPkgList.split("\n"))
                  .filter(StringUtils::isNotBlank)
                  .forEach(pkg -> filter.addNuxeoPackagePrefix(pkg));
        }

        return performSave(filter);
    }

    protected Map<String, Serializable> readFormData(FormData formData) {
        Map<String, Serializable> properties = new HashMap<>();

        // Release date
        String released = formData.getString("released");
        if (StringUtils.isNotBlank(released)) {
            LocalDate date = LocalDate.parse(released);
            Instant instant = date.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
            properties.put(PROP_RELEASED, java.util.Date.from(instant));
        }

        return properties;
    }

    protected Object performSave(SnapshotFilter filter) throws NamingException, NotSupportedException, SystemException,
            SecurityException, RollbackException, HeuristicMixedException, HeuristicRollbackException {
        if (!canSave()) {
            return show404();
        }

        boolean startedTx = false;
        UserTransaction tx = TransactionHelper.lookupUserTransaction();
        if (tx != null && !TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            tx.begin();
            startedTx = true;
        }

        FormData formData = getContext().getForm();
        String source = formData.getString("source");
        try {
            getSnapshotManager().persistRuntimeSnapshot(getContext().getCoreSession(), formData.getString("name"),
                    readFormData(formData), filter);
        } catch (NuxeoException e) {
            log.error("Error during storage", e);
            if (tx != null) {
                tx.rollback();
            }
            return getView("savedKO").arg("message", e.getMessage()).arg("source", source);
        }

        if (tx != null && startedTx) {
            tx.commit();
        }
        return getView("saved").arg("source", source);
    }

    /**
     * Returns the runtime snapshot json export.
     *
     * @since 11.1
     */
    @GET
    @Path("json")
    @Produces("application/json")
    public Object getJson() throws IOException {
        if (!showRuntimeSnapshot()) {
            return show404();
        }
        // init potential resources depending on request
        getSnapshotManager().initWebContext(getContext().getRequest());
        DistributionSnapshot snap = getRuntimeDistribution();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        snap.writeJson(out);
        return out.toString();
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
    @Path("download/{distributionId}")
    public Response downloadDistrib(@PathParam("distributionId") String distribId) throws IOException {
        if (!canImportOrExportDistributions()) {
            return Response.status(404).build();
        }

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
    @Path(VIEW_ADMIN)
    public Object getForms() {
        NuxeoPrincipal principal = getContext().getPrincipal();
        if (SecurityHelper.canManageDistributions(principal)) {
            return getView("forms").arg("hideNav", Boolean.TRUE);
        }
        return show404();
    }

    @POST
    @Path("uploadDistrib")
    @Produces("text/html")
    public Object uploadDistrib() throws IOException {
        if (!canImportOrExportDistributions()) {
            return show404();
        }
        FormData formData = getContext().getForm();
        Blob blob = formData.getFirstBlob();
        String source = formData.getString("source");

        try {
            getSnapshotManager().importSnapshot(getContext().getCoreSession(), blob.getStream());
            getSnapshotManager().readPersistentSnapshots(getContext().getCoreSession());
        } catch (IOException | IllegalArgumentException | NuxeoException e) {
            return getView("importKO").arg("message", e.getMessage()).arg("source", source);
        }

        return getView(getRedirectViewPostUpload(source));
    }

    @POST
    @Path("uploadDistribTmp")
    @Produces("text/html")
    public Object uploadDistribTmp() {
        if (!canImportOrExportDistributions()) {
            return show404();
        }
        FormData formData = getContext().getForm();
        Blob blob = formData.getFirstBlob();
        if (blob == null || blob.getLength() == 0) {
            return null;
        }
        Template view;
        try {
            DocumentModel snap = getSnapshotManager().importTmpSnapshot(getContext().getCoreSession(),
                    blob.getStream());
            if (snap == null) {
                view = getView("importKO").arg("message", "Unable to import archive.");
            } else {
                DistributionSnapshot snapObject = snap.getAdapter(DistributionSnapshot.class);
                view = getView("uploadEdit").arg("tmpSnap", snap).arg("snapObject", snapObject);
            }
        } catch (IOException | IllegalArgumentException | NuxeoException e) {
            view = getView("importKO").arg("message", e.getMessage());
        }

        view.arg("source", formData.getString("source"));
        return view;
    }

    @POST
    @Path("uploadDistribTmpValid")
    @Produces("text/html")
    public Object uploadDistribTmpValid() {
        if (!canImportOrExportDistributions()) {
            return show404();
        }

        FormData formData = getContext().getForm();
        String name = formData.getString("name");
        String version = formData.getString("version");
        String pathSegment = formData.getString("pathSegment");
        String title = formData.getString("title");

        Template view;
        try {
            getSnapshotManager().validateImportedSnapshot(getContext().getCoreSession(), name, version, pathSegment,
                    title);
            getSnapshotManager().readPersistentSnapshots(getContext().getCoreSession());
            view = getView("importDone");
        } catch (IllegalArgumentException | NuxeoException e) {
            view = getView("importKO").arg("message", e.getMessage());
        }

        view.arg("source", formData.getString("source"));
        return view;
    }

    /**
     * Returns the view to redirect to depending on source upload page.
     *
     * @since 11.1
     */
    public String getRedirectViewPostUpload(String source) {
        if ("admin".equals(source)) {
            return VIEW_ADMIN;
        }
        return "";
    }

    @GET
    @Path("_reindex")
    @Produces("text/plain")
    public Object reindex() {
        NuxeoPrincipal nxPrincipal = getContext().getPrincipal();
        if (!nxPrincipal.isAdministrator()) {
            return show404();
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

    public boolean isSiteMode() {
        return getSnapshotManager().isSiteMode();
    }

    public boolean isEmbeddedMode() {
        return Boolean.TRUE.equals(getContext().getProperty(ApiBrowserConstants.EMBEDDED_MODE_MARKER, Boolean.FALSE));
    }

    public boolean isEditor() {
        return !isSiteMode() && canImportOrExportDistributions();
    }

    protected boolean canSave() {
        return !isEmbeddedMode() && !isSiteMode()
                && SecurityHelper.canSnapshotLiveDistribution(getContext().getPrincipal());
    }

    protected boolean canImportOrExportDistributions() {
        return !isEmbeddedMode() && SecurityHelper.canManageDistributions(getContext().getPrincipal());
    }

    /**
     * Returns true if the current {@link RuntimeSnapshot} can be seen by user.
     *
     * @since 11.2
     */
    public boolean showRuntimeSnapshot() {
        return !isSiteMode() && SecurityHelper.canSnapshotLiveDistribution(getContext().getPrincipal());
    }

    /**
     * @since 11.2
     */
    public boolean isRunningFunctionalTests() {
        return !StringUtils.isBlank(Framework.getProperty(ApiBrowserConstants.PROPERTY_TESTER_NAME));
    }

    /**
     * Generates the list of plugins that should be displayed in the menu.
     */
    public List<Plugin<?>> getPluginMenu() {
        return getSnapshotManager().getPlugins()
                                   .stream()
                                   .filter(plugin -> !plugin.isHidden())
                                   .collect(Collectors.toList());
    }

    /**
     * Returns the webapp name (usually "nuxeo")
     *
     * @since 11.2
     */
    public String getWebappName() {
        return VirtualHostHelper.getWebAppName(getContext().getRequest());
    }

}
