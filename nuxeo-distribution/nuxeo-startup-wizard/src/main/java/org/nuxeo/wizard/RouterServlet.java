/*
 * (C) Copyright 2011-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     tdelprat, jcarsique
 *
 */

package org.nuxeo.wizard;

import static org.nuxeo.common.Environment.NUXEO_DATA_DIR;
import static org.nuxeo.launcher.config.ConfigurationGenerator.DB_EXCLUDE_CHECK_LIST;
import static org.nuxeo.launcher.config.ConfigurationGenerator.INSTALL_AFTER_RESTART;
import static org.nuxeo.launcher.config.ConfigurationGenerator.PARAM_BIND_ADDRESS;
import static org.nuxeo.launcher.config.ConfigurationGenerator.PARAM_DB_HOST;
import static org.nuxeo.launcher.config.ConfigurationGenerator.PARAM_DB_NAME;
import static org.nuxeo.launcher.config.ConfigurationGenerator.PARAM_DB_PORT;
import static org.nuxeo.launcher.config.ConfigurationGenerator.PARAM_DB_PWD;
import static org.nuxeo.launcher.config.ConfigurationGenerator.PARAM_DB_USER;
import static org.nuxeo.launcher.config.ConfigurationGenerator.PARAM_MONGODB_NAME;
import static org.nuxeo.launcher.config.ConfigurationGenerator.PARAM_MONGODB_SERVER;
import static org.nuxeo.launcher.config.ConfigurationGenerator.PARAM_TEMPLATE_DBNAME;
import static org.nuxeo.launcher.config.ConfigurationGenerator.PARAM_TEMPLATE_DBSECONDARY_NAME;
import static org.nuxeo.launcher.config.ConfigurationGenerator.PARAM_WIZARD_DONE;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.naming.AuthenticationException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
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

    protected final SimpleNavigationHandler navHandler = SimpleNavigationHandler.instance();

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
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // process action
        handleAction(getAction(req), req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // store posted data
        req.setCharacterEncoding("UTF-8");
        Context.instance(req).getCollector().collectConfigurationParams(req);
        doGet(req, resp);
    }

    protected Method findhandler(Page currentPage, String verb) {
        String methodName = "handle" + currentPage.getAction() + verb;
        Method method = null;
        try {
            method = this.getClass().getMethod(methodName, Page.class, HttpServletRequest.class,
                    HttpServletResponse.class);
        } catch (Exception e) {
            // fall back to default Handler lookup
            methodName = "handleDefault" + verb;
            try {
                method = this.getClass().getMethod(methodName, Page.class, HttpServletRequest.class,
                        HttpServletResponse.class);
            } catch (Exception e2) {
                log.error("Unable to resolve default handler for " + verb, e);
            }
        }
        return method;
    }

    protected void handleAction(String action, HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
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

    public void handleDefaultGET(Page currentPage, HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        currentPage.dispatchToJSP(req, resp);
    }

    public void handleDefaultPOST(Page currentPage, HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // XXX validate data
        currentPage.next().dispatchToJSP(req, resp, true);
    }

    // custom handlers

    public void handleConnectGET(Page currentPage, HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setAttribute("popupUrl", req.getContextPath() + "/ConnectCallback?action=display");
        handleDefaultGET(currentPage, req, resp);
    }

    public void handleConnectCallbackGET(Page currentPage, HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Context ctx = Context.instance(req);
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
            Map<String, String> connectMap = new HashMap<>();
            Context context = Context.instance(req);
            if (token != null) {
                String tokenData = new String(Base64.decodeBase64(token));
                String[] tokenDataLines = tokenData.split("\n");
                for (String line : tokenDataLines) {
                    String[] parts = line.split(":");
                    if (parts.length > 1) {
                        connectMap.put(parts[0], parts[1]);
                    }
                }
                context.storeConnectMap(connectMap);
            }

            // Save CLID
            if (context.isConnectRegistrationDone()) {
                // save Connect registration
                ConnectRegistrationHelper.saveConnectRegistrationFile(context);
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
        } else if ("display".equals(action)) {
            // compute CB url
            String cbUrl = req.getRequestURL().toString();
            cbUrl = cbUrl.replace("/router/" + currentPage.getAction(), "/ConnectCallback?cb=yes");
            // In order to avoid any issue with badly configured reverse proxies
            // => get url from the client side
            if (ctx.getBaseUrl() != null) {
                cbUrl = ctx.getBaseUrl() + "ConnectCallback?cb=yes";
            }
            cbUrl = URLEncoder.encode(cbUrl, "UTF-8");

            String redirect = ctx.getCollector().getConfigurationParam("org.nuxeo.connect.url",
                    "https://connect.nuxeo.com/nuxeo/site/") + "../../register/#/embedded?wizardCallbackUrl=" + cbUrl
                    + "&pkg=" + ctx.getDistributionKey();

            resp.sendRedirect(redirect);
            return;
        }

        String targetUrl = req.getContextPath() + "/" + targetNav;
        req.setAttribute("targetUrl", targetUrl);
        handleDefaultGET(currentPage, req, resp);
    }

    public void handleConnectFinishGET(Page currentPage, HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // get the connect Token and decode associated infos
        String token = req.getParameter(CONNECT_TOKEN_KEY);
        Map<String, String> connectMap = new HashMap<>();
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

    public void handleDBPOST(Page currentPage, HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Context ctx = Context.instance(req);
        ParamCollector collector = ctx.getCollector();

        String templateDbName = collector.getConfigurationParam(PARAM_TEMPLATE_DBNAME);
        String templateDbNoSQLName = collector.getConfigurationParam(PARAM_TEMPLATE_DBSECONDARY_NAME);
        if ("true".equals(req.getParameter("refresh"))) {
            collector.changeDBTemplate(templateDbName);
            collector.changeDBTemplate(templateDbNoSQLName);
            collector.removeDbKeys();
            currentPage.dispatchToJSP(req, resp);
            return;
        }

        // Check relational database
        if (!DB_EXCLUDE_CHECK_LIST.contains(templateDbName)) {
            if (collector.getConfigurationParam(PARAM_DB_NAME).isEmpty()) {
                ctx.trackError(PARAM_DB_NAME, "error.dbname.required");
            }
            if (collector.getConfigurationParam(PARAM_DB_USER).isEmpty()) {
                ctx.trackError(PARAM_DB_USER, "error.dbuser.required");
            }
            if (collector.getConfigurationParam(PARAM_DB_PWD).isEmpty()) {
                ctx.trackError(PARAM_DB_PWD, "error.dbpassword.required");
            }
            if (collector.getConfigurationParam(PARAM_DB_HOST).isEmpty()) {
                ctx.trackError(PARAM_DB_HOST, "error.dbhost.required");
            }
            if (collector.getConfigurationParam(PARAM_DB_PORT).isEmpty()) {
                ctx.trackError(PARAM_DB_PORT, "error.dbport.required");
            } else {
                if (!NumberValidator.validate(collector.getConfigurationParam(PARAM_DB_PORT))) {
                    ctx.trackError(PARAM_DB_PORT, "error.invalid.port");
                } else {
                    int dbPort = Integer.parseInt(collector.getConfigurationParam(PARAM_DB_PORT));
                    if (dbPort < 1024 || dbPort > 65536) {
                        ctx.trackError(PARAM_DB_PORT, "error.invalid.port");
                    }
                }
            }
            ConfigurationGenerator cg = collector.getConfigurationGenerator();
            try {
                cg.checkDatabaseConnection(templateDbName, collector.getConfigurationParam(PARAM_DB_NAME),
                        collector.getConfigurationParam(PARAM_DB_USER), collector.getConfigurationParam(PARAM_DB_PWD),
                        collector.getConfigurationParam(PARAM_DB_HOST), collector.getConfigurationParam(PARAM_DB_PORT));
            } catch (DatabaseDriverException e) {
                ctx.trackError(PARAM_DB_NAME, "error.db.driver.notfound");
                log.warn(e);
            } catch (SQLException e) {
                ctx.trackError(PARAM_DB_NAME, "error.db.connection");
                log.warn(e);
            }
        }

        // Check MongoDB settings
        if ("mongodb".equals(templateDbName)) {
            if (collector.getConfigurationParam(PARAM_MONGODB_NAME).isEmpty()) {
                ctx.trackError(PARAM_MONGODB_NAME, "error.dbname.required");
            }
            if (collector.getConfigurationParam(PARAM_MONGODB_SERVER).isEmpty()) {
                ctx.trackError(PARAM_MONGODB_SERVER, "error.dburi.required");
            }
        }

        if (ctx.hasErrors()) {
            currentPage.dispatchToJSP(req, resp);
        } else {
            currentPage.next().dispatchToJSP(req, resp, true);
        }
    }

    public void handleUserPOST(Page currentPage, HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Context ctx = Context.instance(req);
        ParamCollector collector = ctx.getCollector();

        String refreshParam = req.getParameter("refresh");
        String directoryType = collector.getConfigurationParam("nuxeo.directory.type");

        if ("true".equals(refreshParam)) {
            currentPage.dispatchToJSP(req, resp);
            return;
        }

        if ("checkNetwork".equals(refreshParam) || "checkAuth".equals(refreshParam)
                || "checkUserLdapParam".equals(refreshParam) || "checkGroupLdapParam".equals(refreshParam)) {
            try {
                if ("checkNetwork".equals(refreshParam)) {
                    bindLdapConnection(collector, false);
                    ctx.trackInfo("nuxeo.ldap.url", "info.host.found");
                } else if ("checkAuth".equals(refreshParam)) {
                    bindLdapConnection(collector, true);
                    ctx.trackInfo("nuxeo.ldap.auth", "info.auth.success");
                } else {
                    DirContext dirContext = new InitialDirContext(getContextEnv(collector, true));
                    String searchScope;
                    String searchBaseDn;
                    String searchClass;
                    String searchFilter;
                    if ("checkUserLdapParam".equals(refreshParam)) {
                        searchBaseDn = collector.getConfigurationParam("nuxeo.ldap.user.searchBaseDn");
                        searchScope = collector.getConfigurationParam("nuxeo.ldap.user.searchScope");
                        searchClass = collector.getConfigurationParam("nuxeo.ldap.user.searchClass");
                        searchFilter = collector.getConfigurationParam("nuxeo.ldap.user.searchFilter");
                    } else {
                        searchBaseDn = collector.getConfigurationParam("nuxeo.ldap.group.searchBaseDn");
                        searchScope = collector.getConfigurationParam("nuxeo.ldap.group.searchScope");
                        searchFilter = collector.getConfigurationParam("nuxeo.ldap.group.searchFilter");
                        searchClass = "";
                    }

                    SearchControls scts = new SearchControls();
                    if ("onelevel".equals(searchScope)) {
                        scts.setSearchScope(SearchControls.ONELEVEL_SCOPE);
                    } else {
                        scts.setSearchScope(SearchControls.SUBTREE_SCOPE);
                    }
                    String filter = String.format("(&(%s)(objectClass=%s))",
                            searchFilter.isEmpty() ? "objectClass=*" : searchFilter,
                            searchClass.isEmpty() ? "*" : searchClass);
                    NamingEnumeration<SearchResult> results;
                    try {
                        results = dirContext.search(searchBaseDn, filter, scts);
                        if (!results.hasMore()) {
                            ctx.trackError("nuxeo.ldap.search", "error.ldap.noresult");
                        } else {
                            SearchResult result = results.next();
                            if (searchBaseDn.equalsIgnoreCase(result.getNameInNamespace()) && results.hasMore()) {
                                // try not to display the root of the search
                                // base DN
                                result = results.next();
                            }
                            ctx.trackInfo("dn", result.getNameInNamespace());
                            Attributes attributes = result.getAttributes();
                            NamingEnumeration<String> ids = attributes.getIDs();
                            String id;
                            StringBuilder sb;
                            while (ids.hasMore()) {
                                id = ids.next();
                                NamingEnumeration<?> values = attributes.get(id).getAll();
                                sb = new StringBuilder();
                                while (values.hasMore()) {
                                    sb.append(values.next()).append(" , ");
                                }
                                ctx.trackInfo(id, sb.substring(0, sb.length() - 3));
                            }
                        }
                    } catch (NameNotFoundException e) {
                        ctx.trackError("nuxeo.ldap.search", "error.ldap.searchBaseDn");
                        log.warn(e);
                    }
                    dirContext.close();
                }
            } catch (AuthenticationException e) {
                ctx.trackError("nuxeo.ldap.auth", "error.auth.failed");
                log.warn(e);
            } catch (NamingException e) {
                ctx.trackError("nuxeo.ldap.url", "error.host.not.found");
                log.warn(e);
            }
        }

        // Form submit
        if (!"default".equals(directoryType) && refreshParam.isEmpty()) {
            // first check bind to LDAP server
            try {
                bindLdapConnection(collector, true);
            } catch (NamingException e) {
                ctx.trackError("nuxeo.ldap.auth", "error.ldap.bind.failed");
                log.warn(e);
            }

            // then check mandatory fields
            if (collector.getConfigurationParam("nuxeo.ldap.user.searchBaseDn").isEmpty()) {
                ctx.trackError("nuxeo.ldap.user.searchBaseDn", "error.user.searchBaseDn.required");
            }
            if (collector.getConfigurationParam("nuxeo.ldap.user.mapping.rdn").isEmpty()) {
                ctx.trackError("nuxeo.ldap.user.mapping.rdn", "error.user.rdn.required");
            }
            if (collector.getConfigurationParam("nuxeo.ldap.user.mapping.username").isEmpty()) {
                ctx.trackError("nuxeo.ldap.user.mapping.username", "error.user.username.required");
            }
            if (collector.getConfigurationParam("nuxeo.ldap.user.mapping.password").isEmpty()) {
                ctx.trackError("nuxeo.ldap.user.mapping.password", "error.user.password.required");
            }
            if (collector.getConfigurationParam("nuxeo.ldap.user.mapping.firstname").isEmpty()) {
                ctx.trackError("nuxeo.ldap.user.mapping.firstname", "error.user.firstname.required");
            }
            if (collector.getConfigurationParam("nuxeo.ldap.user.mapping.lastname").isEmpty()) {
                ctx.trackError("nuxeo.ldap.user.mapping.lastname", "error.user.lastname.required");
            }
            String userGroupStorage = collector.getConfigurationParam("nuxeo.user.group.storage");
            if (!"userLdapOnly".equals(userGroupStorage) && !"multiUserSqlGroup".equals(userGroupStorage)) {
                if (collector.getConfigurationParam("nuxeo.ldap.group.searchBaseDn").isEmpty()) {
                    ctx.trackError("nuxeo.ldap.group.searchBaseDn", "error.group.searchBaseDn.required");
                }
                if (collector.getConfigurationParam("nuxeo.ldap.group.mapping.rdn").isEmpty()) {
                    ctx.trackError("nuxeo.ldap.group.mapping.rdn", "error.group.rdn.required");
                }
                if (collector.getConfigurationParam("nuxeo.ldap.group.mapping.name").isEmpty()) {
                    ctx.trackError("nuxeo.ldap.group.mapping.name", "error.group.name.required");
                }
            }
            if ("true".equals(collector.getConfigurationParam("nuxeo.user.emergency.enable"))) {
                if (collector.getConfigurationParam("nuxeo.user.emergency.username").isEmpty()) {
                    ctx.trackError("nuxeo.user.emergency.username", "error.emergency.username.required");
                }
                if (collector.getConfigurationParam("nuxeo.user.emergency.password").isEmpty()) {
                    ctx.trackError("nuxeo.user.emergency.password", "error.emergency.password.required");
                }
            }
        }

        if (ctx.hasErrors() || ctx.hasInfos()) {
            currentPage.dispatchToJSP(req, resp);
        } else {
            currentPage.next().dispatchToJSP(req, resp, true);
        }
    }

    private Hashtable<Object, Object> getContextEnv(ParamCollector collector, boolean checkAuth) {
        String ldapUrl = collector.getConfigurationParam("nuxeo.ldap.url");
        String ldapBindDn = collector.getConfigurationParam("nuxeo.ldap.binddn");
        String ldapBindPassword = collector.getConfigurationParam("nuxeo.ldap.bindpassword");
        ConfigurationGenerator cg = collector.getConfigurationGenerator();
        return cg.getContextEnv(ldapUrl, ldapBindDn, ldapBindPassword, checkAuth);
    }

    private void bindLdapConnection(ParamCollector collector, boolean authenticate) throws NamingException {
        ConfigurationGenerator cg = collector.getConfigurationGenerator();
        cg.checkLdapConnection(getContextEnv(collector, authenticate));
    }

    public void handleSmtpPOST(Page currentPage, HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        Context ctx = Context.instance(req);
        ParamCollector collector = ctx.getCollector();

        if (collector.getConfigurationParam("mail.transport.auth").equals("true")) {
            if (collector.getConfigurationParam("mail.transport.user").isEmpty()) {
                ctx.trackError("mail.transport.user", "error.mail.transport.user.required");
            }
            if (collector.getConfigurationParam("mail.transport.password").isEmpty()) {
                ctx.trackError("mail.transport.password", "error.mail.transport.password.required");
            }
        }

        if (!collector.getConfigurationParam("mail.transport.port").isEmpty()) {
            if (!NumberValidator.validate(collector.getConfigurationParam("mail.transport.port"))) {
                ctx.trackError("mail.transport.port", "error.mail.transport.port.mustbeanumber");
            }
        }

        if (ctx.hasErrors()) {
            currentPage.dispatchToJSP(req, resp);
        } else {
            currentPage.next().dispatchToJSP(req, resp, true);
        }
    }

    public void handleRecapPOST(Page currentPage, HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Context ctx = Context.instance(req);
        ParamCollector collector = ctx.getCollector();
        ConfigurationGenerator cg = collector.getConfigurationGenerator();

        // Mark package selection done
        PackageDownloaderHelper.markPackageSelectionDone(ctx);

        Map<String, String> changedParameters = collector.getConfigurationParams();
        changedParameters.put(PARAM_WIZARD_DONE, "true");
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

    public void handleGeneralPOST(Page currentPage, HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Context ctx = Context.instance(req);
        ParamCollector collector = ctx.getCollector();
        String bindAddress = collector.getConfigurationParamValue(PARAM_BIND_ADDRESS);
        if (bindAddress != null && !bindAddress.isEmpty()) {
            if (!IPValidator.validate(bindAddress)) {
                ctx.trackError(PARAM_BIND_ADDRESS, "error.invalid.ip");
            }
            try {
                InetAddress inetAddress = ConfigurationGenerator.getBindAddress(bindAddress);
                ConfigurationGenerator.checkAddressReachable(inetAddress);
            } catch (ConfigurationException e) {
                ctx.trackError(PARAM_BIND_ADDRESS, "error.already.used.ip");
            }
        }

        if (ctx.hasErrors()) {
            currentPage.dispatchToJSP(req, resp);
        } else {
            currentPage.next().dispatchToJSP(req, resp, true);
        }
    }

    public void handleHomeGET(Page currentPage, HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Context ctx = Context.instance(req);
        if (PackageDownloaderHelper.isPackageSelectionDone(ctx)) {
            navHandler.deactivatePage("PackagesSelection");
            navHandler.deactivatePage("PackagesDownload");
            navHandler.activatePage("PackagesSelectionDone");
        }
        handleDefaultGET(currentPage, req, resp);
    }

    public void handleHomePOST(Page currentPage, HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
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

    public void handleProxyPOST(Page currentPage, HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
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
            collector.addConfigurationParam("nuxeo.http.proxy.ntml.domain", null);
            if (!PackageDownloaderHelper.isPackageSelectionDone(ctx)) {
                PackageDownloader.instance().setProxy(null, 0, null, null, null, null);
            }
        } else {
            if (!NumberValidator.validate(collector.getConfigurationParam("nuxeo.http.proxy.port"))) {
                ctx.trackError("nuxeo.http.proxy.port", "error.nuxeo.http.proxy.port");
            }
            if (collector.getConfigurationParam("nuxeo.http.proxy.host").isEmpty()) {
                ctx.trackError("nuxeo.http.proxy.host", "error.nuxeo.http.proxy.emptyHost");
            }
            if ("anonymous".equals(proxyType)) {
                collector.addConfigurationParam("nuxeo.http.proxy.login", null);
                collector.addConfigurationParam("nuxeo.http.proxy.password", null);
                collector.addConfigurationParam("nuxeo.http.proxy.ntml.host", null);
                collector.addConfigurationParam("nuxeo.http.proxy.ntml.domain", null);

                if (!ctx.hasErrors()) {
                    if (!PackageDownloaderHelper.isPackageSelectionDone(ctx)) {
                        PackageDownloader.instance().setProxy(
                                collector.getConfigurationParamValue("nuxeo.http.proxy.host"),
                                Integer.parseInt(collector.getConfigurationParamValue("nuxeo.http.proxy.port")), null,
                                null, null, null);
                    }
                }
            } else {
                if (collector.getConfigurationParam("nuxeo.http.proxy.login").isEmpty()) {
                    ctx.trackError("nuxeo.http.proxy.login", "error.nuxeo.http.proxy.emptyLogin");
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

    public void handleResetGET(Page currentPage, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // reset
        Context.reset();
        SimpleNavigationHandler.reset();
        PackageDownloader.reset();

        // return to first page
        String target = "/" + req.getContextPath() + "/"
                + SimpleNavigationHandler.instance().getDefaultPage().getAction();
        if (target.startsWith("//")) {
            target = target.substring(1);
        }
        resp.sendRedirect(target);
    }

    public void handlePackageOptionsResourceGET(Page currentPage, HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        DownloadablePackageOptions options = PackageDownloader.instance().getPackageOptions();
        resp.setContentType("text/json");
        resp.getWriter().write(options.asJson());
    }

    public void handlePackagesSelectionGET(Page currentPage, HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        handleDefaultGET(currentPage, req, resp);
    }

    public void handlePackagesSelectionPOST(Page currentPage, HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        List<String> options = new ArrayList<>();
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

    public void handlePackagesDownloadGET(Page currentPage, HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if ("true".equals(req.getParameter("startDownload"))) {
            PackageDownloader.instance().startDownload();
        } else if (req.getParameter("reStartDownload") != null) {
            PackageDownloader.instance().reStartDownload(req.getParameter("reStartDownload"));
        }
        currentPage.dispatchToJSP(req, resp);
    }

    public void handlePackagesDownloadPOST(Page currentPage, HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        ParamCollector collector = Context.instance(req).getCollector();

        String installationFilePath = new File(collector.getConfigurationParam(NUXEO_DATA_DIR),
                INSTALL_AFTER_RESTART).getAbsolutePath();

        PackageDownloader.instance().scheduleDownloadedPackagesForInstallation(installationFilePath);
        PackageDownloader.reset();
        currentPage.next().dispatchToJSP(req, resp, true);
    }

}
