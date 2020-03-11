/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 */
package org.nuxeo.connect.client.we;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.connect.client.vindoz.InstallAfterRestart;
import org.nuxeo.connect.packages.PackageManager;
import org.nuxeo.connect.packages.dependencies.DependencyResolution;
import org.nuxeo.connect.packages.dependencies.TargetPlatformFilterHelper;
import org.nuxeo.connect.update.LocalPackage;
import org.nuxeo.connect.update.Package;
import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.PackageUpdateService;
import org.nuxeo.connect.update.ValidationStatus;
import org.nuxeo.connect.update.Version;
import org.nuxeo.connect.update.model.Field;
import org.nuxeo.connect.update.model.Form;
import org.nuxeo.connect.update.task.Task;
import org.nuxeo.ecm.admin.runtime.PlatformVersionHelper;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.webengine.forms.FormData;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;

/**
 * Provides REST bindings for {@link Package} install management.
 *
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 */
@WebObject(type = "installHandler")
public class InstallHandler extends DefaultObject {

    protected static final Log log = LogFactory.getLog(InstallHandler.class);

    protected static final String INSTALL_PARAM_MAPS = "org.nuxeo.connect.updates.install.params";

    protected String getStorageKey(String pkgId) {
        return INSTALL_PARAM_MAPS + "_" + pkgId;
    }

    @SuppressWarnings("unchecked")
    protected Map<String, String> getInstallParameters(String pkgId) {
        Map<String, String> params = (Map<String, String>) getContext().getRequest().getAttribute(getStorageKey(pkgId));
        if (params == null) {
            params = new HashMap<>();
        }
        return params;
    }

    protected void storeInstallParameters(String pkgId, Map<String, String> params) {
        getContext().getRequest().setAttribute(getStorageKey(pkgId), params);
    }

    protected void clearInstallParameters(String pkgId) {
        getContext().getRequest().setAttribute(getStorageKey(pkgId), null);
    }

    @GET
    @Produces("text/html")
    @Path(value = "showTermsAndConditions/{pkgId}")
    public Object showTermsAndConditions(@PathParam("pkgId") String pkgId, @QueryParam("source") String source,
            @QueryParam("depCheck") Boolean depCheck) {
        if (depCheck == null) {
            depCheck = true;
        }
        try {
            PackageUpdateService pus = Framework.getService(PackageUpdateService.class);
            LocalPackage pkg = pus.getPackage(pkgId);
            String content = pkg.getTermsAndConditionsContent();
            return getView("termsAndConditions").arg("pkg", pkg).arg("source", source).arg("content", content).arg(
                    "depCheck", depCheck);
        } catch (PackageException e) {
            log.error("Error during terms and conditions phase ", e);
            return getView("installError").arg("e", e).arg("source", source);
        }
    }

    @GET
    @Produces("text/html")
    @Path(value = "start/{pkgId}")
    public Object startInstall(@PathParam("pkgId") String pkgId, @QueryParam("source") String source,
            @QueryParam("tacAccepted") Boolean acceptedTAC, @QueryParam("depCheck") Boolean depCheck,
            @QueryParam("autoMode") Boolean autoMode) {
        try {
            PackageUpdateService pus = Framework.getService(PackageUpdateService.class);
            LocalPackage pkg = pus.getPackage(pkgId);
            if (pkg == null) {
                throw new NuxeoException("Can not find package " + pkgId);
            }
            if (pkg.requireTermsAndConditionsAcceptance() && !Boolean.TRUE.equals(acceptedTAC)) {
                return showTermsAndConditions(pkgId, source, depCheck);
            }
            if (!Boolean.FALSE.equals(depCheck)) {
                // check deps requirements
                if (pkg.getDependencies() != null && pkg.getDependencies().length > 0) {
                    PackageManager pm = Framework.getService(PackageManager.class);
                    DependencyResolution resolution = pm.resolveDependencies(Collections.singletonList(pkgId),
                            Collections.emptyList(), Collections.emptyList(), PlatformVersionHelper.getPlatformFilter());
                    if (resolution.isFailed() && PlatformVersionHelper.getPlatformFilter() != null) {
                        // retry without PF filter ...
                        resolution = pm.resolveDependencies(Collections.singletonList(pkgId), Collections.emptyList(),
                                Collections.emptyList(), null);
                    }
                    if (resolution.isFailed()) {
                        return getView("dependencyError").arg("resolution", resolution).arg("pkg", pkg).arg("source",
                                source);
                    } else {
                        if (resolution.requireChanges()) {
                            if (autoMode == null) {
                                autoMode = true;
                            }
                            return getView("displayDependencies").arg("resolution", resolution).arg("pkg", pkg).arg(
                                    "source", source).arg("autoMode", autoMode);
                        }
                        // no dep changes => can continue standard install
                        // process
                    }
                }
            }
            Task installTask = pkg.getInstallTask();
            ValidationStatus status = installTask.validate();
            String targetPlatform = PlatformVersionHelper.getPlatformFilter();
            if (!TargetPlatformFilterHelper.isCompatibleWithTargetPlatform(pkg, targetPlatform)) {
                status.addWarning("This package is not validated for you current platform: " + targetPlatform);
            }
            if (status.hasErrors()) {
                return getView("canNotInstall").arg("status", status).arg("pkg", pkg).arg("source", source);
            }

            boolean needWizard = false;
            Form[] forms = installTask.getPackage().getInstallForms();
            if (forms != null && forms.length > 0) {
                needWizard = true;
            }
            return getView("startInstall").arg("status", status).arg("needWizard", needWizard).arg("installTask",
                    installTask).arg("pkg", pkg).arg("source", source);
        } catch (PackageException e) {
            log.error("Error during first step of installation", e);
            return getView("installError").arg("e", e).arg("source", source);
        }
    }

    @GET
    @Produces("text/html")
    @Path(value = "form/{pkgId}/{formId}")
    public Object showInstallForm(@PathParam("pkgId") String pkgId, @PathParam("formId") int formId,
            @QueryParam("source") String source) {
        PackageUpdateService pus = Framework.getService(PackageUpdateService.class);
        try {
            LocalPackage pkg = pus.getPackage(pkgId);
            Task installTask = pkg.getInstallTask();
            Form[] forms = installTask.getPackage().getInstallForms();
            if (forms == null || forms.length < formId - 1) {
                return getView("installError").arg("e",
                        new NuxeoException("No form with Id " + formId + " for package " + pkgId)).arg("source",
                        source);
            }
            return getView("showInstallForm").arg("form", forms[formId]).arg("pkg", pkg).arg("source", source).arg(
                    "step", formId + 1).arg("steps", forms.length);
        } catch (PackageException e) {
            log.error("Error during displaying Form nb " + formId, e);
            return getView("installError").arg("e", e).arg("source", source);
        }
    }

    @POST
    @Produces("text/html")
    @Path(value = "form/{pkgId}/{formId}")
    public Object processInstallForm(@PathParam("pkgId") String pkgId, @PathParam("formId") int formId,
            @QueryParam("source") String source) {
        PackageUpdateService pus = Framework.getService(PackageUpdateService.class);
        try {
            LocalPackage pkg = pus.getPackage(pkgId);
            Task installTask = pkg.getInstallTask();
            Form[] forms = installTask.getPackage().getInstallForms();
            if (forms == null || forms.length < formId - 1) {
                return getView("installError").arg("e",
                        new NuxeoException("No form with Id " + formId + " for package " + pkgId)).arg("source",
                        source);
            }

            Form form = forms[formId];
            FormData fdata = getContext().getForm();
            Map<String, String> params = getInstallParameters(pkgId);
            for (Field field : form.getFields()) {
                String data = fdata.getString(field.getName());
                if (data != null) {
                    params.put(field.getName(), data);
                }
                // XXX validation, and type checking ...
            }
            storeInstallParameters(pkgId, params);
            if (formId + 1 == forms.length) {
                // this was the last form screen : start the install
                return doInstall(pkgId, source);
            } else {
                return showInstallForm(pkgId, formId + 1, source);
            }
        } catch (PackageException e) {
            log.error("Error during processing Form nb " + formId, e);
            return getView("installError").arg("e", e).arg("source", source);
        }
    }

    @GET
    @Produces("text/html")
    @Path(value = "bulkRun/{pkgId}")
    public Object doBulkInstall(@PathParam("pkgId") String pkgId, @QueryParam("source") String source,
            @QueryParam("confirm") Boolean confirm) {
        if (!RequestHelper.isInternalLink(getContext())) {
            return getView("installError").arg("e",
                    new NuxeoException("Installation seems to have been started from an external link.")).arg(
                    "source", source);
        }
        PackageManager pm = Framework.getService(PackageManager.class);
        PackageUpdateService pus = Framework.getService(PackageUpdateService.class);
        try {
            DependencyResolution resolution = pm.resolveDependencies(Collections.singletonList(pkgId),
                    Collections.emptyList(), Collections.emptyList(), PlatformVersionHelper.getPlatformFilter());
            if (resolution.isFailed() && PlatformVersionHelper.getPlatformFilter() != null) {
                // retry without PF filter ...
                resolution = pm.resolveDependencies(Collections.singletonList(pkgId), Collections.emptyList(),
                        Collections.emptyList(), null);
            }
            List<String> downloadPackagesIds = resolution.getDownloadPackageIds();
            if (downloadPackagesIds.size() > 0) {
                return getView("installError").arg("e",
                        new NuxeoException("Some packages need to be downloaded before running bulk installation")).arg(
                        "source", source);
            }

            List<String> pkgIds = resolution.getOrderedPackageIdsToInstall();
            List<String> warns = new ArrayList<>();
            List<String> descs = new ArrayList<>();
            if (!pkgIds.contains(pkgId)) {
                pkgIds.add(pkgId);
            }
            List<String> rmPkgIds = new ArrayList<>();
            for (Entry<String, Version> rmEntry : resolution.getLocalPackagesToRemove().entrySet()) {
                String id = rmEntry.getKey() + "-" + rmEntry.getValue().toString();
                rmPkgIds.add(id);
            }
            for (String id : pkgIds) {
                Package pkg = pus.getPackage(id);
                if (pkg == null) {
                    return getView("installError").arg("e", new NuxeoException("Unable to find local package " + id)).arg(
                            "source", source);
                }
                String targetPlatform = PlatformVersionHelper.getPlatformFilter();
                if (!TargetPlatformFilterHelper.isCompatibleWithTargetPlatform(pkg, targetPlatform)) {
                    warns.add("Package " + id + " is not validated for your current platform: " + targetPlatform);
                }
                descs.add(pkg.getDescription());
            }
            if (Boolean.TRUE.equals(confirm)) {
                for (String id : rmPkgIds) {
                    InstallAfterRestart.addPackageForUnInstallation(id);
                }
                for (String id : pkgIds) {
                    InstallAfterRestart.addPackageForInstallation(id);
                }
                return getView("bulkInstallOnRestart").arg("pkgIds", pkgIds).arg("rmPkgIds", rmPkgIds).arg("source",
                        source);
            } else {
                return getView("bulkInstallOnRestartConfirm").arg("pkgIds", pkgIds).arg("rmPkgIds", rmPkgIds).arg(
                        "warns", warns).arg("descs", descs).arg("source", source).arg("pkgId", pkgId);
            }
        } catch (PackageException e) {
            log.error("Error during installation of " + pkgId, e);
            return getView("installError").arg("e", e).arg("source", source);
        }
    }

    @GET
    @Produces("text/html")
    @Path(value = "run/{pkgId}")
    public Object doInstall(@PathParam("pkgId") String pkgId, @QueryParam("source") String source) {
        if (!RequestHelper.isInternalLink(getContext())) {
            return getView("installError").arg("e",
                    new NuxeoException("Installation seems to have been started from an external link.")).arg(
                    "source", source);
        }
        PackageUpdateService pus = Framework.getService(PackageUpdateService.class);
        try {
            LocalPackage pkg = pus.getPackage(pkgId);
            if (InstallAfterRestart.isNeededForPackage(pkg)) {
                InstallAfterRestart.addPackageForInstallation(pkg.getId());
                return getView("installOnRestart").arg("pkg", pkg).arg("source", source);
            }
            Task installTask = pkg.getInstallTask();
            Map<String, String> params = getInstallParameters(pkgId);
            try {
                installTask.run(params);
            } catch (PackageException e) {
                log.error("Error during installation of " + pkgId, e);
                installTask.rollback();
                return getView("installError").arg("e", e).arg("source", source);
            }
            clearInstallParameters(pkgId);
            return getView("installedOK").arg("installTask", installTask).arg("pkg", pkg).arg("source", source);
        } catch (PackageException e) {
            log.error("Error during installation of " + pkgId, e);
            return getView("installError").arg("e", e).arg("source", source);
        }
    }

}
