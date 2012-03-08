/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     tdelprat, jcarsique
 *
 */

package org.nuxeo.wizard;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.launcher.commons.DatabaseDriverException;
import org.nuxeo.launcher.config.ConfigurationException;
import org.nuxeo.launcher.config.ConfigurationGenerator;
import org.nuxeo.wizard.context.Context;
import org.nuxeo.wizard.context.ParamCollector;
import org.nuxeo.wizard.download.DownloadablePackageOptions;
import org.nuxeo.wizard.download.PackageDownloader;
import org.nuxeo.wizard.helpers.ConnectRegistrationHelper;
import org.nuxeo.wizard.helpers.IPValidator;
import org.nuxeo.wizard.helpers.NumberValidator;
import org.nuxeo.wizard.helpers.PackageDownloaderHelper;
import org.nuxeo.wizard.nav.Page;
import org.nuxeo.wizard.nav.SimpleNavigationHandler;

/**
 * Main entry point : find the right handler and start jsp rendering
 *
 * @author Tiry (tdelprat@nuxeo.com)
 * @since 5.4.2
 */
public class RouterServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    protected static Log log = LogFactory.getLog(RouterServlet.class);

    protected SimpleNavigationHandler navHandler = SimpleNavigationHandler.instance();

    public static final String CONNECT_TOKEN_KEY = "ConnectRegistrationToken";

    protected String getAction(HttpServletRequest req) {
        String uri = req.getRequestURI();

        int idx = uri.indexOf("?");
        if (idx > 0) {
            uri = uri.substring(0, idx - 1);
        }
        String action = uri.replace(req.getContextPath() + "/router/", "");
        if (action.startsWith("/")) {
            action = action.substring(1);
        }
        return action;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // process action
        handleAction(getAction(req), req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // store posted data
        Context.instance(req).getCollector().collectConfigurationParams(req);

        doGet(req, resp);
    }

    protected Method findhandler(Page currentPage, String verb) {

        String methodName = "handle" + currentPage.getAction() + verb;
        Method method = null;
        try {
            method = this.getClass().getMethod(methodName, Page.class,
                    HttpServletRequest.class, HttpServletResponse.class);
        } catch (Exception e) {
            // fall back to default Handler lookup
            methodName = "handleDefault" + verb;
            try {
                method = this.getClass().getMethod(methodName, Page.class,
                        HttpServletRequest.class, HttpServletResponse.class);
            } catch (Exception e2) {
                log.error("Unable to resolve default handler for " + verb, e);
            }
        }
        return method;
    }

    protected void handleAction(String action, HttpServletRequest req,
            HttpServletResponse resp) throws ServletException, IOException {

        // locate page
        Page currentPage = navHandler.getCurrentPage(action);
        if (currentPage == null) {
            resp.sendError(404, "Action " + action + " is not supported");
            return;
        }

        // find action handler
        Method handler = findhandler(currentPage, req.getMethod());
        if (handler == null) {
            resp.sendError(500, "No handler found for " + action);
            return;
        }

        // execute handler => triggers rendering
        try {
            handler.invoke(this, currentPage, req, resp);
        } catch (Exception e) {
            log.error("Error during handler execution", e);
            req.setAttribute("error", e);
            req.getRequestDispatcher("/error.jsp").forward(req, resp);
        }
    }

    // default handlers

    public void handleDefaultGET(Page currentPage, HttpServletRequest req,
            HttpServletResponse resp) throws ServletException, IOException {
        currentPage.dispatchToJSP(req, resp);
    }

    public void handleDefaultPOST(Page currentPage, HttpServletRequest req,
            HttpServletResponse resp) throws ServletException, IOException {
        // XXX validate data
        currentPage.next().dispatchToJSP(req, resp, true);
    }

    // custom handlers

    public void handleConnectGET(Page currentPage, HttpServletRequest req,
            HttpServletResponse resp) throws ServletException, IOException {

        Context ctx = Context.instance(req);

        // compute CB url
        String cbUrl = req.getRequestURL().toString();
        cbUrl = cbUrl.replace("/router/" + currentPage.getAction(),
                "/ConnectCallback?cb=yes");
        // In order to avoid any issue with badly configured reverse proxies
        // => get url from the client side
        if (ctx.getBaseUrl() != null) {
            cbUrl = ctx.getBaseUrl() + "ConnectCallback?cb=yes";
        }
        cbUrl = URLEncoder.encode(cbUrl, "UTF-8");

        req.setAttribute("callBackUrl", cbUrl);

        handleDefaultGET(currentPage, req, resp);
    }

    public void handleConnectCallbackGET(Page currentPage,
            HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String token = req.getParameter(CONNECT_TOKEN_KEY);
        String action = req.getParameter("action");
        String targetNav = null;

        if (action == null || action.isEmpty()) {
            action = "skip";
        }
        if (action.equals("register") && (token == null || token.isEmpty())) {
            action = "skip";
        }

        if ("register".equals(action)) {
            // store the registration info
            Map<String, String> connectMap = new HashMap<String, String>();
            if (token != null) {
                String tokenData = new String(Base64.decodeBase64(token));
                String[] tokenDataLines = tokenData.split("\n");
                for (String line : tokenDataLines) {
                    String[] parts = line.split(":");
                    if (parts.length > 1) {
                        connectMap.put(parts[0], parts[1]);
                    }
                }
                Context.instance(req).storeConnectMap(connectMap);
            }

            // deactivate the confirm form
            SimpleNavigationHandler.instance().deactivatePage("ConnectFinish");
            // go to the next page
            targetNav = currentPage.next().getAction();

        } else if ("skip".equals(action)) {
            // activate the confirm form
            SimpleNavigationHandler.instance().activatePage("ConnectFinish");
            // go to it
            targetNav = currentPage.next().getAction();
        } else if ("prev".equals(action)) {
            targetNav = currentPage.prev().prev().getAction();
        }

        String targetUrl = req.getContextPath() + "/" + targetNav;

        req.setAttribute("targetUrl", targetUrl);
        handleDefaultGET(currentPage, req, resp);
    }

    public void handleConnectFinishGET(Page currentPage,
            HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // get the connect Token and decode associated infos
        String token = req.getParameter(CONNECT_TOKEN_KEY);
        Map<String, String> connectMap = new HashMap<String, String>();
        if (token != null) {
            String tokenData = new String(Base64.decodeBase64(token));
            String[] tokenDataLines = tokenData.split("\n");
            for (String line : tokenDataLines) {
                String[] parts = line.split(":");
                if (parts.length > 1) {
                    connectMap.put(parts[0], parts[1]);
                }
            }
            Context.instance(req).storeConnectMap(connectMap);
        }

        handleDefaultGET(currentPage, req, resp);
    }

    public void handleDBPOST(Page currentPage, HttpServletRequest req,
            HttpServletResponse resp) throws ServletException, IOException {

        Context ctx = Context.instance(req);
        ParamCollector collector = ctx.getCollector();

        if ("true".equals(req.getParameter("refresh"))) {
            String templateName = collector.getConfigurationParam(ConfigurationGenerator.PARAM_TEMPLATE_DBNAME);
            collector.changeDBTemplate(templateName);
            currentPage.dispatchToJSP(req, resp);
            return;
        }

        if (!collector.getConfigurationParam(
                ConfigurationGenerator.PARAM_TEMPLATE_DBNAME).equals("default")) {
            if (collector.getConfigurationParam("nuxeo.db.name").isEmpty()) {
                ctx.trackError("nuxeo.db.name", "error.dbname.required");
            }
            if (collector.getConfigurationParam("nuxeo.db.user").isEmpty()) {
                ctx.trackError("nuxeo.db.user", "error.dbuser.required");
            }
            if (collector.getConfigurationParam("nuxeo.db.password").isEmpty()) {
                ctx.trackError("nuxeo.db.password", "error.dbpassword.required");
            }
            if (collector.getConfigurationParam("nuxeo.db.host").isEmpty()) {
                ctx.trackError("nuxeo.db.host", "error.dbhost.required");
            }
            if (collector.getConfigurationParam("nuxeo.db.port").isEmpty()) {
                ctx.trackError("nuxeo.db.port", "error.dbport.required");
            } else {
                if (!NumberValidator.validate(collector.getConfigurationParam("nuxeo.db.port"))) {
                    ctx.trackError("nuxeo.db.port", "error.invalid.port");
                } else {
                    int dbPort = Integer.parseInt(collector.getConfigurationParam("nuxeo.db.port"));
                    if (dbPort < 1024 || dbPort > 65536) {
                        ctx.trackError("nuxeo.db.port", "error.invalid.port");
                    }
                }
            }
            ConfigurationGenerator cg = collector.getConfigurationGenerator();
            try {
                cg.checkDatabaseConnection(
                        collector.getConfigurationParam(ConfigurationGenerator.PARAM_TEMPLATE_DBNAME),
                        collector.getConfigurationParam("nuxeo.db.name"),
                        collector.getConfigurationParam("nuxeo.db.user"),
                        collector.getConfigurationParam("nuxeo.db.password"),
                        collector.getConfigurationParam("nuxeo.db.host"),
                        collector.getConfigurationParam("nuxeo.db.port"));
            } catch (DatabaseDriverException e) {
                ctx.trackError("nuxeo.db.name", "error.db.driver.notfound");
                log.warn(e);
            } catch (SQLException e) {
                ctx.trackError("nuxeo.db.name", "error.db.connection");
                log.warn(e);
            }
        }

        if (ctx.hasErrors()) {
            currentPage.dispatchToJSP(req, resp);
        } else {
            currentPage.next().dispatchToJSP(req, resp, true);
        }

    }

    public void handleSmtpPOST(Page currentPage, HttpServletRequest req,
            HttpServletResponse resp) throws ServletException, IOException {

        Context ctx = Context.instance(req);
        ParamCollector collector = ctx.getCollector();

        if (collector.getConfigurationParam("mail.smtp.auth").equals("true")) {
            if (collector.getConfigurationParam("mail.smtp.username").isEmpty()) {
                ctx.trackError("mail.smtp.username",
                        "error.mail.smtp.username.required");
            }
            if (collector.getConfigurationParam("mail.smtp.password").isEmpty()) {
                ctx.trackError("mail.smtp.password",
                        "error.mail.smtp.password.required");
            }
        }

        if (!collector.getConfigurationParam("mail.smtp.port").isEmpty()) {
            if (!NumberValidator.validate(collector.getConfigurationParam("mail.smtp.port"))) {
                ctx.trackError("mail.smtp.port",
                        "error.mail.smtp.port.mustbeanumber");
            }
        }

        if (ctx.hasErrors()) {
            currentPage.dispatchToJSP(req, resp);
        } else {
            currentPage.next().dispatchToJSP(req, resp, true);
        }

    }

    public void handleRecapPOST(Page currentPage, HttpServletRequest req,
            HttpServletResponse resp) throws ServletException, IOException {

        Context ctx = Context.instance(req);
        ParamCollector collector = ctx.getCollector();
        ConfigurationGenerator cg = collector.getConfigurationGenerator();

        if (ctx.isConnectRegistrationDone()) {
            // save Connect registration
            ConnectRegistrationHelper.saveConnectRegistrationFile(ctx);
        }

        // Mark package selection done
        PackageDownloaderHelper.markPackageSelectionDone(ctx);

        Map<String, String> changedParameters = collector.getConfigurationParams();
        changedParameters.put(ConfigurationGenerator.PARAM_WIZARD_DONE, "true");
        try {
            // save config
            cg.saveFilteredConfiguration(changedParameters);

            // // => page will trigger the restart
            // new Page("", "reStarting.jsp").dispatchToJSP(req, resp);
            currentPage.next().dispatchToJSP(req, resp, true);
        } catch (ConfigurationException e) {
            log.error("Could not save wizard parameters.", e);
            currentPage.dispatchToJSP(req, resp);
        }
    }

    public void handleGeneralPOST(Page currentPage, HttpServletRequest req,
            HttpServletResponse resp) throws ServletException, IOException {

        Context ctx = Context.instance(req);
        ParamCollector collector = ctx.getCollector();
        String bindAddress = collector.getConfigurationParamValue("nuxeo.bind.address");
        if (bindAddress != null && !bindAddress.isEmpty()) {
            if (!IPValidator.validate(bindAddress)) {
                ctx.trackError("nuxeo.bind.address", "error.invalid.ip");
            }
            try {
                InetAddress inetAddress = InetAddress.getByName(bindAddress);
                ConfigurationGenerator.checkAddressReachable(inetAddress);
            } catch (UnknownHostException e) {
                ctx.trackError("nuxeo.bind.address", "error.invalid.ip");
            } catch (ConfigurationException e) {
                ctx.trackError("nuxeo.bind.address", "error.already.used.ip");
            }
        }

        if (ctx.hasErrors()) {
            currentPage.dispatchToJSP(req, resp);
        } else {
            currentPage.next().dispatchToJSP(req, resp, true);
        }
    }

    public void handleHomeGET(Page currentPage, HttpServletRequest req,
            HttpServletResponse resp) throws ServletException, IOException {

        Context ctx = Context.instance(req);
        if (PackageDownloaderHelper.isPackageSelectionDone(ctx)) {
            navHandler.deactivatePage("PackagesSelection");
            navHandler.deactivatePage("PackagesDownload");
            navHandler.activatePage("PackagesSelectionDone");
        }

        handleDefaultGET(currentPage, req, resp);
    }

    public void handleHomePOST(Page currentPage, HttpServletRequest req,
            HttpServletResponse resp) throws ServletException, IOException {

        String baseUrl = req.getParameter("baseUrl");
        if (baseUrl != null && !baseUrl.isEmpty()) {
            if (baseUrl.endsWith("Home")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 4);
                Context.instance(req).setBaseUrl(baseUrl);
            }
        }

        String browserInternetAccess = req.getParameter("browserInternetAccess");
        if ("true".equals(browserInternetAccess)) {
            Context.instance(req).setBrowserInternetAccess(true);
            SimpleNavigationHandler.instance().deactivatePage("NetworkBlocked");
            SimpleNavigationHandler.instance().activatePage("Connect");
        } else {
            Context.instance(req).setBrowserInternetAccess(false);
            SimpleNavigationHandler.instance().activatePage("NetworkBlocked");
            SimpleNavigationHandler.instance().deactivatePage("Connect");
        }

        currentPage.next().dispatchToJSP(req, resp, true);
    }

    public void handleProxyPOST(Page currentPage, HttpServletRequest req,
            HttpServletResponse resp) throws ServletException, IOException {

        Context ctx = Context.instance(req);
        ParamCollector collector = ctx.getCollector();
        String proxyType = collector.getConfigurationParamValue("nuxeo.http.proxy.type");
        if ("none".equals(proxyType)) {
            collector.addConfigurationParam("nuxeo.http.proxy.type", null);
            collector.addConfigurationParam("nuxeo.http.proxy.login", null);
            collector.addConfigurationParam("nuxeo.http.proxy.password", null);
            collector.addConfigurationParam("nuxeo.http.proxy.host", null);
            collector.addConfigurationParam("nuxeo.http.proxy.port", null);
            collector.addConfigurationParam("nuxeo.http.proxy.ntml.host", null);
            collector.addConfigurationParam("nuxeo.http.proxy.ntml.domain",
                    null);
            if (!PackageDownloaderHelper.isPackageSelectionDone(ctx)) {
                PackageDownloader.instance().setProxy(null, 0, null, null,
                        null, null);
            }
        } else {
            if (!NumberValidator.validate(collector.getConfigurationParam("nuxeo.http.proxy.port"))) {
                ctx.trackError("nuxeo.http.proxy.port",
                        "error.nuxeo.http.proxy.port");
            }
            if (collector.getConfigurationParam("nuxeo.http.proxy.host").isEmpty()) {
                ctx.trackError("nuxeo.http.proxy.host",
                        "error.nuxeo.http.proxy.emptyHost");
            }
            if ("anonymous".equals(proxyType)) {
                collector.addConfigurationParam("nuxeo.http.proxy.login", null);
                collector.addConfigurationParam("nuxeo.http.proxy.password",
                        null);
                collector.addConfigurationParam("nuxeo.http.proxy.ntml.host",
                        null);
                collector.addConfigurationParam("nuxeo.http.proxy.ntml.domain",
                        null);

                if (!ctx.hasErrors()) {
                    if (!PackageDownloaderHelper.isPackageSelectionDone(ctx)) {
                        PackageDownloader.instance().setProxy(
                                collector.getConfigurationParamValue("nuxeo.http.proxy.host"),
                                Integer.parseInt(collector.getConfigurationParamValue("nuxeo.http.proxy.port")),
                                null, null, null, null);
                    }
                }
            } else {
                if (collector.getConfigurationParam("nuxeo.http.proxy.login").isEmpty()) {
                    ctx.trackError("nuxeo.http.proxy.login",
                            "error.nuxeo.http.proxy.emptyLogin");
                } else {
                    if (!ctx.hasErrors()) {
                        if (!PackageDownloaderHelper.isPackageSelectionDone(ctx)) {
                            PackageDownloader.instance().setProxy(
                                    collector.getConfigurationParamValue("nuxeo.http.proxy.host"),
                                    Integer.parseInt(collector.getConfigurationParamValue("nuxeo.http.proxy.port")),
                                    collector.getConfigurationParamValue("nuxeo.http.proxy.login"),
                                    collector.getConfigurationParamValue("nuxeo.http.proxy.password"),
                                    collector.getConfigurationParamValue("nuxeo.http.proxy.ntlm.host"),
                                    collector.getConfigurationParamValue("nuxeo.http.proxy.ntml.domain"));
                        }
                    }
                }
            }
        }

        if (ctx.hasErrors()) {
            currentPage.dispatchToJSP(req, resp);
        } else {
            currentPage.next().dispatchToJSP(req, resp, true);
        }
    }

    public void handleResetGET(Page currentPage, HttpServletRequest req,
            HttpServletResponse resp) throws IOException {

        // reset
        Context.reset();
        SimpleNavigationHandler.reset();
        PackageDownloader.reset();

        // return to first page
        String target = "/"
                + req.getContextPath()
                + "/"
                + SimpleNavigationHandler.instance().getDefaultPage().getAction();
        if (target.startsWith("//")) {
            target = target.substring(1);
        }
        resp.sendRedirect(target);
    }

    public void handlePackageOptionsResourceGET(Page currentPage,
            HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        DownloadablePackageOptions options = PackageDownloader.instance().getPackageOptions();
        resp.setContentType("text/json");
        resp.getWriter().write(options.asJson());

    }

    public void handlePackagesSelectionGET(Page currentPage,
            HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        handleDefaultGET(currentPage, req, resp);
    }

    public void handlePackagesSelectionPOST(Page currentPage,
            HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        List<String> options = new ArrayList<String>();
        @SuppressWarnings("unchecked")
        Enumeration<String> params = req.getParameterNames();
        while (params.hasMoreElements()) {
            String p = params.nextElement();
            if ("on".equals(req.getParameter(p))) {
                options.add(p);
            }
        }

        PackageDownloader.instance().selectOptions(options);

        currentPage.next().dispatchToJSP(req, resp, true);
    }

    public void handlePackagesDownloadGET(Page currentPage,
            HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if ("true".equals(req.getParameter("startDownload"))) {
            PackageDownloader.instance().startDownload();
        } else if (req.getParameter("reStartDownload") != null) {
            PackageDownloader.instance().reStartDownload(
                    req.getParameter("reStartDownload"));
        }
        currentPage.dispatchToJSP(req, resp);
    }

    public void handlePackagesDownloadPOST(Page currentPage,
            HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        ParamCollector collector = Context.instance(req).getCollector();

        String installationFilePath = new File(
                collector.getConfigurationParam(org.nuxeo.common.Environment.NUXEO_DATA_DIR),
                ConfigurationGenerator.INSTALL_AFTER_RESTART).getAbsolutePath();

        PackageDownloader.instance().scheduleDownloadedPackagesForInstallation(
                installationFilePath);
        PackageDownloader.reset();

        currentPage.next().dispatchToJSP(req, resp, true);
    }

}
