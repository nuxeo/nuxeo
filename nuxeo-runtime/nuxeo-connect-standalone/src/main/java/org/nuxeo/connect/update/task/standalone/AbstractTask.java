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
 *     bstefanescu, jcarsique
 */
package org.nuxeo.connect.update.task.standalone;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.Environment;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.connect.update.LocalPackage;
import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.PackageState;
import org.nuxeo.connect.update.PackageUpdateService;
import org.nuxeo.connect.update.ValidationStatus;
import org.nuxeo.connect.update.task.Task;
import org.nuxeo.connect.update.task.update.UpdateManager;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public abstract class AbstractTask implements Task {

    static final Log log = LogFactory.getLog(AbstractTask.class);

    public static final String PKG_ID = "package.id";

    public static final String PKG_NAME = "package.name";

    public static final String PKG_VERSION = "package.version";

    public static final String PKG_ROOT = "package.root";

    public static final String ENV_HOME = "env.home";

    /**
     * @since 5.5
     */
    public static final String ENV_SERVER_HOME = "env.server.home";

    /**
     * Set only on JBoss - the EAR root directory path
     */
    public static final String ENV_EAR = "env.ear";

    public static final String ENV_LIB = "env.lib";

    public static final String ENV_SYSLIB = "env.syslib";

    public static final String ENV_BUNDLES = "env.bundles";

    public static final String ENV_CONFIG = "env.config";

    /**
     * @since 5.5
     */
    public static final String ENV_TEMPLATES = "env.templates";

    public static final String ENV_TIMESTAMP = "sys.timestamp";

    /**
     * The host application name.
     *
     * @see Environment#getHostApplicationName()
     */
    public static final String ENV_HOSTAPP_NAME = "env.hostapp.name";

    /**
     * The host application version
     *
     * @see Environment#getHostApplicationVersion()
     */
    public static final String ENV_HOSTAPP_VERSION = "env.hostapp.version";

    protected boolean restart;

    protected LocalPackage pkg;

    protected String serverPathPrefix;

    protected UpdateManager updateMgr;

    protected boolean updateMgrLoaded = false;

    protected PackageUpdateService service;

    /**
     * A map of environment key/values that can be used in XML install files as variables.
     */
    protected final Map<String, String> env;

    public AbstractTask(PackageUpdateService pus) {
        service = pus;
        env = new HashMap<>();
        Environment nxenv = Environment.getDefault();
        File serverHome = nxenv.getServerHome();
        File nxHome = nxenv.getRuntimeHome();
        File config = nxenv.getConfig();
        serverPathPrefix = serverHome.getAbsolutePath();
        if (!serverPathPrefix.endsWith(File.separator)) {
            serverPathPrefix = serverPathPrefix.concat(File.separator);
        }
        env.put(ENV_SERVER_HOME, serverHome.getAbsolutePath());
        env.put(ENV_HOME, nxHome.getAbsolutePath());
        env.put(ENV_CONFIG, config.getAbsolutePath());
        env.put(ENV_HOSTAPP_NAME, nxenv.getHostApplicationName());
        env.put(ENV_HOSTAPP_VERSION, nxenv.getHostApplicationVersion());
        env.put(ENV_SYSLIB, new File(serverHome, "lib").getAbsolutePath());
        if (nxenv.isJBoss()) {
            File ear = config.getParentFile();
            env.put(ENV_EAR, ear.getAbsolutePath());
            env.put(ENV_LIB, new File(ear, "lib").getAbsolutePath());
            env.put(ENV_BUNDLES, new File(ear, "bundles").getAbsolutePath());
        } else {
            env.put(ENV_LIB, new File(nxHome, "lib").getAbsolutePath());
            env.put(ENV_BUNDLES, new File(nxHome, "bundles").getAbsolutePath());
        }
        env.put(ENV_TEMPLATES, new File(serverHome, "templates").getAbsolutePath());
        env.put(ENV_TIMESTAMP, new SimpleDateFormat("yyMMddHHmmss").format(new Date()));
        updateMgr = new UpdateManager(serverHome, service.getRegistry());
    }

    public abstract boolean isInstallTask();

    @Override
    @SuppressWarnings("hiding")
    public void initialize(LocalPackage pkg, boolean restart) throws PackageException {
        this.pkg = pkg;
        this.restart = restart;
        env.put(PKG_ID, pkg.getId());
        env.put(PKG_NAME, pkg.getName());
        env.put(PKG_VERSION, pkg.getVersion().toString());
        env.put(PKG_ROOT, pkg.getData().getRoot().getAbsolutePath());

        if (log.isDebugEnabled()) {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            final PrintStream outPrint = new PrintStream(out);
            MapUtils.debugPrint(outPrint, null, env);
            log.debug(out.toString());
        }
    }

    /**
     * Get a file given its key in the environment map. If no key exists then null is returned.
     *
     * @param key
     */
    public File getFile(String key) {
        String val = env.get(key);
        return val == null ? null : new File(val);
    }

    @Override
    public boolean isRestartRequired() {
        return restart;
    }

    @Override
    public LocalPackage getPackage() {
        return pkg;
    }

    protected Map<Object, Object> createContextMap(Map<String, String> params) {
        Map<Object, Object> map = new HashMap<>(System.getProperties());
        map.putAll(env);
        if (params != null && !params.isEmpty()) {
            map.putAll(params);
        }
        return map;
    }

    protected String loadParametrizedFile(File file, Map<String, String> params) throws IOException {
        String content = FileUtils.readFileToString(file);
        // replace variables.
        return StringUtils.expandVars(content, createContextMap(params));
    }

    protected void saveParams(Map<String, String> params) throws PackageException {
        if (params == null || params.isEmpty()) {
            return;
        }
        try {
            Properties props = new Properties();
            props.putAll(params);
            File file = pkg.getData().getEntry(LocalPackage.INSTALL_PROPERTIES);
            FileOutputStream out = new FileOutputStream(file);
            try {
                props.store(out, "user install parameters");
            } finally {
                out.close();
            }
        } catch (IOException e) {
            throw new PackageException("Failed to save install parameters", e);
        }
    }

    @Override
    public synchronized void run(Map<String, String> params) throws PackageException {
        if (isInstallTask()) {
            LocalPackage oldpkg = service.getActivePackage(pkg.getName());
            if (oldpkg != null) {
                if (oldpkg.getPackageState() == PackageState.INSTALLING) {
                    throw new PackageException("Another package with the same name is installing: " + oldpkg.getName());
                } else {
                    // uninstall it.
                    Task utask = oldpkg.getUninstallTask();
                    try {
                        utask.run(new HashMap<String, String>());
                    } catch (PackageException e) {
                        utask.rollback();
                        throw new PackageException("Failed to uninstall: " + oldpkg.getId()
                                + ". Cannot continue installation of " + pkg.getId(), e);
                    }
                }
            }
        }
        service.setPackageState(pkg, PackageState.INSTALLING);
        saveParams(params);
        doRun(params);
        taskDone();
        if (updateMgrLoaded) {
            updateMgr.store();
        }
    }

    public synchronized UpdateManager getUpdateManager() throws PackageException {
        if (!updateMgrLoaded) {
            updateMgr.load();
            updateMgrLoaded = true;
        }
        return updateMgr;
    }

    protected abstract void rollbackDone() throws PackageException;

    protected abstract void taskDone() throws PackageException;

    @Override
    public void rollback() throws PackageException {
        try {
            doRollback();
        } finally {
            rollbackDone();
        }
    }

    @Override
    public void setRestartRequired(boolean isRestartRequired) {
        this.restart = isRestartRequired;
    }

    protected abstract void doRun(Map<String, String> params) throws PackageException;

    protected abstract void doRollback() throws PackageException;

    @Override
    public ValidationStatus validate() throws PackageException {
        ValidationStatus status = new ValidationStatus();
        if (isInstallTask()) {
            validateInstall(status);
        }
        doValidate(status);
        return status;
    }

    public abstract void doValidate(ValidationStatus status) throws PackageException;

    protected LocalPackage validateInstall(ValidationStatus status) throws PackageException {
        LocalPackage oldpkg = service.getActivePackage(pkg.getName());
        if (oldpkg != null) {
            if (oldpkg.getPackageState() == PackageState.INSTALLING) {
                status.addWarning("A package with the same name: " + oldpkg.getId()
                        + " is being installing. Try again later.");
            } else {
                status.addWarning("The package " + oldpkg.getId() + " will be uninstalled!");
            }
            return oldpkg;
        }
        return null;
    }

    @Override
    public String getRelativeFilePath(File file) {
        String path = file.getAbsolutePath();
        if (path.startsWith(serverPathPrefix)) {
            return path.substring(serverPathPrefix.length());
        }
        return path;
    }

}
