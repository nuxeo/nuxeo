/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 */
package org.nuxeo.connect.client.we;

import java.util.Arrays;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.nuxeo.connect.connector.ConnectServerError;
import org.nuxeo.connect.data.DownloadablePackage;
import org.nuxeo.connect.data.DownloadingPackage;
import org.nuxeo.connect.downloads.ConnectDownloadManager;
import org.nuxeo.connect.packages.PackageManager;
import org.nuxeo.connect.update.PackageState;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;

/**
 * Provides REST binding for {@link org.nuxeo.connect.update.Package} download
 * management.
 *
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 */
@WebObject(type = "downloadHandler")
public class DownloadHandler extends DefaultObject {

    protected static final Log log = LogFactory.getLog(DownloadHandler.class);

    @GET
    @Produces("text/plain")
    @Path(value = "progress/{pkgId}")
    public String getDownloadProgress(@PathParam("pkgId") String pkgId) {
        DownloadingPackage pkg = getDownloadingPackage(pkgId);
        if (pkg == null) {
            return null;
        }
        return pkg.getDownloadProgress() + "";
    }

    @GET
    @Produces("application/json")
    @Path(value = "progressAsJSON")
    public String getDownloadsProgress() {
        ConnectDownloadManager cdm = Framework.getLocalService(ConnectDownloadManager.class);
        List<DownloadingPackage> pkgs = cdm.listDownloadingPackages();
        StringBuffer sb = new StringBuffer();
        sb.append("[");
        for (int i = 0; i < pkgs.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append("{ \"pkgid\" : ");
            sb.append("\"" + pkgs.get(i).getId() + "\",");
            sb.append(" \"progress\" : ");
            sb.append(pkgs.get(i).getDownloadProgress() + "}");
        }
        sb.append("]");
        return sb.toString();
    }

    @GET
    @Produces("text/html")
    @Path(value = "progressPage/{pkgId}")
    public Object getDownloadProgressPage(@PathParam("pkgId") String pkgId,
            @QueryParam("source") String source,
            @QueryParam("install") Boolean install,
            @QueryParam("depCheck") Boolean depCheck,
            @QueryParam("type") String pkgType,
            @QueryParam("onlyRemote") Boolean onlyRemote,
            @QueryParam("filterOnPlatform") Boolean filterOnPlatform) {
        DownloadablePackage pkg = getDownloadingPackage(pkgId);
        boolean downloadOver = false;
        // flag to start install after download
        if (install == null) {
            install = false;
        }
        if (depCheck == null) {
            depCheck = true;
        }
        if (pkg == null) {
            PackageManager pm = Framework.getLocalService(PackageManager.class);
            pkg = pm.getPackage(pkgId);
            if (pkg.getPackageState() != PackageState.DOWNLOADING) {
                downloadOver = true;
            }
        }
        return getView("downloadStarted").arg("pkg", pkg).arg("source", source).arg(
                "over", downloadOver).arg("install", install).arg("depCheck",
                depCheck).arg("filterOnPlatform", filterOnPlatform.toString()).arg(
                "type", pkgType.toString()).arg("onlyRemote",
                onlyRemote.toString());
    }

    protected DownloadingPackage getDownloadingPackage(String pkgId) {
        ConnectDownloadManager cdm = Framework.getLocalService(ConnectDownloadManager.class);
        List<DownloadingPackage> pkgs = cdm.listDownloadingPackages();
        for (DownloadingPackage pkg : pkgs) {
            if (pkg.getId().equals(pkgId)) {
                return pkg;
            }
        }
        return null;
    }

    @GET
    @Produces("text/html")
    @Path(value = "start/{pkgId}")
    public Object startDownload(@PathParam("pkgId") String pkgId,
            @QueryParam("source") String source,
            @QueryParam("install") Boolean install,
            @QueryParam("depCheck") Boolean depCheck,
            @QueryParam("type") String pkgType,
            @QueryParam("onlyRemote") Boolean onlyRemote,
            @QueryParam("filterOnPlatform") Boolean filterOnPlatform) {
        PackageManager pm = Framework.getLocalService(PackageManager.class);
        // flag to start install after download
        if (install == null) {
            install = false;
        }
        if (depCheck == null) {
            depCheck = true;
        }
        if (!RequestHelper.isInternalLink(getContext())) {
            DownloadablePackage pkg = pm.getPackage(pkgId);
            return getView("confirmDownload").arg("pkg", pkg).arg("source",
                    source);
        }
        try {
            pm.download(pkgId);
        } catch (ConnectServerError e) {
            return getView("downloadError").arg("e", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return getView("downloadStarted").arg("pkg",
                getDownloadingPackage(pkgId)).arg("source", source).arg("over",
                false).arg("install", install).arg("depCheck", depCheck).arg(
                "filterOnPlatform", filterOnPlatform.toString()).arg("type",
                pkgType.toString()).arg("onlyRemote", onlyRemote.toString());
    }

    @GET
    @Produces("application/json")
    @Path(value = "startDownloads")
    public String startDownloads(@QueryParam("pkgList") String pkgList) {
        if (RequestHelper.isInternalLink(getContext())) {
            if (pkgList != null) {
                String[] pkgs = pkgList.split("/");
                PackageManager pm = Framework.getLocalService(PackageManager.class);
                try {
                    log.info("Starting download for packages "
                            + Arrays.toString(pkgs));
                    pm.download(Arrays.asList(pkgs));
                } catch (ConnectServerError e) {
                    log.error(e, e);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                // here we generate a fake progress report so that if some
                // download are very fast, they will still be visible on the
                // client side
                StringBuffer sb = new StringBuffer();
                sb.append("[");
                for (int i = 0; i < pkgs.length; i++) {
                    if (i > 0) {
                        sb.append(",");
                    }
                    sb.append("{ \"pkgid\" : ");
                    sb.append("\"" + pkgs[i] + "\",");
                    sb.append(" \"progress\" : 0}");
                }
                sb.append("]");
                return sb.toString();
            }
        }
        return "[]";
    }

    /**
     * @since 5.6
     */
    @GET
    @Path(value = "cancel/{pkgId}")
    public Object cancelDownload(@PathParam("pkgId") String pkgId,
            @QueryParam("source") String source) {
        PackageManager pm = Framework.getLocalService(PackageManager.class);
        pm.cancelDownload(pkgId);
        return redirect(getPrevious().getPath() + "/packages/" + source);
    }

}
