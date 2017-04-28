/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 * Contributors: Nuxeo team
 *
 */
package org.nuxeo.launcher.config;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.TextTemplate;
import org.nuxeo.launcher.config.backingservices.BackingChecker;
import org.nuxeo.launcher.config.backingservices.DBCheck;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.FailsafeException;
import net.jodah.failsafe.RetryPolicy;

/**
 * Calls backing services checks to verify that they are ready to use before starting Nuxeo.
 *
 * @since 9.2
 */
public class BackingServiceConfigurator {

    /**
     *
     */
    private static final String JAR_EXTENSION = ".jar";

    private static final String PARAM_RETRY_POLICY_ENABLED = "nuxeo.backing.check.retry.enabled";

    private static final String PARAM_RETRY_POLICY_MAX_RETRIES = "nuxeo.backing.check.retry.maxRetries";

    private static final String PARAM_RETRY_POLICY_DELAY_IN_MS = "nuxeo.backing.check.retry.delayInMs";

    private static final Log log = LogFactory.getLog(BackingServiceConfigurator.class);

    Set<BackingChecker> checkers;

    private ConfigurationGenerator configurationGenerator;

    public BackingServiceConfigurator(ConfigurationGenerator configurationGenerator) {
        this.configurationGenerator = configurationGenerator;
    }

    /**
     * Calls all BackingChecker if they accept the current configuration.
     *
     * @throws ConfigurationException
     */
    public void verifyInstallation() throws ConfigurationException {

        RetryPolicy retryPolicy = buildRetryPolicy();

        // Get all checkers
        for (BackingChecker checker : getCheckers()) {
            if (checker.acceptConfiguration(configurationGenerator)) {
                try {
                    Failsafe.with(retryPolicy)
                            .onFailedAttempt(failure -> log.error(failure.getMessage())) //
                            .onRetry((c, f,
                                    ctx) -> log.warn(String.format("Failure %d. Retrying....", ctx.getExecutions()))) //
                            .run(() -> checker.check(configurationGenerator)); //
                } catch (FailsafeException e) {
                    if (e.getCause() instanceof ConfigurationException) {
                        throw ((ConfigurationException) e.getCause());
                    } else {
                        throw e;
                    }
                }
            }
        }
    }

    private RetryPolicy buildRetryPolicy() {
        RetryPolicy retryPolicy = new RetryPolicy().withMaxRetries(0);

        Properties userConfig = configurationGenerator.getUserConfig();
        if ("true".equals((userConfig.getProperty(PARAM_RETRY_POLICY_ENABLED,"false")))) {

            int maxRetries = Integer.parseInt(userConfig.getProperty(PARAM_RETRY_POLICY_MAX_RETRIES, "20"));
            int delay = Integer.parseInt(userConfig.getProperty(PARAM_RETRY_POLICY_DELAY_IN_MS, "5000"));

            retryPolicy = retryPolicy.retryOn(ConfigurationException.class).withMaxRetries(maxRetries).withDelay(delay,
                    TimeUnit.MILLISECONDS);
        }
        return retryPolicy;
    }

    protected Collection<BackingChecker> getCheckers() throws ConfigurationException {

        if (checkers == null) {
            checkers = new HashSet<>();

            for (String template : configurationGenerator.getTemplateList()) {
                try {
                    File templateDir = configurationGenerator.getTemplateConf(template).getParentFile();
                    String classPath = getClasspathForTemplate(template);
                    String checkClass = configurationGenerator.getUserConfig().getProperty(template + ".check.class");

                    Optional<URLClassLoader> ucl = getClassLoaderForTemplate(templateDir, classPath);
                    if (ucl.isPresent()) {
                        Class<?> klass = Class.forName(checkClass, true, ucl.get());
                        checkers.add((BackingChecker) klass.newInstance());
                    }

                } catch (IOException e) {
                    log.warn("Unable to read check configuration for template : " + template, e);
                } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                    throw new ConfigurationException("Unable to check configuration for backing service " + template,
                            e);
                }
            }
            checkers.add(new DBCheck());
        }
        return checkers;
    }

    /**
     * Read the classpath parameter from the template and expand parameters with their value. It allow classpath of the
     * form ${nuxeo.home}/nxserver/bundles/... VisibleForTesting
     *
     * @param template The name of the template
     * @return
     */
    String getClasspathForTemplate(String template) {
        String classPath = configurationGenerator.getUserConfig().getProperty(template + ".check.classpath");
        final TextTemplate templateParser = new TextTemplate(configurationGenerator.getUserConfig());
        classPath = templateParser.processText(classPath);
        return classPath;
    }

    /**
     * Build a ClassLoader based on the classpath definition of a template.
     *
     * @param templateDir
     * @param classPath
     * @return
     * @throws ConfigurationException
     * @throws IOException
     * @since 9.2
     */
    protected Optional<URLClassLoader> getClassLoaderForTemplate(File templateDir, String classPath)
            throws ConfigurationException, IOException {
        if (StringUtils.isBlank(classPath)) {
            return Optional.empty();
        }

        String[] classpathEntries = classPath.split(":");

        List<URL> urlsList = new ArrayList<>();

        List<File> files = new ArrayList<>();
        for (String entry : classpathEntries) {
            files.addAll(getJarsFromClasspathEntry(templateDir, entry));
        }

        if (!files.isEmpty()) {
            for (File file : files) {
                try {
                    urlsList.add(new URL("jar:file:" + file.getPath() + "!/"));
                    log.debug("Added " + file.getPath());
                } catch (MalformedURLException e) {
                    log.error(e);
                }
            }
        } else {
            return Optional.empty();
        }

        URLClassLoader ucl = new URLClassLoader(urlsList.toArray(new URL[0]));
        return Optional.of(ucl);
    }

    /**
     * Given a single classpath entry, return the liste of JARs referenced by it.<br>
     * For instance :
     * <ul>
     * <li>nxserver/lib -> ${templatePath}/nxserver/lib</li>
     * <li>/somePath/someLib-*.jar</li></li> VisibleForTesting
     *
     * @param templatePath
     * @param entry
     * @return
     */
    Collection<File> getJarsFromClasspathEntry(File templatePath, String entry) {

        Collection<File> jars = new ArrayList<>();

        // Add templatePath if relative classPath
        File target = entry.startsWith("/") ? new File(entry) : new File(templatePath, entry);
        String path = target.getAbsolutePath();

        int slashIndex = path.lastIndexOf("/");
        String dirName = path.substring(0, slashIndex);
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + path);

        File parentDir = new File(dirName);
        File[] realMatchingFiles = parentDir.listFiles(
                f -> matcher.matches(f.toPath()) && isInSubDirectory(configurationGenerator.getNuxeoHome(), f));

        if (realMatchingFiles != null) {
            for (File file : realMatchingFiles) {
                if (file.isDirectory()) {
                    jars.addAll(Arrays.asList(file.listFiles(f -> f.getName().endsWith(JAR_EXTENSION))));
                } else {
                    if (file.getName().endsWith(JAR_EXTENSION)) {
                        jars.add(file);
                    }
                }
            }
        }
        return jars;
    }

    /**
     * Checks if file is included under a directory.
     *
     * @param dir
     * @param file
     * @return
     */
    private static boolean isInSubDirectory(File dir, File file) {

        if (file == null)
            return false;

        if (file.equals(dir))
            return true;

        return isInSubDirectory(dir, file.getParentFile());
    }

}
