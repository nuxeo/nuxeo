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
package org.nuxeo.connect.client.we;

import java.util.HashMap;
import java.util.Map;

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
import org.nuxeo.connect.update.LocalPackage;
import org.nuxeo.connect.update.Package;
import org.nuxeo.connect.update.PackageUpdateService;
import org.nuxeo.connect.update.ValidationStatus;
import org.nuxeo.connect.update.model.Field;
import org.nuxeo.connect.update.model.Form;
import org.nuxeo.connect.update.task.Task;
import org.nuxeo.ecm.admin.runtime.PlatformVersionHelper;
import org.nuxeo.ecm.core.api.ClientException;
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
        Map<String, String> params = (Map<String, String>) getContext().getRequest().getAttribute(
                getStorageKey(pkgId));
        if (params == null) {
            params = new HashMap<String, String>();
        }
        return params;
    }

    protected void storeInstallParameters(String pkgId,
            Map<String, String> params) {
        getContext().getRequest().setAttribute(getStorageKey(pkgId), params);
    }

    protected void clearInstallParameters(String pkgId) {
        getContext().getRequest().setAttribute(getStorageKey(pkgId), null);
    }

    @GET
    @Produces("text/html")
    @Path(value = "showTermsAndConditions/{pkgId}")
    public Object showTermsAndConditions(@PathParam("pkgId") String pkgId,@QueryParam("source") String source,
            @QueryParam("depCheck") Boolean depCheck) {
        if (depCheck==null) {
            depCheck=true;
        }
        try {
            PackageUpdateService pus = Framework.getLocalService(PackageUpdateService.class);
            LocalPackage pkg = pus.getPackage(pkgId);
            String content = pkg.getTermsAndConditionsContent();
            return getView("termsAndConditions").arg("pkg", pkg).arg("source", source).arg("content", content).arg("depCheck", depCheck);
        }
        catch (Exception e) {
            log.error("Error during terms and conditions phase ", e);
            return getView("installError").arg("e", e).arg("source", source);
        }
    }

    @GET
    @Produces("text/html")
    @Path(value = "start/{pkgId}")
    public Object startInstall(@PathParam("pkgId") String pkgId,
            @QueryParam("source") String source,@QueryParam("tacAccepted") Boolean acceptedTAC,
            @QueryParam("depCheck") Boolean depCheck) {

        try {
            PackageUpdateService pus = Framework.getLocalService(PackageUpdateService.class);
            LocalPackage pkg = pus.getPackage(pkgId);
            if (pkg==null) {
                throw new ClientException("Can not find package " + pkgId);
            }

//            if (InstallAfterRestart.isNeededForPackage(pkg)) {
//                InstallAfterRestart.addPackage(pkg.getId());
//                return getView("installOnRestart").arg(
//                        "pkg", pkg).arg("source", source);
//            }

            if (pkg.requireTermsAndConditionsAcceptance() && !Boolean.TRUE.equals(acceptedTAC)) {
                return showTermsAndConditions(pkgId, source, depCheck);
            }
            if (!Boolean.FALSE.equals(depCheck)) {
                // check deps requirements
                if (pkg.getDependencies()!=null && pkg.getDependencies().length>0) {
                    PackageManager pm = Framework.getLocalService(PackageManager.class);
                    DependencyResolution resolution = pm.resolveDependencies(pkgId, PlatformVersionHelper.getPlatformFilter());
                    if (resolution.isFailed() && PlatformVersionHelper.getPlatformFilter()!=null) {
                        // retry without PF filter ...
                        resolution = pm.resolveDependencies(pkgId, null);
                    }
                    if (resolution.isFailed()) {
                        return getView("dependencyError").arg("resolution", resolution).arg(
                                "pkg", pkg).arg("source", source);
                    } else {
                        if (resolution.requireChanges()) {
                            return getView("displayDependencies").arg("resolution", resolution).arg(
                                    "pkg", pkg).arg("source", source);
                        }
                        // no dep changes => can continue standard install process
                    }
                }
            }

            Task installTask = pkg.getInstallTask();
            ValidationStatus status = installTask.validate();

            if (!PlatformVersionHelper.isCompatible(pkg.getTargetPlatforms())) {
                status.addWarning("This package is not validated for you current platform");
            }

            if (status.hasErrors()) {
                return getView("canNotInstall").arg("status", status).arg(
                        "pkg", pkg).arg("source", source);
            }

            boolean needWizard = false;

            Form[] forms = installTask.getPackage().getInstallForms();
            if (forms != null && forms.length > 0) {
                needWizard = true;
            }
            return getView("startInstall").arg("status", status).arg(
                    "needWizard", needWizard).arg("installTask", installTask).arg(
                    "pkg", pkg).arg("source", source);
        } catch (Exception e) {
            log.error("Error during first step of installation", e);
            return getView("installError").arg("e", e).arg("source", source);
        }
    }

    @GET
    @Produces("text/html")
    @Path(value = "form/{pkgId}/{formId}")
    public Object showInstallForm(@PathParam("pkgId") String pkgId,
            @PathParam("formId") int formId, @QueryParam("source") String source) {

        PackageUpdateService pus = Framework.getLocalService(PackageUpdateService.class);

        try {
            LocalPackage pkg = pus.getPackage(pkgId);
            Task installTask = pkg.getInstallTask();
            Form[] forms = installTask.getPackage().getInstallForms();

            if (forms == null || forms.length < formId - 1) {
                return getView("installError").arg(
                        "e",
                        new ClientException("No form with Id " + formId
                                + " for package " + pkgId)).arg("source",
                        source);
            }

            return getView("showInstallForm").arg("form", forms[formId]).arg(
                    "pkg", pkg).arg("source", source).arg("step", formId + 1).arg(
                    "steps", forms.length);

        } catch (Exception e) {
            log.error("Error during displaying Form nb " + formId, e);
            return getView("installError").arg("e", e).arg("source", source);
        }
    }

    @POST
    @Produces("text/html")
    @Path(value = "form/{pkgId}/{formId}")
    public Object processInstallForm(@PathParam("pkgId") String pkgId,
            @PathParam("formId") int formId, @QueryParam("source") String source) {

        PackageUpdateService pus = Framework.getLocalService(PackageUpdateService.class);

        try {
            LocalPackage pkg = pus.getPackage(pkgId);
            Task installTask = pkg.getInstallTask();
            Form[] forms = installTask.getPackage().getInstallForms();

            if (forms == null || forms.length < formId - 1) {
                return getView("installError").arg(
                        "e",
                        new ClientException("No form with Id " + formId
                                + " for package " + pkgId)).arg("source",
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
        } catch (Exception e) {
            log.error("Error during processing Form nb " + formId, e);
            return getView("installError").arg("e", e).arg("source", source);
        }
    }

    @GET
    @Produces("text/html")
    @Path(value = "run/{pkgId}")
    public Object doInstall(@PathParam("pkgId") String pkgId,
            @QueryParam("source") String source) {

        if (!RequestHelper.isInternalLink(getContext())) {
            return getView("installError")
                    .arg("e", new ClientException("Installation seems to have been started from an external link."))
                    .arg("source", source);
        }
        PackageUpdateService pus = Framework.getLocalService(PackageUpdateService.class);

        try {
            LocalPackage pkg = pus.getPackage(pkgId);

            if (InstallAfterRestart.isNeededForPackage(pkg)) {
                InstallAfterRestart.addPackage(pkg.getId());
                return getView("installOnRestart").arg(
                        "pkg", pkg).arg("source", source);
            }

            Task installTask = pkg.getInstallTask();
            Map<String, String> params = getInstallParameters(pkgId);
            try {
                installTask.run(params);
            } catch (Throwable e) {
                log.error("Error during installation of " + pkgId, e);
                installTask.rollback();
                return getView("installError").arg("e", e).arg("source", source);
            }
            clearInstallParameters(pkgId);
            return getView("installedOK").arg("installTask", installTask).arg(
                    "pkg", pkg).arg("source", source);

        } catch (Exception e) {
            log.error("Error during installation of " + pkgId, e);
            return getView("installError").arg("e", e).arg("source", source);
        }
    }

}
