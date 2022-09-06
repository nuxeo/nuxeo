/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc <kleturc@nuxeo.com>
 */

package org.nuxeo.launcher.config;

import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.nuxeo.launcher.config.ConfigurationConstants.PARAM_BIND_ADDRESS;
import static org.nuxeo.launcher.config.ConfigurationConstants.PARAM_CONTEXT_PATH;
import static org.nuxeo.launcher.config.ConfigurationConstants.PARAM_HTTP_PORT;
import static org.nuxeo.launcher.config.ConfigurationConstants.PARAM_HTTP_TOMCAT_ADMIN_PORT;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.UnknownHostException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.Environment;
import org.nuxeo.launcher.config.backingservices.BackingChecker;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.FailsafeException;
import net.jodah.failsafe.RetryPolicy;

/**
 * @since 11.5
 */
public class ConfigurationChecker {

    private static final Logger log = LogManager.getLogger(ConfigurationChecker.class);

    protected static final String PARAM_RETRY_POLICY_ENABLED = "nuxeo.backing.check.retry.enabled";

    protected static final String PARAM_RETRY_POLICY_MAX_RETRIES = "nuxeo.backing.check.retry.maxRetries";

    protected static final String PARAM_RETRY_POLICY_DELAY_IN_MS = "nuxeo.backing.check.retry.delayInMs";

    protected static final int PARAM_POLICY_DEFAULT_DELAY_IN_MS = 5000;

    protected static final int PARAM_RETRY_POLICY_DEFAULT_RETRIES = 20;

    protected static final String DEFAULT_CONTEXT_NAME = "/nuxeo";

    protected static final String[] COMPLIANT_JAVA_VERSIONS = new String[] { "17" };

    protected static final String JVMCHECK_PROP = "jvmcheck";

    protected static final String JVMCHECK_FAIL = "fail";

    protected static final String JVMCHECK_NOFAIL = "nofail";

    protected static final int ADDRESS_PING_TIMEOUT_MS = 1_000;

    protected static final int MIN_PORT = 1;

    protected static final int MAX_PORT = 65535;

    protected static final Path BAD_INSTANCE_CLID_PATH = Path.of(Environment.DEFAULT_DATA_DIR, "instance.clid");

    protected final Properties systemProperties;

    public ConfigurationChecker(Properties systemProperties) {
        this.systemProperties = systemProperties;
    }

    /**
     * @return true if server configuration files already exist
     */
    public boolean isConfigured(ConfigurationHolder configHolder) {
        Path nuxeoContext = Path.of("conf", "Catalina", "localhost", getContextName(configHolder) + ".xml");
        return Files.exists(configHolder.getHomePath().resolve(nuxeoContext));
    }

    protected String getContextName(ConfigurationHolder configHolder) {
        return configHolder.getProperty(PARAM_CONTEXT_PATH, DEFAULT_CONTEXT_NAME).substring(1);
    }

    /**
     * Verifies that the server is well configured and ready to be started.
     */
    public void verify(ConfigurationHolder configHolder) throws ConfigurationException {
        checkJavaVersionIsCompliant();
        checkAddressesAndPorts(configHolder);
        checkPaths(configHolder);
        checkBackingServices(configHolder);
    }

    /**
     * Checks the java version present in system properties compared to compliant ones.
     */
    protected void checkJavaVersionIsCompliant() throws ConfigurationException {
        String version = systemProperties.getProperty("java.version");
        checkJavaVersion(version, COMPLIANT_JAVA_VERSIONS);
    }

    /**
     * Checks the given java version compared to the given compliant ones.
     *
     * @param version the java version
     * @param compliantVersions the compliant java versions
     */
    protected void checkJavaVersion(String version, String[] compliantVersions) throws ConfigurationException {
        // compliantVersions represents the java versions on which Nuxeo runs perfectly, so:
        // - if we run Nuxeo with a major java version present in compliantVersions and compatible with then this
        // method exits without error and without logging a warn message about loose compliance
        // - if we run Nuxeo with a major java version not present in compliantVersions but greater than once then
        // this method exits without error and logs a warn message about loose compliance
        // - if we run Nuxeo with a non valid java version then method exits with error
        // - if we run Nuxeo with a non valid java version and with jvmcheck=nofail property then method exits without
        // error and logs a warn message about loose compliance

        // try to retrieve the closest compliant java version
        String lastCompliantVersion = null;
        for (String compliantVersion : compliantVersions) {
            if (checkJavaVersion(version, compliantVersion, false, false)) {
                // current compliant version is valid, go to next one
                lastCompliantVersion = compliantVersion;
            } else if (lastCompliantVersion != null) {
                // current compliant version is not valid, but we found a valid one earlier, 1st case
                return;
            } else if (checkJavaVersion(version, compliantVersion, true, true)) {
                // current compliant version is not valid, try to check java version with jvmcheck=nofail, 4th case
                // here we will log about loose compliance for the lower compliant java version
                return;
            }
        }
        // we might have lastCompliantVersion, unless nothing is valid against the current java version
        if (lastCompliantVersion != null) {
            // 2nd case: log about loose compliance if current major java version is greater than the greatest
            // compliant java version
            checkJavaVersion(version, lastCompliantVersion, false, true);
            return;
        }

        // 3th case
        String message = String.format("Nuxeo requires Java %s (detected %s).", ArrayUtils.toString(compliantVersions),
                version);
        throw new ConfigurationException(message + " See '" + JVMCHECK_PROP + "' option to bypass version check.");
    }

    /**
     * Checks the java version compared to the required one.
     * <p>
     * Loose compliance is assumed if the major version is greater than the required major version or a jvmcheck=nofail
     * flag is set.
     *
     * @param version the java version
     * @param requiredVersion the required java version
     * @param allowNoFailFlag if {@code true} then check jvmcheck=nofail flag to always have loose compliance
     * @param warnIfLooseCompliance if {@code true} then log a WARN if the is loose compliance
     * @return true if the java version is compliant (maybe loosely) with the required version
     */
    protected boolean checkJavaVersion(String version, String requiredVersion, boolean allowNoFailFlag,
            boolean warnIfLooseCompliance) {
        allowNoFailFlag = allowNoFailFlag
                && JVMCHECK_NOFAIL.equalsIgnoreCase(systemProperties.getProperty(JVMCHECK_PROP, JVMCHECK_FAIL));
        try {
            JVMVersion required = JVMVersion.parse(requiredVersion);
            JVMVersion actual = JVMVersion.parse(version);
            boolean compliant = actual.compareTo(required) >= 0;
            if (compliant && actual.compareTo(required, JVMVersion.UpTo.MAJOR) == 0) {
                return true;
            }
            if (!compliant && !allowNoFailFlag) {
                return false;
            }
            // greater major version or noFail is present in system property, considered loosely compliant but may warn
            if (warnIfLooseCompliance) {
                log.warn("Nuxeo requires Java {}+ (detected {}).", requiredVersion, version);
            }
            return true;
        } catch (ParseException cause) {
            if (allowNoFailFlag) {
                log.warn("Cannot check java version", cause);
                return true;
            }
            throw new IllegalArgumentException("Cannot check java version", cause);
        }
    }

    /**
     * Will check the configured addresses are reachable and Nuxeo required ports are available on those addresses.
     */
    protected void checkAddressesAndPorts(ConfigurationHolder configHolder) throws ConfigurationException {
        InetAddress bindAddress = getBindAddress(configHolder);
        // Sanity check
        if (bindAddress.isMulticastAddress()) {
            throw new ConfigurationException("Multicast address won't work: " + bindAddress);
        }
        checkAddressReachable(bindAddress);
        checkPortAvailable(bindAddress, configHolder.getPropertyAsInteger(PARAM_HTTP_PORT, 8080));
        checkPortAvailable(bindAddress, configHolder.getPropertyAsInteger(PARAM_HTTP_TOMCAT_ADMIN_PORT, 8005));
    }

    /**
     * Checks the userConfig bind address is not 0.0.0.0 and replaces it with 127.0.0.1 if needed
     *
     * @param configHolder The configuration holding the {@link ConfigurationConstants#PARAM_BIND_ADDRESS}
     * @return the userConfig bind address if not 0.0.0.0 else 127.0.0.1
     */
    protected InetAddress getBindAddress(ConfigurationHolder configHolder) throws ConfigurationException {
        InetAddress bindAddress;
        try {
            String hostName = configHolder.getProperty(PARAM_BIND_ADDRESS);
            bindAddress = InetAddress.getByName(hostName);
            if (bindAddress.isAnyLocalAddress()) {
                boolean preferIPv6 = "false".equals(systemProperties.getProperty("java.net.preferIPv4Stack"))
                        && "true".equals(systemProperties.getProperty("java.net.preferIPv6Addresses"));
                bindAddress = preferIPv6 ? InetAddress.getByName("::1") : InetAddress.getByName("127.0.0.1");
                log.debug("Bind address is \"ANY\", using local address instead: {}", bindAddress);
            }
            log.debug("Configured bind address: {}", bindAddress);
        } catch (UnknownHostException e) {
            throw new ConfigurationException(e);
        }
        return bindAddress;
    }

    /**
     * @param address address to check for availability
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    protected void checkAddressReachable(InetAddress address) throws ConfigurationException {
        try {
            log.debug("Checking availability of address: {}", address);
            address.isReachable(ADDRESS_PING_TIMEOUT_MS);
        } catch (IllegalArgumentException | IOException e) {
            throw new ConfigurationException("Unreachable bind address " + address, e);
        }
    }

    /**
     * Checks if port is available on given address.
     *
     * @param port port to check for availability
     * @throws ConfigurationException Throws an exception if address is unavailable.
     */
    protected void checkPortAvailable(InetAddress address, int port) throws ConfigurationException {
        if (port == 0 || port == -1) {
            log.warn("Port is set to {} - assuming it is disabled - skipping availability check", port);
            return;
        }
        if (port < MIN_PORT || port > MAX_PORT) {
            throw new IllegalArgumentException("Invalid port: " + port);
        }
        log.debug("Checking availability of port {} on address {}", port, address);
        try (ServerSocket socketTCP = new ServerSocket(port, 0, address)) {
            socketTCP.setReuseAddress(true);
        } catch (IOException e) {
            throw new ConfigurationException(e.getMessage() + ": " + address + ":" + port, e);
        }
    }

    /**
     * Checks server paths; warn if deprecated paths exist.
     */
    protected void checkPaths(ConfigurationHolder configHolder) throws ConfigurationException {
        // clid location should follow data directory
        Path badInstanceClid = configHolder.getRuntimeHomePath().resolve(BAD_INSTANCE_CLID_PATH);
        Path dataPath = configHolder.getDataPath();
        if (Files.exists(badInstanceClid) && !badInstanceClid.startsWith(dataPath)) {
            log.warn("Moving {} to {}.", badInstanceClid, dataPath);
            try {
                FileUtils.moveFileToDirectory(badInstanceClid.toFile(), dataPath.toFile(), true);
            } catch (IOException e) {
                throw new ConfigurationException("NXP-6722 move failed: " + e.getMessage(), e);
            }
        }

        Path oldPackagesPath = dataPath.resolve(Environment.DEFAULT_MP_DIR);
        Path packagesPath = configHolder.getPackagesPath();
        if (Files.exists(oldPackagesPath) && !oldPackagesPath.equals(packagesPath)) {
            log.warn("NXP-8014 Packages cache location changed. You can safely delete {} or move its content to {}",
                    oldPackagesPath, packagesPath);
        }
    }

    /**
     * Calls all {@link BackingChecker} if they accept the current configuration.
     */
    public void checkBackingServices(ConfigurationHolder configHolder) throws ConfigurationException {
        RetryPolicy retryPolicy = buildRetryPolicy(configHolder);
        for (BackingChecker checker : instantiateBackingCheckers(configHolder)) {
            if (checker.accepts(configHolder)) {
                ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                try {
                    // Propagate the checker's class loader for jaas
                    Thread.currentThread().setContextClassLoader(checker.getClass().getClassLoader());
                    Failsafe.with(retryPolicy)
                            .onFailedAttempt(failure -> log.error(failure.getMessage(), failure))
                            .onRetry((c, f, ctx) -> log.warn("Failure {}. Retrying....", ctx.getExecutions()))
                            .run(() -> checker.check(configHolder));
                } catch (FailsafeException e) {
                    if (e.getCause() instanceof ConfigurationException) {
                        throw ((ConfigurationException) e.getCause());
                    } else {
                        throw new ConfigurationException("Error during backing checks", e);
                    }
                } finally {
                    Thread.currentThread().setContextClassLoader(classLoader);
                }
            }
        }
    }

    protected RetryPolicy buildRetryPolicy(ConfigurationHolder configHolder) {
        RetryPolicy retryPolicy = new RetryPolicy().withMaxRetries(0);
        if (configHolder.getPropertyAsBoolean(PARAM_RETRY_POLICY_ENABLED)) {
            int maxRetries = configHolder.getPropertyAsInteger(PARAM_RETRY_POLICY_MAX_RETRIES,
                    PARAM_RETRY_POLICY_DEFAULT_RETRIES);
            int delay = configHolder.getPropertyAsInteger(PARAM_RETRY_POLICY_DELAY_IN_MS,
                    PARAM_POLICY_DEFAULT_DELAY_IN_MS);

            retryPolicy = retryPolicy.retryOn(ConfigurationException.class)
                                     .withMaxRetries(maxRetries)
                                     .withDelay(delay, TimeUnit.MILLISECONDS);
        }
        return retryPolicy;
    }

    protected List<BackingChecker> instantiateBackingCheckers(ConfigurationHolder configHolder)
            throws ConfigurationException {
        var checkers = new ArrayList<BackingChecker>();
        var items = new ArrayList<>(configHolder.getIncludedTemplateNames());
        // Add backing without template
        items.add("elasticsearch");
        items.add("kafka");
        for (String item : items) {
            try {
                log.debug("Resolving checker: {}", item);
                URLClassLoader ucl = getBackingCheckerClassLoader(configHolder, item);
                if (ucl.getURLs().length > 0) {
                    log.debug("Adding checker: {} with class path: {}", () -> item,
                            () -> Arrays.toString(ucl.getURLs()));
                    String checkClass = configHolder.getProperty(item + ".check.class");
                    log.debug("Instantiating checker: {} class: {}", item, checkClass);
                    Class<?> klass = Class.forName(checkClass, true, ucl);
                    checkers.add((BackingChecker) klass.getDeclaredConstructor().newInstance());
                }
            } catch (ReflectiveOperationException | ClassCastException e) {
                throw new ConfigurationException("Unable to check configuration for backing service: " + item, e);
            }
        }
        return checkers;
    }

    /**
     * Build a ClassLoader based on the classpath definition of a template.
     */
    protected URLClassLoader getBackingCheckerClassLoader(ConfigurationHolder configHolder, String checker) {
        Path templatePath = getTemplatePath(configHolder, checker);
        String classPath = getBackingCheckerClasspath(configHolder, checker);
        URL[] urls = Stream.of(classPath.split(":(?!\\\\)"))
                           .flatMap(e -> getJarsFromClasspathEntry(configHolder, templatePath, e))
                           .map(this::convertToJarFileURL)
                           .filter(Objects::nonNull)
                           .peek(u -> log.debug("Adding url: {}", u))
                           .toArray(URL[]::new);
        return new URLClassLoader(urls);
    }

    private Path getTemplatePath(ConfigurationHolder configHolder, String checker) {
        Path templatePath = configHolder.getTemplatesPath().resolve(checker);
        if (Files.notExists(templatePath)) {
            templatePath = configHolder.getTemplatesPath().resolve("default"); // for backing checker without template
        }
        return templatePath;
    }

    /**
     * Read the classpath parameter from the template and expand parameters with their value. It allows classpath of the
     * form ${nuxeo.home}/nxserver/bundles/...
     */
    protected String getBackingCheckerClasspath(ConfigurationHolder configHolder, String template) {
        String classPath = configHolder.getProperty(template + ".check.classpath");
        return trimToEmpty(configHolder.instantiateTemplateParser().keepEncryptedAsVar(false).processText(classPath));
    }

    /**
     * Given a single classpath entry, return the list of JARs referenced by it.<br>
     * For instance :
     * <ul>
     * <li>nxserver/lib -&gt; ${templatePath}/nxserver/lib</li>
     * <li>/somePath/someLib-*.jar</li>
     * </ul>
     */
    protected Stream<Path> getJarsFromClasspathEntry(ConfigurationHolder configHolder, Path templatePath,
            String entry) {
        // don't use the Path API as * is a reserved character on Windows
        var entryPath = entry.replace("/", File.separator);
        if (!new File(entryPath).isAbsolute()) {
            entryPath = templatePath.toString() + File.separator + entryPath;
        }

        var slashIndex = entryPath.lastIndexOf(File.separator);
        if (slashIndex == -1) {
            return Stream.empty();
        }
        var parentDir = new File(entryPath.substring(0, slashIndex));

        // ugly trick mandatory to let the PathMatcher match on windows
        var pattern = "glob:" + entryPath.replaceAll("\\\\", "\\\\\\\\");
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher(pattern);
        FileFilter filter = f -> matcher.matches(f.toPath()) && f.toPath().startsWith(configHolder.getHomePath());
        File[] matchingFiles = parentDir.listFiles(filter);
        if (matchingFiles != null) {
            var builder = Stream.<Path> builder();
            for (File file : matchingFiles) {
                if (file.isDirectory()) {
                    List.of(file.listFiles(f -> f.getName().endsWith(".jar"))).forEach(f -> builder.add(f.toPath()));
                } else if (file.getName().endsWith(".jar")) {
                    builder.add(file.toPath());
                }
            }
            return builder.build();
        }
        return Stream.empty();
    }

    protected URL convertToJarFileURL(Path path) {
        try {
            return new URL("jar:file:" + path + "!/");
        } catch (MalformedURLException e) {
            log.error("Unable to convert path: {} to URL", path, e);
            return null;
        }
    }

    /**
     * Checks that the process is executed with a supported Java version. See
     * <a href="http://www.oracle.com/technetwork/java/javase/versioning-naming-139433.html">J2SE SDK/JRE Version String
     * Naming Convention</a>
     */
    public static void checkJavaVersion() throws ConfigurationException {
        new ConfigurationChecker(System.getProperties()).checkJavaVersionIsCompliant();
    }

    /**
     * Checks the java version compared to the required one.
     * <p>
     * If major version is same as required major version and minor is greater or equal, it is compliant.
     * <p>
     * If major version is greater than required major version, it is compliant.
     *
     * @param version the java version
     * @param requiredVersion the required java version
     * @return true if the java version is compliant with the required version
     */
    public static boolean checkJavaVersion(String version, String requiredVersion) {
        return new ConfigurationChecker(System.getProperties()).checkJavaVersion(version, requiredVersion, false,
                false);
    }
}
