/*
 * (C) Copyright 2010-2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Julien Carsique
 *     Florent Guillaume
 *     Ronan DANIELLOU
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.launcher;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;
import static org.apache.logging.log4j.LogManager.ROOT_LOGGER_NAME;
import static org.nuxeo.common.Environment.NUXEO_CONTEXT_PATH;
import static org.nuxeo.common.Environment.NUXEO_DATA_DIR;
import static org.nuxeo.common.Environment.NUXEO_HOME;
import static org.nuxeo.common.Environment.NUXEO_LOG_DIR;
import static org.nuxeo.common.Environment.NUXEO_MP_DIR;
import static org.nuxeo.common.Environment.NUXEO_TMP_DIR;
import static org.nuxeo.launcher.config.ConfigurationGenerator.NUXEO_CONF;
import static org.nuxeo.launcher.config.ConfigurationGenerator.NUXEO_DEFAULT_CONF;

import java.io.Console;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.SocketTimeoutException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.validation.constraints.NotNull;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.nuxeo.common.Environment;
import org.nuxeo.common.codec.Crypto;
import org.nuxeo.common.codec.CryptoProperties;
import org.nuxeo.connect.connector.NuxeoClientInstanceType;
import org.nuxeo.connect.connector.http.ConnectUrlConfig;
import org.nuxeo.connect.data.ConnectProject;
import org.nuxeo.connect.identity.LogicalInstanceIdentifier.NoCLID;
import org.nuxeo.connect.identity.TechnicalInstanceIdentifier;
import org.nuxeo.connect.registration.RegistrationException;
import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.Version;
import org.nuxeo.launcher.config.ConfigurationException;
import org.nuxeo.launcher.config.ConfigurationGenerator;
import org.nuxeo.launcher.config.TomcatConfigurator;
import org.nuxeo.launcher.connect.ConnectBroker;
import org.nuxeo.launcher.connect.ConnectRegistrationBroker;
import org.nuxeo.launcher.connect.LauncherRestartException;
import org.nuxeo.launcher.daemon.DaemonThreadFactory;
import org.nuxeo.launcher.gui.NuxeoLauncherGUI;
import org.nuxeo.launcher.info.CommandInfo;
import org.nuxeo.launcher.info.CommandSetInfo;
import org.nuxeo.launcher.info.ConfigurationInfo;
import org.nuxeo.launcher.info.DistributionInfo;
import org.nuxeo.launcher.info.InstanceInfo;
import org.nuxeo.launcher.info.KeyValueInfo;
import org.nuxeo.launcher.info.MessageInfo;
import org.nuxeo.launcher.info.PackageInfo;
import org.nuxeo.launcher.monitoring.StatusServletClient;
import org.nuxeo.launcher.process.ProcessManager;
import org.nuxeo.log4j.Log4JHelper;

import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.json.impl.writer.JsonXmlStreamWriter;

/**
 * @author jcarsique
 * @since 5.4.2
 * @implNote since 11.1, launcher only handles Tomcat and is no more abstract
 * @implNote the launcher has a specific log4j configuration, check its log4j2.xml
 */
public class NuxeoLauncher {

    /** @since 7.4 */
    protected static final String OUTPUT_UNSET_VALUE = "<unset>";

    /** @since 5.6 */
    protected static final String OPTION_NODEPS = "nodeps";

    private static final String OPTION_NODEPS_DESC = "Ignore package dependencies and constraints.";

    /** @since 5.6 */
    protected static final String OPTION_GUI = "gui";

    private static final String OPTION_GUI_DESC = "Start graphical user interface (default is true on Windows and false on other platforms).";

    /** @since 5.6 */
    protected static final String OPTION_JSON = "json";

    private static final String OPTION_JSON_DESC = "Output JSON for mp-* commands.";

    /** @since 5.6 */
    protected static final String OPTION_XML = "xml";

    private static final String OPTION_XML_DESC = "Output XML for mp-* commands.";

    /** @since 5.6 */
    protected static final String OPTION_DEBUG = "debug";

    private static final String OPTION_DEBUG_DESC = "Activate debug messages.\n"
            + "<categories>: comma-separated Java categories to debug (default: \"org.nuxeo.launcher\").";

    /** @since 7.4 */
    private static final String OPTION_DEBUG_CATEGORY_ARG_NAME = "categories";

    /** @since 5.6 */
    protected static final String OPTION_DEBUG_CATEGORY = "dc";

    private static final String OPTION_DEBUG_CATEGORY_DESC = "Deprecated: see categories on '--debug' option.";

    /** @since 5.6 */
    protected static final String OPTION_QUIET = "quiet";

    private static final String OPTION_QUIET_DESC = "Suppress information messages.";

    /** @since 5.6 */
    protected static final String OPTION_HELP = "help";

    private static final String OPTION_HELP_DESC = "Show detailed help.";

    /** @since 5.6 */
    protected static final String OPTION_RELAX = "relax";

    private static final String OPTION_RELAX_DESC = "Allow relax constraint on current platform (default: "
            + ConnectBroker.OPTION_RELAX_DEFAULT + ").";

    /** @since 5.6 */
    protected static final String OPTION_ACCEPT = "accept";

    private static final String OPTION_ACCEPT_DESC = "Accept, refuse or ask confirmation for all changes (default: "
            + ConnectBroker.OPTION_ACCEPT_DEFAULT + ").\n"
            + "In non interactive mode, '--accept=true' also sets '--relax=true' if needed.";

    /** @since 5.9.1 */
    protected static final String OPTION_SNAPSHOT = "snapshot";

    private static final String OPTION_SNAPSHOT_DESC = "Allow use of SNAPSHOT Nuxeo Packages.\n"
            + "This option is implicit:\n" //
            + "\t- on SNAPSHOT distributions (daily builds),\n"
            + "\t- if the command explicitly requests a SNAPSHOT package.";

    /** @since 5.9.1 */
    @Deprecated(since = "11.1")
    @SuppressWarnings("DeprecatedIsStillUsed")
    protected static final String OPTION_FORCE = "force";

    @Deprecated(since = "11.1")
    @SuppressWarnings("DeprecatedIsStillUsed")
    private static final String OPTION_FORCE_DESC = "Deprecated since 11.1: strict mode is the default.";

    /** @since 7.4 */
    @Deprecated(since = "11.1")
    @SuppressWarnings("DeprecatedIsStillUsed")
    protected static final String OPTION_STRICT = "strict";

    @Deprecated(since = "11.1")
    @SuppressWarnings("DeprecatedIsStillUsed")
    private static final String OPTION_STRICT_DESC = "Deprecated since 11.1: strict mode is the default.";

    /** @since 11.1 */
    protected static final String OPTION_LENIENT = "lenient";

    protected static final String OPTION_LENIENT_DESC = "Do not abort in error the start command when a component cannot "
            + "be activated or if a server is already running.";

    /** @since 5.6 */
    protected static final String OPTION_HIDE_DEPRECATION = "hide-deprecation-warnings";

    protected static final String OPTION_HIDE_DEPRECATION_DESC = "Hide deprecation warnings.";

    /** @since 6.0 */
    protected static final String OPTION_IGNORE_MISSING = "ignore-missing";

    protected static final String OPTION_IGNORE_MISSING_DESC = "Ignore unknown packages on mp-add, mp-install and mp-set commands.";

    /** @since 6.0 */
    protected static final String OPTION_CLID = "clid";

    private static final String OPTION_CLID_DESC = "Use the provided instance CLID file";

    /** @since 10.3 */
    protected static final String OPTION_OFFLINE = "offline";

    private static final String OPTION_OFFLINE_DESC = "Allow offline registration";

    /** @since 8.10-HF15 */
    protected static final String OPTION_RENEW = "renew";

    private static final String OPTION_RENEW_DESC = "Renew the current CLID";

    /** @since 7.4 */
    protected static final String OPTION_ENCRYPT = "encrypt";

    private static final String OPTION_ENCRYPT_ARG_NAME = "algorithm";

    private static final String OPTION_ENCRYPT_DESC = String.format("Activate key value symmetric encryption.\n"
            + "The algorithm can be configured: <%s> is a cipher transformation of the form: \"algorithm/mode/padding\" or \"algorithm\".\n"
            + "Default value is \"%s\" (Advanced Encryption Standard, Electronic Cookbook Mode, PKCS5-style padding).",
            OPTION_ENCRYPT, Crypto.DEFAULT_ALGO);

    /** @since 7.4 */
    protected static final String OPTION_SET = "set";

    private static final String OPTION_SET_ARG_NAME = "template";

    private static final String OPTION_SET_DESC = String.format("Set the value for a given key.\n"
            + "The value is stored in {{%s}} by default unless a template name is provided; if so, it is then stored in the template's {{%s}} file.\n"
            + "If the value is empty (''), then the property is unset.\n"
            + "This option is implicit if no '--get' or '--get-regexp' option is used and there are exactly two parameters (key value).",
            NUXEO_CONF, NUXEO_DEFAULT_CONF);

    /** @since 7.4 */
    protected static final String OPTION_GET = "get";

    private static final String OPTION_GET_DESC = "Get the value for a given key. Returns error code 6 if the key was not found.\n"
            + "This option is implicit if '--set' option is not used and there are more or less than two parameters.";

    /** @since 7.4 */
    protected static final String OPTION_GET_REGEXP = "get-regexp";

    private static final String OPTION_GET_REGEXP_DESC = "Get the value for all keys matching the given regular expression(s).";

    /** @since 8.3 */
    protected static final String OPTION_GZIP_OUTPUT = "gzip";

    private static final String OPTION_GZIP_DESC = "Compress the output.";

    /** @since 8.3 */
    protected static final String OPTION_OUTPUT = "output";

    private static final String OPTION_OUTPUT_DESC = "Write output in specified file.";

    /** @since 8.3 */
    protected static final String OPTION_PRETTY_PRINT = "pretty-print";

    private static final String OPTION_PRETTY_PRINT_DESC = "Pretty print the output.";

    // Fallback to avoid an error when the log dir is not initialized
    static {
        if (System.getProperty(NUXEO_LOG_DIR) == null) {
            System.setProperty(NUXEO_LOG_DIR, ".");
        }
    }

    /** @since 5.6 */
    private static final String DEFAULT_NUXEO_CONTEXT_PATH = "/nuxeo";

    private static final Logger log = LogManager.getLogger(NuxeoLauncher.class);

    private static final Marker NO_NEW_LINE = MarkerManager.getMarker("NO_NEW_LINE");

    private static final Options options = initParserOptions();

    private static final String JAVA_OPTS_PROPERTY = "launcher.java.opts";

    private static final String JAVA_OPTS_DEFAULT = "-Xms512m -Xmx1024m";

    private static final String OVERRIDE_JAVA_TMPDIR_PARAM = "launcher.override.java.tmpdir";

    protected boolean overrideJavaTmpDir;

    private static final String START_MAX_WAIT_PARAM = "launcher.start.max.wait";

    private static final String STOP_MAX_WAIT_PARAM = "launcher.stop.max.wait";

    /**
     * Default maximum time to wait for server startup summary in logs (in seconds).
     */
    private static final String START_MAX_WAIT_DEFAULT = "300";

    /**
     * Default maximum time to wait for effective stop (in seconds)
     */
    private static final String STOP_MAX_WAIT_DEFAULT = "60";

    private static final String[] COMMANDS_NO_GUI = { "configure", "mp-purge", "mp-add", "mp-install", "mp-uninstall",
            "mp-request", "mp-remove", "mp-hotfix", "mp-upgrade", "mp-reset", "mp-list", "mp-listall", "mp-update",
            "status", "showconf", "mp-show", "mp-set", "config", "encrypt", "decrypt", OPTION_HELP, "register",
            "register-trial" };

    private static final String[] COMMANDS_NO_RUNNING_SERVER = { "mp-purge", "mp-add", "mp-install", "mp-uninstall",
            "mp-request", "mp-remove", "mp-hotfix", "mp-upgrade", "mp-reset", "mp-update", "mp-set" };

    /**
     * @since 7.4
     */
    protected boolean commandRequiresNoRunningServer() {
        return Arrays.asList(COMMANDS_NO_RUNNING_SERVER).contains(command);
    }

    /**
     * @since 7.4
     */
    protected boolean commandRequiresNoGUI() {
        return Arrays.asList(COMMANDS_NO_GUI).contains(command);
    }

    /**
     * Program is running or service is OK.
     *
     * @since 5.7
     */
    public static final int STATUS_CODE_ON = 0;

    /**
     * Program is not running.
     *
     * @since 5.7
     */
    public static final int STATUS_CODE_OFF = 3;

    /**
     * Program or service status is unknown.
     *
     * @since 5.7
     */
    public static final int STATUS_CODE_UNKNOWN = 4;

    /** @since 5.7 */
    public static final int EXIT_CODE_OK = 0;

    /**
     * Generic or unspecified error.
     *
     * @since 5.7
     */
    public static final int EXIT_CODE_ERROR = 1;

    /**
     * Invalid or excess argument(s).
     *
     * @since 5.7
     */
    public static final int EXIT_CODE_INVALID = 2;

    /**
     * Unimplemented feature.
     *
     * @since 5.7
     */
    public static final int EXIT_CODE_UNIMPLEMENTED = 3;

    /**
     * User had insufficient privilege.
     *
     * @since 5.7
     */
    public static final int EXIT_CODE_UNAUTHORIZED = 4;

    /**
     * Program is not installed.
     *
     * @since 5.7
     */
    public static final int EXIT_CODE_NOT_INSTALLED = 5;

    /**
     * Program is not configured.
     *
     * @since 5.7
     */
    public static final int EXIT_CODE_NOT_CONFIGURED = 6;

    /**
     * Program is not running.
     *
     * @since 5.7
     */
    public static final int EXIT_CODE_NOT_RUNNING = 7;

    /** @since 11.1 */
    public static final int EXIT_CODE_CANNOT_EXECUTE = 126;

    /**
     * Launcher is changed.
     *
     * @since 10.2
     */
    public static final int EXIT_CODE_LAUNCHER_CHANGED = 128;

    private static final String OPTION_HELP_DESC_ENV = "\nENVIRONMENT VARIABLES\n"
            + "        NUXEO_HOME\t\tPath to server root directory.\n" //
            + "        NUXEO_CONF\t\tPath to {{nuxeo.conf}} file.\n" //
            + "        PATH\n" //
            + "\tJAVA\t\t\tPath to the {{java}} executable.\n" //
            + "        JAVA_HOME\t\tPath to the Java home directory. Can also be defined in {{nuxeo.conf}}.\n" //
            + "        JAVA_OPTS\t\tOptional values passed to the JVM. Can also be defined in {{nuxeo.conf}}.\n" //
            + "        REQUIRED_JAVA_VERSION\tNuxeo requirement on Java version.\n" //
            + "\nJAVA USAGE\n"
            + String.format(
                    "        java [-D%s=\"JVM options\"] [-D%s=\"/path/to/nuxeo\"] [-D%s=\"/path/to/nuxeo.conf\"]"
                            + " [-Djvmcheck=nofail] -jar \"path/to/nuxeo-launcher.jar\" \\\n"
                            + "        \t[options] <command> [command parameters]\n\n",
                    JAVA_OPTS_PROPERTY, NUXEO_HOME, NUXEO_CONF)
            + String.format("        %s\tParameters for the server JVM (default are %s).\n", JAVA_OPTS_PROPERTY,
                    JAVA_OPTS_DEFAULT)
            + String.format("        %s\t\tNuxeo server root path (default is parent of called script).\n", NUXEO_HOME)
            + String.format("        %s\t\tPath to {{%1$s}} file (default is \"$NUXEO_HOME/bin/%1$s\").\n", NUXEO_CONF)
            + "        jvmcheck\t\tIf set to \"nofail\", ignore JVM version validation errors.\n";

    private static final String OPTION_HELP_DESC_COMMANDS = "\nCOMMANDS\n" //
            + "        help\t\t\tPrint this message.\n" //
            + "        gui\t\t\tDeprecated: use '--gui' option instead.\n" //
            + "        start\t\t\tStart Nuxeo server in background, waiting for effective start. Useful for batch executions requiring the server being immediately available after the script returned.\n" //
            + "        stop\t\t\tStop any Nuxeo server started with the same {{nuxeo.conf}} file.\n" //
            + "        restart\t\t\tRestart Nuxeo server.\n" //
            + "        config\t\t\tGet and set template or global parameters.\n" //
            + "        encrypt\t\t\tOutput encrypted value for a given parameter.\n" //
            + "        decrypt\t\t\tOutput decrypted value for a given parameter.\n" //
            + "        configure\t\tConfigure Nuxeo server with parameters from {{nuxeo.conf}}.\n" //
            + "        console\t\t\tStart Nuxeo server in a console mode. Ctrl-C will stop it.\n" //
            + "        status\t\t\tPrint server running status.\n" //
            + "        startbg\t\t\tStart Nuxeo server in background, without waiting for effective start. Useful for starting Nuxeo as a service.\n" //
            + "        restartbg\t\tRestart Nuxeo server with a call to \"startbg\" after \"stop\".\n" //
            + "        pack\t\t\tBuild a static archive.\n" //
            + "        showconf\t\tDisplay the instance configuration.\n" //
            + "        mp-list\t\t\tList local Nuxeo Packages.\n" //
            + "        mp-listall\t\tList all Nuxeo Packages.\n" //
            + "        mp-init\t\t\tDeprecated: no more Nuxeo Packages locally available in the distribution.\n" //
            + "        mp-update\t\tUpdate cache of Nuxeo Packages list.\n" //
            + "        mp-add\t\t\tAdd Nuxeo Package(s) to local cache. You must provide the package file(s), name(s) or ID(s) as parameter.\n" //
            + "        mp-install\t\tRun Nuxeo Package installation. It is automatically called at startup if {{installAfterRestart.log}} file exists in data directory. Else you must provide the package file(s), name(s) or ID(s) as parameter.\n" //
            + "        mp-uninstall\t\tUninstall Nuxeo Package(s). You must provide the package name(s) or ID(s) as parameter (see \"mp-list\" command).\n" //
            + "        mp-remove\t\tRemove Nuxeo Package(s) from the local cache. You must provide the package name(s) or ID(s) as parameter (see \"mp-list\" command).\n" //
            + "        mp-reset\t\tReset all packages to DOWNLOADED state. May be useful after a manual server upgrade.\n" //
            + "        mp-set\t\t\tInstall a list of Nuxeo Packages and remove those not in the list.\n" //
            + "        mp-request\t\tInstall and uninstall Nuxeo Package(s) in one command. You must provide a *quoted* list of package names or IDs prefixed with + (install) or - (uninstall).\n" //
            + "        mp-purge\t\tUninstall and remove all packages from the local cache.\n" //
            + "        mp-hotfix\t\tInstall all the available hotfixes for the current platform but do not upgrade already installed ones (requires a registered instance).\n" //
            + "        mp-upgrade\t\tGet all the available upgrades for the Nuxeo Packages currently installed (requires a registered instance).\n" //
            + "        mp-show\t\t\tShow Nuxeo Package(s) information. You must provide the package file(s), name(s) or ID(s) as parameter.\n" //
            + "        register\t\tRegister your instance with an existing Connect account. You must provide the credentials, the project name or ID, its type and a description.\n" //
            + "        register-trial\t\tThis command is deprecated. To register for a free 30 day trial on Nuxeo Online Services, please visit https://connect.nuxeo.com/register\n" //
            + "\nThe following commands are always executed in console/headless mode (no GUI): " //
            + "\"configure\", \"mp-purge\", \"mp-add\", \"mp-install\", \"mp-uninstall\", \"mp-request\", " //
            + "\"mp-remove\", \"mp-hotfix\", \"mp-upgrade\", \"mp-reset\", \"mp-list\", \"mp-listall\", \"mp-update\", " //
            + "\"status\", \"showconf\", \"mp-show\", \"mp-set\", \"config\", \"encrypt\", \"decrypt\", \"help\".\n" //
            + "\nThe following commands cannot be executed on a running server: \"pack\", \"mp-purge\", " //
            + "\"mp-add\", \"mp-install\", \"mp-uninstall\", \"mp-request\", \"mp-remove\", \"mp-hotfix\", \"mp-upgrade\", " //
            + "\"mp-reset\".\n" //
            + "\nCommand parameters may need to be prefixed with '--' to separate them from option arguments when confusion arises.";

    private static final String OPTION_HELP_USAGE = "        nuxeoctl <command> [options] [--] [command parameters]\n\n";

    private static final String OPTION_HELP_HEADER = "SYNOPSIS\n"
            + "        nuxeoctl encrypt [--encrypt <algorithm>] [<clearValue>..] [-d [<categories>]|-q]\n"
            + "                Output encrypted value for <clearValue>.\n"
            + "                If <clearValue> is not provided, it is read from stdin.\n\n"
            + "        nuxeoctl decrypt '<cryptedValue>'.. [-d [<categories>]|-q]\n" //
            + "                Output decrypted value for <cryptedValue>. The secret key is read from stdin.\n\n"
            + "        nuxeoctl config [<key> <value>].. <key> [<value>] [--encrypt [<algorithm>]] [--set [<template>]] [-d [<categories>]|-q]\n"
            + "                Set template or global parameters.\n"
            + "                If <value> is not provided and the --set 'option' is used, then the value is read from stdin.\n\n"
            + "        nuxeoctl config [--get] <key>.. [-d [<categories>]|-q]\n"
            + "                Get value for the given key(s).\n\n"
            + "        nuxeoctl config [--get-regexp] <regexp>.. [-d [<categories>]|-q]\n"
            + "                Get value for the keys matching the given regular expression(s).\n\n"
            + "        nuxeoctl help|status|showconf [-d [<categories>]|-q]\n\n"
            + "        nuxeoctl configure [-d [<categories>]|-q|-hdw]\n\n"
            + "        nuxeoctl stop [-d [<categories>]|-q|--gui <true|false|yes|no>]\n\n"
            + "        nuxeoctl start|restart|console|startbg|restartbg [-d [<categories>]|-q|--clid <arg>|--gui <true|false|yes|no>|--lenient|-hdw]\n\n"
            + "        nuxeoctl mp-show [command parameters] [-d [<categories>]|-q|--clid <arg>|--xml|--json]\n\n"
            + "        nuxeoctl mp-list|mp-listall|mp-update [command parameters] [-d [<categories>]|-q|--clid <arg>|--relax <true|false|yes|no>|--xml|--json]\n\n"
            + "        nuxeoctl mp-reset|mp-purge|mp-hotfix|mp-upgrade [command parameters] [-d [<categories>]|-q|--clid <arg>|--xml|--json|--accept <true|false|yes|no|ask>]\n\n"
            + "        nuxeoctl mp-add|mp-install|mp-uninstall|mp-remove|mp-set|mp-request [command parameters] [-d [<categories>]|-q|--clid <arg>|--xml|--json|--nodeps|--relax <true|false|yes|no|ask>|--accept <true|false|yes|no|ask>|-s|-im]\n\n"
            + "        nuxeoctl register [<username> [<project> [<type> <description>] [<token>]]]\n"
            + "                Register an instance with Nuxeo Online Services. Token can be created at https://connect.nuxeo.com/nuxeo/site/connect/tokens\n\n"
            + "        nuxeoctl register --clid <arg>\n"
            + "                Register an instance according to the given CLID file.\n\n"
            + "        nuxeoctl register --renew [--clid <arg>]\n"
            + "                Renew an instance registration with Nuxeo Online Services.\n\n" //
            + "OPTIONS";

    private static final String OPTION_HELP_FOOTER = "\nSee online documentation \"ADMINDOC/nuxeoctl and Control Panel Usage\": https://doc.nuxeo.com/x/FwNc";

    private static final int PAGE_SIZE = 20;

    protected ConfigurationGenerator configurationGenerator;

    public final ConfigurationGenerator getConfigurationGenerator() {
        return configurationGenerator;
    }

    protected ProcessManager processManager;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(
            new DaemonThreadFactory("NuxeoProcessThread", false));

    protected String[] params;

    protected String command;

    /**
     * @since 7.4
     */
    public boolean commandIs(String aCommand) {
        return StringUtils.equalsIgnoreCase(command, aCommand);
    }

    private boolean useGui = false;

    private int status = STATUS_CODE_UNKNOWN;

    private StatusServletClient statusServletClient;

    private static boolean quiet = false;

    private static boolean debug = false;

    private static boolean strict = true;

    private boolean xmlOutput = false;

    private boolean jsonOutput = false;

    private ConnectBroker connectBroker = null;

    private String clid = null;

    private ConnectRegistrationBroker connectRegistrationBroker = null;

    private InstanceInfo info;

    CommandLine cmdLine;

    private boolean ignoreMissing = false;

    private static Map<String, NuxeoLauncherGUI> guis;

    /**
     * @since 5.5
     */
    public NuxeoLauncherGUI getGUI() {
        if (guis == null) {
            return null;
        }
        return guis.get(configurationGenerator.getNuxeoConf().toString());
    }

    /**
     * @since 5.5
     */
    public void setGUI(NuxeoLauncherGUI gui) {
        if (guis == null) {
            guis = new HashMap<>();
        }
        guis.put(configurationGenerator.getNuxeoConf().toString(), gui);
    }

    public NuxeoLauncher(ConfigurationGenerator configurationGenerator) {
        this.configurationGenerator = configurationGenerator;
        init();
    }

    /**
     * @since 5.6
     */
    public void init() {
        if (!configurationGenerator.init(true)) {
            throw new IllegalStateException("Initialization failed");
        }
        statusServletClient = new StatusServletClient(configurationGenerator);
        statusServletClient.setKey(configurationGenerator.getUserConfig().getProperty(Environment.SERVER_STATUS_KEY));
        String processRegex = "^(?!/bin/sh).*" + Pattern.quote(configurationGenerator.getNuxeoConf().getPath()) + ".*"
                + Pattern.quote(TomcatConfigurator.STARTUP_CLASS) + ".*$";
        processManager = ProcessManager.of(processRegex);
        // Set OS-specific decorations
        if (SystemUtils.IS_OS_MAC) {
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "NuxeoCtl");
        }
    }

    /**
     * Gets the Java options defined in Nuxeo configuration files, e.g. <tt>bin/nuxeo.conf</tt> and
     * <tt>bin/nuxeoctl</tt>.
     *
     * @return the Java options.
     */
    protected List<String> getJavaOptsProperty(Function<String, String> mapper) {
        return configurationGenerator.getJavaOpts(mapper);
    }

    /**
     * Check if some server is already running (from another thread) and throw a Runtime exception if it finds one.
     */
    public void checkNoRunningServer() throws IllegalStateException {
        try {
            processManager.findPid().ifPresent(pid -> {
                throw new NuxeoLauncherException("Cannot execute command. A server is running with process ID " + pid,
                        EXIT_CODE_CANNOT_EXECUTE);
            });
        } catch (IOException e) {
            log.warn("Could not check existing process: {}", e::getMessage);
        }
    }

    /**
     * Will wrap, if necessary, the command within a Shell command
     *
     * @param roughCommand Java command which will be run
     * @return wrapped command depending on the OS
     */
    private List<String> getOSCommand(List<String> roughCommand) {
        if (SystemUtils.IS_OS_UNIX) {
            return getUnixCommand(roughCommand);
        } else if (SystemUtils.IS_OS_WINDOWS) {
            return getWindowsCommand(roughCommand);
        }
        throw new NuxeoLauncherException("Unknown os, can't launch server", EXIT_CODE_CANNOT_EXECUTE);
    }

    private List<String> getWindowsCommand(List<String> roughCommand) {
        return roughCommand.stream().filter(StringUtils::isNotBlank).map(s -> '"' + s + '"').collect(toList());
    }

    private List<String> getUnixCommand(List<String> roughCommand) {
        // linearize command
        String linearizedCommand = roughCommand.stream()
                                               .filter(StringUtils::isNotBlank)
                                               .map(s -> s.replaceAll(" ", "\\\\ "))
                                               .reduce("exec ", (s1, s2) -> s1 + ' ' + s2);
        return List.of("/bin/sh", "-c", linearizedCommand);
    }

    protected Collection<? extends String> getServerProperties() {
        File home = configurationGenerator.getNuxeoHome();
        return List.of(formatPropertyToCommandLine("catalina.base", home.getPath()),
                formatPropertyToCommandLine("catalina.home", home.getPath()));
    }

    private File getJavaExecutable() {
        return Path.of(System.getProperty("java.home")).resolve("bin").resolve("java").toFile();
    }

    protected String getClassPath() {
        File binDir = configurationGenerator.getNuxeoBinDir();
        String cp = ".";
        cp = addToClassPath(cp, "nxserver" + File.separator + "lib");
        cp = addToClassPath(cp, getBinJarName(binDir, ConfigurationGenerator.BOOTSTRAP_JAR_REGEX));
        // since Tomcat 7, we need tomcat-juli.jar for bootstrap as well
        cp = addToClassPath(cp, getBinJarName(binDir, ConfigurationGenerator.JULI_JAR_REGEX));
        return cp;
    }

    protected String getBinJarName(File binDir, String pattern) {
        File[] binJarFiles = ConfigurationGenerator.getJarFilesFromPattern(binDir, pattern);
        if (binJarFiles.length != 1) {
            throw new RuntimeException("There should be only 1 file but " + binJarFiles.length + " were found in "
                    + binDir.getAbsolutePath() + " looking for " + pattern);
        }
        return binDir.getName() + File.separator + binJarFiles[0].getName();
    }

    protected Collection<String> getNuxeoProperties() {
        List<String> nuxeoProperties = new ArrayList<>();
        nuxeoProperties.add(formatPropertyToCommandLine(NUXEO_HOME, configurationGenerator.getNuxeoHome().getPath()));
        nuxeoProperties.add(formatPropertyToCommandLine(NUXEO_CONF, configurationGenerator.getNuxeoConf().getPath()));
        nuxeoProperties.add(formatNuxeoPropertyToCommandLine(NUXEO_LOG_DIR));
        nuxeoProperties.add(formatNuxeoPropertyToCommandLine(NUXEO_DATA_DIR));
        nuxeoProperties.add(formatNuxeoPropertyToCommandLine(NUXEO_TMP_DIR));
        nuxeoProperties.add(formatNuxeoPropertyToCommandLine(NUXEO_MP_DIR));
        if (!DEFAULT_NUXEO_CONTEXT_PATH.equals(
                configurationGenerator.getUserConfig().getProperty(NUXEO_CONTEXT_PATH))) {
            nuxeoProperties.add(formatNuxeoPropertyToCommandLine(NUXEO_CONTEXT_PATH));
        }
        if (overrideJavaTmpDir) {
            nuxeoProperties.add(formatPropertyToCommandLine("java.io.tmpdir",
                    configurationGenerator.getUserConfig().getProperty(NUXEO_TMP_DIR)));
        }
        return nuxeoProperties;
    }

    private String formatNuxeoPropertyToCommandLine(String property) {
        return formatPropertyToCommandLine(property, configurationGenerator.getUserConfig().getProperty(property));
    }

    protected static String formatPropertyToCommandLine(String key, String value) {
        return String.format("-D%s=%s", key, value);
    }

    protected String addToClassPath(String cp, String filename) {
        File classPathEntry = new File(configurationGenerator.getNuxeoHome(), filename);
        if (!classPathEntry.exists()) {
            classPathEntry = new File(filename);
        }
        if (!classPathEntry.exists()) {
            throw new RuntimeException("Tried to add nonexistent classpath entry: " + filename);
        }
        cp += System.getProperty("path.separator") + classPathEntry.getPath();
        return cp;
    }

    /**
     * @since 5.6
     */
    protected static Options initParserOptions() {
        Options options = new Options();
        // help option
        options.addOption(Option.builder("h").longOpt(OPTION_HELP).desc(OPTION_HELP_DESC).build());
        // Quiet option
        options.addOption(Option.builder("q").longOpt(OPTION_QUIET).desc(OPTION_QUIET_DESC).build());
        { // Debug options (mutually exclusive)
            OptionGroup debugOptions = new OptionGroup();
            // Debug option
            debugOptions.addOption(Option.builder("d")
                                         .longOpt(OPTION_DEBUG)
                                         .desc(OPTION_DEBUG_DESC)
                                         .hasArgs()
                                         .argName(OPTION_DEBUG_CATEGORY_ARG_NAME)
                                         .optionalArg(true)
                                         .valueSeparator(',')
                                         .build());
            // Debug category option
            debugOptions.addOption(Option.builder(OPTION_DEBUG_CATEGORY)
                                         .desc(OPTION_DEBUG_CATEGORY_DESC)
                                         .hasArgs()
                                         .argName(OPTION_DEBUG_CATEGORY_ARG_NAME)
                                         .optionalArg(true)
                                         .valueSeparator(',')
                                         .build());
            options.addOptionGroup(debugOptions);
        }
        // For help output purpose only: that option is managed and
        // swallowed by the nuxeoctl Shell script
        options.addOption(Option.builder()
                                .longOpt("debug-launcher")
                                .desc("Linux-only. Activate Java debugging mode on the Launcher.")
                                .build());
        // Instance CLID option
        options.addOption(Option.builder().longOpt(OPTION_CLID).desc(OPTION_CLID_DESC).hasArg().build());
        // Register offline option
        options.addOption(Option.builder().longOpt(OPTION_OFFLINE).desc(OPTION_OFFLINE_DESC).build());
        // Register renew option
        options.addOption(Option.builder().longOpt(OPTION_RENEW).desc(OPTION_RENEW_DESC).build());
        { // Output options (mutually exclusive)
            OptionGroup outputOptions = new OptionGroup();
            // XML option
            outputOptions.addOption(Option.builder().longOpt(OPTION_XML).desc(OPTION_XML_DESC).build());
            // JSON option
            outputOptions.addOption(Option.builder().longOpt(OPTION_JSON).desc(OPTION_JSON_DESC).build());
            options.addOptionGroup(outputOptions);
        }
        // GUI option
        options.addOption(Option.builder()
                                .longOpt(OPTION_GUI)
                                .desc(OPTION_GUI_DESC)
                                .hasArg()
                                .argName("true|false|yes|no")
                                .build());
        // Package management option
        options.addOption(Option.builder().longOpt(OPTION_NODEPS).desc(OPTION_NODEPS_DESC).build());
        // Relax on target platform option
        options.addOption(Option.builder()
                                .longOpt(OPTION_RELAX)
                                .desc(OPTION_RELAX_DESC)
                                .hasArg()
                                .argName("true|false|yes|no|ask")
                                .build());
        // Accept option
        options.addOption(Option.builder()
                                .longOpt(OPTION_ACCEPT)
                                .desc(OPTION_ACCEPT_DESC)
                                .hasArg()
                                .argName("true|false|yes|no|ask")
                                .build());
        // Allow SNAPSHOT option
        options.addOption(Option.builder("s").longOpt(OPTION_SNAPSHOT).desc(OPTION_SNAPSHOT_DESC).build());
        // Force option
        options.addOption(Option.builder("f").longOpt(OPTION_FORCE).desc(OPTION_FORCE_DESC).build());
        // Strict option
        options.addOption(Option.builder().longOpt(OPTION_STRICT).desc(OPTION_STRICT_DESC).build());
        // lenient option
        options.addOption(Option.builder().longOpt(OPTION_LENIENT).desc(OPTION_LENIENT_DESC).build());

        // Ignore missing option
        options.addOption(Option.builder("im").longOpt(OPTION_IGNORE_MISSING).desc(OPTION_IGNORE_MISSING_DESC).build());
        // Hide deprecation warnings option
        options.addOption(
                Option.builder("hdw").longOpt(OPTION_HIDE_DEPRECATION).desc(OPTION_HIDE_DEPRECATION_DESC).build());
        // Encrypt option
        options.addOption(Option.builder()
                                .longOpt(OPTION_ENCRYPT)
                                .desc(OPTION_ENCRYPT_DESC)
                                .hasArg()
                                .argName(OPTION_ENCRYPT_ARG_NAME)
                                .optionalArg(true)
                                .build());
        // Output options
        options.addOption(Option.builder()
                                .longOpt(OPTION_GZIP_OUTPUT)
                                .desc(OPTION_GZIP_DESC)
                                .hasArg()
                                .argName("true|false")
                                .optionalArg(true)
                                .build());
        options.addOption(Option.builder()
                                .longOpt(OPTION_PRETTY_PRINT)
                                .desc(OPTION_PRETTY_PRINT_DESC)
                                .hasArg()
                                .argName("true|false")
                                .optionalArg(true)
                                .build());
        options.addOption(Option.builder()
                                .longOpt(OPTION_OUTPUT)
                                .desc(OPTION_OUTPUT_DESC)
                                .hasArg()
                                .argName("file")
                                .optionalArg(true)
                                .build());
        { // Config options (mutually exclusive)
            OptionGroup configOptions = new OptionGroup();
            // Set option
            configOptions.addOption(Option.builder()
                                          .longOpt(OPTION_SET)
                                          .desc(OPTION_SET_DESC)
                                          .hasArg()
                                          .argName(OPTION_SET_ARG_NAME)
                                          .optionalArg(true)
                                          .build());
            configOptions.addOption(Option.builder().longOpt(OPTION_GET).desc(OPTION_GET_DESC).build());
            configOptions.addOption(Option.builder().longOpt(OPTION_GET_REGEXP).desc(OPTION_GET_REGEXP_DESC).build());
            options.addOptionGroup(configOptions);
        }
        return options;
    }

    /**
     * @since 5.6
     */
    protected static CommandLine parseOptions(String[] args) throws ParseException {
        CommandLineParser parser = new DefaultParser();
        CommandLine cmdLine = parser.parse(options, args);
        if (cmdLine.hasOption(OPTION_HELP)) {
            cmdLine.getArgList().add(OPTION_HELP);
            setQuiet();
        } else if (cmdLine.getArgList().isEmpty()) {
            throw new ParseException("Missing command.");
        }
        // Common options to the Launcher and the ConfigurationGenerator
        if (cmdLine.hasOption(OPTION_QUIET) || cmdLine.hasOption(OPTION_XML) || cmdLine.hasOption(OPTION_JSON)) {
            setQuiet();
        }
        if (cmdLine.hasOption(OPTION_DEBUG)) {
            setDebug(cmdLine.getOptionValues(OPTION_DEBUG));
        }
        if (cmdLine.hasOption(OPTION_DEBUG_CATEGORY)) {
            setDebug(cmdLine.getOptionValues(OPTION_DEBUG_CATEGORY));
        }
        if (cmdLine.hasOption(OPTION_FORCE) || cmdLine.hasOption(OPTION_STRICT)) {
            log.warn("--force and --strict have no impact, Nuxeo is started in strict mode by default.");
        }
        if (cmdLine.hasOption(OPTION_LENIENT)) {
            relaxStrict();
        }
        return cmdLine;
    }

    public static void main(String[] args) {
        NuxeoLauncher launcher = null;
        try {
            launcher = createLauncher(args);
            if (launcher.commandRequiresNoGUI()) {
                launcher.useGui = false;
            }
            if (launcher.useGui && launcher.getGUI() == null) {
                launcher.setGUI(new NuxeoLauncherGUI(launcher));
            }
            launch(launcher);
        } catch (LauncherRestartException e) {
            log.info("Restarting launcher...");
            System.exit(EXIT_CODE_LAUNCHER_CHANGED);
        } catch (ParseException e) {
            log.error("Invalid command line: {}", e::getMessage);
            log.debug(e, e);
            printShortHelp();
            System.exit(EXIT_CODE_INVALID);
        } catch (IOException | PackageException | ConfigurationException | GeneralSecurityException e) {
            log.error(e.getMessage());
            log.debug(e, e);
            System.exit(EXIT_CODE_INVALID);
        } catch (NuxeoLauncherException e) {
            log.error(e.getMessage());
            log.debug(e, e);
            System.exit(e.getExitCode());
        } catch (Exception e) {
            log.error("Cannot execute command. {}", e.getMessage(), e);
            log.debug(e, e);
            System.exit(EXIT_CODE_ERROR);
        }
    }

    /**
     * @since 5.5
     */
    public static void launch(final NuxeoLauncher launcher)
            throws IOException, PackageException, ConfigurationException, GeneralSecurityException {
        boolean commandSucceeded = true;
        if (launcher.commandIs(null)) {
            return;
        }
        if (launcher.commandRequiresNoRunningServer()) {
            launcher.checkNoRunningServer();
        }
        if (launcher.commandIs(OPTION_HELP)) {
            printLongHelp();
        } else if (launcher.commandIs("status")) {
            String statusMsg = launcher.status();
            if (!quiet) {
                log.warn(statusMsg);
                if (launcher.isStarted()) {
                    log.info("Go to {}", launcher::getURL);
                    log.info(launcher.getStartupSummary());
                }
            }
            // only case where exit status is not for error
            System.exit(launcher.getStatus());
        } else if (launcher.commandIs("startbg")) {
            commandSucceeded = launcher.doStart();
        } else if (launcher.commandIs("start")) {
            if (launcher.useGui) {
                launcher.getGUI().start();
            } else {
                commandSucceeded = launcher.doStartAndWait();
            }
        } else if (launcher.commandIs("console")) {
            launcher.executor.execute(launcher::doConsole);
        } else if (launcher.commandIs("stop")) {
            if (launcher.useGui) {
                launcher.getGUI().stop();
            } else {
                launcher.doStop();
            }
        } else if (launcher.commandIs("restartbg")) {
            launcher.doStop();
            commandSucceeded = launcher.doStart();
        } else if (launcher.commandIs("restart")) {
            launcher.doStop();
            commandSucceeded = launcher.doStartAndWait();
        } else if (launcher.commandIs("configure")) {
            launcher.configure();
        } else if (launcher.commandIs("mp-list")) {
            launcher.pkgList();
        } else if (launcher.commandIs("mp-listall")) {
            launcher.pkgListAll();
        } else if (launcher.commandIs("mp-init")) {
            commandSucceeded = launcher.pkgInit();
        } else if (launcher.commandIs("mp-purge")) {
            commandSucceeded = launcher.pkgPurge();
        } else if (launcher.commandIs("mp-add")) {
            if (launcher.cmdLine.hasOption(OPTION_NODEPS)) {
                commandSucceeded = launcher.pkgAdd(launcher.params);
            } else {
                commandSucceeded = launcher.pkgRequest(Arrays.asList(launcher.params), null, null, null);
            }
        } else if (launcher.commandIs("mp-install")) {
            if (launcher.cmdLine.hasOption(OPTION_NODEPS)) {
                commandSucceeded = launcher.pkgInstall(launcher.params);
            } else {
                commandSucceeded = launcher.pkgRequest(null, Arrays.asList(launcher.params), null, null);
            }
        } else if (launcher.commandIs("mp-uninstall")) {
            if (launcher.cmdLine.hasOption(OPTION_NODEPS)) {
                commandSucceeded = launcher.pkgUninstall(launcher.params);
            } else {
                commandSucceeded = launcher.pkgRequest(null, null, Arrays.asList(launcher.params), null);
            }
        } else if (launcher.commandIs("mp-remove")) {
            if (launcher.cmdLine.hasOption(OPTION_NODEPS)) {
                commandSucceeded = launcher.pkgRemove(launcher.params);
            } else {
                commandSucceeded = launcher.pkgRequest(null, null, null, Arrays.asList(launcher.params));
            }
        } else if (launcher.commandIs("mp-request")) {
            if (launcher.cmdLine.hasOption(OPTION_NODEPS)) {
                throw new NuxeoLauncherException("The command mp-request is not available with the --nodeps option",
                        EXIT_CODE_INVALID);
            } else {
                commandSucceeded = launcher.pkgCompoundRequest(Arrays.asList(launcher.params));
            }
        } else if (launcher.commandIs("mp-set")) {
            commandSucceeded = launcher.pkgSetRequest(Arrays.asList(launcher.params),
                    launcher.cmdLine.hasOption(OPTION_NODEPS));
        } else if (launcher.commandIs("mp-hotfix")) {
            commandSucceeded = launcher.pkgHotfix();
        } else if (launcher.commandIs("mp-upgrade")) {
            commandSucceeded = launcher.pkgUpgrade();
        } else if (launcher.commandIs("mp-reset")) {
            commandSucceeded = launcher.pkgReset();
        } else if (launcher.commandIs("mp-update")) {
            commandSucceeded = launcher.pkgRefreshCache();
        } else if (launcher.commandIs("showconf")) {
            launcher.showConfig();
        } else if (launcher.commandIs("mp-show")) {
            commandSucceeded = launcher.pkgShow(launcher.params);
        } else if (launcher.commandIs("encrypt")) {
            launcher.encrypt();
        } else if (launcher.commandIs("decrypt")) {
            launcher.decrypt();
        } else if (launcher.commandIs("config")) {
            launcher.config();
        } else if (launcher.commandIs("register")) {
            commandSucceeded = launcher.register();
        } else if (launcher.commandIs("register-trial")) {
            commandSucceeded = launcher.registerTrial();
        } else {
            printLongHelp();
            throw new NuxeoLauncherException(
                    "Unknown command: " + launcher.command + ", see help above or with nuxeoctl help",
                    EXIT_CODE_INVALID);
        }
        CommandSetInfo cset = launcher.connectBroker.getCommandSet();
        if (launcher.xmlOutput && launcher.command.startsWith("mp-")) {
            launcher.printXMLOutput(cset);
        }
        if (!commandSucceeded && !quiet || debug) {
            cset.log(commandSucceeded);
        }
        if (!commandSucceeded) {
            System.exit(EXIT_CODE_ERROR);
        }
    }

    /**
     * @since 8.3
     */
    public String promptDescription() throws ConfigurationException, IOException {
        return prompt("Description: ", null, null);
    }

    /**
     * Prompt for a value read from the console or stdin.
     *
     * @param message message to display at prompt
     * @param predicate a predicate that must match a correct user input. Ignored if {@code null}.
     * @param error an error message to display or raise when the user input is {@code null} or does not match the
     *            {@code regex}
     * @return the user input. Never null.
     * @throws ConfigurationException If the user input is read from stdin and is {@code null} or does not match the
     *             {@code regex}
     * @since 8.3
     */
    public String prompt(String message, Predicate<String> predicate, String error)
            throws IOException, ConfigurationException {
        boolean doRegexMatch = predicate != null;
        String value;
        Console console = System.console();
        if (console != null) {
            value = console.readLine(message);
            while (value == null || doRegexMatch && !predicate.test(value)) {
                console.printf(error + "\n", value);
                value = console.readLine(message);
            }
        } else { // try reading from stdin
            value = IOUtils.toString(System.in, UTF_8);
            if (value == null || doRegexMatch && !predicate.test(value)) {
                throw new ConfigurationException(error);
            }
        }
        return value;
    }

    /**
     * @param message message to display at prompt
     * @since 8.3
     */
    public char[] promptPassword(String message) throws IOException {
        Console console = System.console();
        if (console != null) {
            return console.readPassword(message);
        } else { // try reading from stdin
            return IOUtils.toCharArray(System.in, UTF_8);
        }
    }

    /**
     * @since 11.1
     */
    public char[] promptToken() throws IOException {
        return promptPassword("Please enter your token: ");
    }

    /**
     * @return a {@link NuxeoClientInstanceType}. Never {@code null}.
     * @since 8.3
     */
    public NuxeoClientInstanceType promptInstanceType() throws IOException, ConfigurationException {
        NuxeoClientInstanceType type;
        Console console = System.console();
        if (console == null) {
            String typeStr = IOUtils.toString(System.in, UTF_8);
            type = NuxeoClientInstanceType.fromString(typeStr);
            if (type == null) {
                throw new ConfigurationException("Unknown type: " + typeStr);
            }
            return type;
        }
        do {
            String s = console.readLine("Instance type (dev|preprod|prod): [dev] ");
            if (StringUtils.isBlank(s)) {
                type = NuxeoClientInstanceType.DEV;
            } else {
                type = NuxeoClientInstanceType.fromString(s);
            }
        } while (type == null);
        return type;
    }

    /**
     * @param projects available projects the user must choose one amongst.
     * @return a project. Never null.
     * @throws ConfigurationException If {@code projects} is empty or if there is not such a project named as the
     *             parameter read from stdin.
     * @since 8.3
     */
    public ConnectProject promptProject(@NotNull List<ConnectProject> projects)
            throws ConfigurationException, IOException {
        if (projects.isEmpty()) {
            throw new ConfigurationException("You don't have access to any project.");
        }
        if (projects.size() == 1) {
            return projects.get(0);
        }

        String projectName;
        Console console = System.console();
        if (console == null) {
            projectName = IOUtils.toString(System.in, UTF_8);
            ConnectProject project = getConnectRegistrationBroker().getProjectByName(projectName, projects);
            if (project == null) {
                throw new ConfigurationException("Unknown project: " + projectName);
            }
            return project;
        }

        System.out.println("Available projects:");
        int i = 0;
        boolean hasNextPage = true;
        while (true) {
            if (i > 0 && !SystemUtils.IS_OS_WINDOWS) {
                // Remove last line to only have projects
                System.out.print("\33[1A\33[2K");
            }

            int fromIndex = i * PAGE_SIZE;
            int toIndex = (i + 1) * PAGE_SIZE;
            if (toIndex >= projects.size()) {
                toIndex = projects.size();
                hasNextPage = false;
            }

            projects.subList(fromIndex, toIndex)
                    .forEach(project -> System.out.println("\t- " + project.getSymbolicName()));
            if (toIndex < projects.size()) {
                int pageLeft = (projects.size() - i * PAGE_SIZE + PAGE_SIZE - 1) / PAGE_SIZE;
                System.out.print(String.format("Project name (press Enter for next page; %d pages left): ", pageLeft));
            } else {
                System.out.print("Project name: ");
            }
            if (hasNextPage) {
                i++;
            }
            projectName = console.readLine();
            if (StringUtils.isNotEmpty(projectName)) {
                ConnectProject project = getConnectRegistrationBroker().getProjectByName(projectName, projects);
                if (project != null) {
                    return project;
                }
                System.err.println("Unknown project: " + projectName);
                i = 0;
                hasNextPage = true;
            }
        }
    }

    /**
     * Register the instance, generating the CLID or using the passed one; or renew a registration.
     *
     * <pre>
     * {@code
     * nuxeoctl register [<username> [<project> [<type> <description>] [token]]]
     *
     * nuxeoctl register --clid <file>
     *
     * nuxeoctl register --renew [--clid <file>]
     * }
     * </pre>
     *
     * Missing parameters are read from stdin.
     *
     * @return true if succeed
     * @since 8.3
     */
    public boolean register() throws IOException, ConfigurationException {
        // register --renew
        if (cmdLine.hasOption(OPTION_RENEW)) {
            if (params.length != 0) {
                throw new ConfigurationException("Unexpected arguments for --renew.");
            }
            return registerRenew();
        }

        // register --clid <file>
        if (cmdLine.hasOption(OPTION_CLID) && params.length == 0) {
            return registerSaveCLID();
        }

        // register --offline
        if (cmdLine.hasOption(OPTION_OFFLINE) && params.length == 0) {
            return registerOffline();
        }

        return registerRemoteInstance();
    }

    protected boolean registerRenew() throws IOException {
        try {
            getConnectRegistrationBroker().remoteRenewRegistration();
        } catch (RegistrationException e) {
            log.debug(e, e);
            return false;
        }
        log.info("Server registration renewed");
        return true;
    }

    protected boolean registerSaveCLID() throws IOException, ConfigurationException {
        // at this point the --clid option has already been processed and the file's CLID loaded
        try {
            getConnectBroker().saveCLID();
        } catch (NoCLID e) {
            throw new ConfigurationException(e);
        }
        log.info("Server registration saved");
        return true;
    }

    protected boolean registerOffline() throws IOException, ConfigurationException {
        log.info("\nTo register your instance:");
        log.info("1. Visit {}/connect/registerInstance", ConnectUrlConfig::getBaseUrl);
        log.info(
                "2. Select the project on which you want the instance to be registered and copy the technical identifier found below (CTID):\n\n{}\n",
                () -> TechnicalInstanceIdentifier.instance().getCTID());
        Date expirationDate = new Date();
        prompt("3. Enter the given identifier to register your instance (CLID): ", strCLID -> {
            try {
                getConnectRegistrationBroker().registerLocal(strCLID, "");
                long timestamp = Long.parseLong(StringUtils.substringBetween(strCLID, ".", "."));
                expirationDate.setTime(timestamp * 1000);
            } catch (IOException | ConfigurationException | NumberFormatException e) {
                return false;
            }
            return true;
        }, "This identifier is invalid or cannot be read properly or cannot be saved.");

        log.info("Server registration saved");
        log.info("Your Nuxeo Online Services is valid until {}", expirationDate);
        return true;
    }

    protected boolean registerRemoteInstance() throws IOException, ConfigurationException {
        // 0/1 param: [<username>]
        // 2/3 params: <username> <project> [token]
        // 4/5 params: <username> <project> <type> <description> [token]
        if (params.length > 5) {
            throw new ConfigurationException("Wrong number of arguments.");
        }
        String username;
        if (params.length > 0) {
            username = params[0];
        } else {
            username = prompt("Username: ", StringUtils::isNotBlank, "Username cannot be empty.");
        }
        char[] token;
        if (params.length == 3 || params.length == 5) {
            token = params[params.length - 1].toCharArray();
        } else {
            token = promptToken();
        }
        ConnectProject project;
        List<ConnectProject> projects = getConnectRegistrationBroker().getAvailableProjects(username, token);
        if (params.length > 1) {
            String projectName = params[1];
            project = getConnectRegistrationBroker().getProjectByName(projectName, projects);
            if (project == null) {
                throw new ConfigurationException("Unknown project: " + projectName);
            }
        } else {
            project = promptProject(projects);
        }
        NuxeoClientInstanceType type;
        String description;
        if (params.length > 3) {
            type = NuxeoClientInstanceType.fromString(params[2]);
            if (type == null) {
                throw new ConfigurationException("Unknown type: " + params[2]);
            }
            description = params[3];
        } else {
            type = promptInstanceType();
            description = promptDescription();
        }

        return registerRemoteInstance(username, token, project, type, description);
    }

    protected boolean registerRemoteInstance(String username, char[] token, ConnectProject project,
            NuxeoClientInstanceType type, String description) throws IOException, ConfigurationException {
        getConnectRegistrationBroker().registerRemote(username, token, project.getUuid(), type, description);
        log.info("Server registered to {} for project {}\nType: {}\nDescription: {}", username, project, type,
                description);
        return true;
    }

    /**
     * Register a trial project. The command synopsis:
     *
     * <pre>
     * <code>
     * nuxeoctl register-trial [ &lt;first&gt; &lt;last&gt; &lt;email&gt; &lt;company&gt; &lt;project&gt; ]
     * </code>
     * </pre>
     *
     * @since 8.3
     * @deprecated Since 9.3: To register for a free 30 day trial on Nuxeo Online Services, please visit
     *             https://connect.nuxeo.com/register
     */
    @Deprecated
    @SuppressWarnings("DeprecatedIsStillUsed")
    public boolean registerTrial() {
        String msg = "This command is deprecated. To register for a free 30 day trial on Nuxeo Online Services,"
                + " please visit https://connect.nuxeo.com/register";
        throw new NuxeoLauncherException(msg, EXIT_CODE_UNIMPLEMENTED);
    }

    /**
     * @since 7.4
     */
    protected void encrypt() throws GeneralSecurityException {
        Crypto crypto = configurationGenerator.getCrypto();
        String algorithm = cmdLine.getOptionValue(OPTION_ENCRYPT, null);
        if (params.length == 0) {
            Console console = System.console();
            String encryptedString;
            if (console != null) {
                encryptedString = crypto.encrypt(algorithm,
                        Crypto.getBytes(console.readPassword("Please enter the value to encrypt: ")));
            } else { // try reading from stdin
                try {
                    encryptedString = crypto.encrypt(algorithm, IOUtils.toByteArray(System.in));
                } catch (IOException e) {
                    throw new NuxeoLauncherException("Encrypt failed", EXIT_CODE_ERROR, e);
                }
            }
            log.info(encryptedString);
        } else {
            for (String strToEncrypt : params) {
                String encryptedString = crypto.encrypt(algorithm, strToEncrypt.getBytes());
                log.info(encryptedString);
            }
        }
    }

    /**
     * @since 7.4
     */
    protected void decrypt() {
        Crypto crypto = configurationGenerator.getCrypto();
        askCryptoKeyAndDecrypt(crypto, params).forEach(log::info);
    }

    protected List<String> askCryptoKeyAndDecrypt(Crypto crypto, String... values) {
        boolean validKey;
        Console console = System.console();
        if (console != null) {
            validKey = crypto.verifyKey(console.readPassword("Please enter the secret key: "));
        } else { // try reading from stdin
            try {
                validKey = crypto.verifyKey(IOUtils.toByteArray(System.in));
            } catch (IOException e) {
                throw new NuxeoLauncherException("Key verification failed", EXIT_CODE_ERROR, e);
            }
        }
        if (!validKey) {
            throw new NuxeoLauncherException("The key is not valid", EXIT_CODE_INVALID);
        }
        return Stream.of(values).map(crypto::decrypt).map(Crypto::getChars).map(String::new).collect(toList());
    }

    /**
     * @since 7.4
     */
    protected void config() throws ConfigurationException, IOException, GeneralSecurityException {
        if (cmdLine.hasOption(OPTION_SET)
                || !cmdLine.hasOption(OPTION_GET) && !cmdLine.hasOption(OPTION_GET_REGEXP) && params.length == 2) {
            setConfigProperties();
        } else { // OPTION_GET || OPTION_GET_REGEXP || !OPTION_SET &&
                 // params.length != 2
            getConfigProperties();
        }
    }

    /**
     * @since 7.4
     */
    protected void getConfigProperties() {
        boolean isRegexp = cmdLine.hasOption(OPTION_GET_REGEXP);
        CryptoProperties userConfig = configurationGenerator.getUserConfig();
        List<String> keys;
        if (isRegexp) {
            keys = new ArrayList<>();
            for (Object key : userConfig.keySet()) {
                for (String param : params) {
                    Pattern pattern = Pattern.compile(param, Pattern.CASE_INSENSITIVE);
                    if (pattern.matcher((String) key).find()) {
                        keys.add((String) key);
                    }
                }
            }
        } else {
            keys = Arrays.asList(params);
        }

        Crypto crypto = userConfig.getCrypto();
        boolean keyChecked = false; // Secret key is asked only once
        boolean raw = true;
        StringBuilder sb = new StringBuilder();
        final String newLine = System.lineSeparator();
        for (String key : keys) {
            String value = userConfig.getProperty(key, raw);
            if (value == null) {
                // TODO keep it ?
                // errorValue = EXIT_CODE_NOT_CONFIGURED;
                sb.append(OUTPUT_UNSET_VALUE).append(newLine);
            } else {
                if (raw && !keyChecked && Crypto.isEncrypted(value)) {
                    keyChecked = true;
                    List<String> decryptedValues = askCryptoKeyAndDecrypt(crypto, value);
                    if (decryptedValues != null) {
                        raw = false;
                        value = decryptedValues.get(0);
                    }
                }
                if (isRegexp) {
                    sb.append(key).append('=');
                }
                sb.append(value).append(newLine);
            }
        }
        log.info(NO_NEW_LINE, sb.toString());
    }

    /**
     * @since 7.4
     */
    protected void setConfigProperties() throws ConfigurationException, IOException, GeneralSecurityException {
        Crypto crypto = configurationGenerator.getCrypto();
        boolean doEncrypt = cmdLine.hasOption(OPTION_ENCRYPT);
        String algorithm = cmdLine.getOptionValue(OPTION_ENCRYPT, null);
        Map<String, String> changedParameters = new HashMap<>();
        for (Iterator<String> iterator = Arrays.asList(params).iterator(); iterator.hasNext();) {
            String key = iterator.next();
            String value;
            boolean isCryptKey = Environment.CRYPT_KEY.equals(key) || Environment.CRYPT_KEYSTORE_PASS.equals(key);
            if (iterator.hasNext()) {
                value = iterator.next();
                if (doEncrypt) {
                    value = crypto.encrypt(algorithm, value.getBytes());
                } else if (isCryptKey) {
                    value = Base64.encodeBase64String(value.getBytes());
                }
            } else {
                Console console = System.console();
                if (console != null) {
                    final String fmt = "Please enter the value for %s: ";
                    if (doEncrypt) {
                        value = crypto.encrypt(algorithm, Crypto.getBytes(console.readPassword(fmt, key)));
                    } else if (isCryptKey) {
                        value = Base64.encodeBase64String(Crypto.getBytes(console.readPassword(fmt, key)));
                    } else {
                        value = console.readLine(fmt, key);
                    }
                } else { // try reading from stdin
                    try {
                        if (doEncrypt) {
                            value = crypto.encrypt(algorithm, IOUtils.toByteArray(System.in));
                        } else if (isCryptKey) {
                            value = Base64.encodeBase64String(IOUtils.toByteArray(System.in));
                        } else {
                            value = IOUtils.toString(System.in, UTF_8);
                        }
                    } catch (IOException e) {
                        throw new NuxeoLauncherException("Reading from stdin failed", EXIT_CODE_ERROR, e);
                    }
                }
            }
            changedParameters.put(key, value);
        }
        String template = cmdLine.getOptionValue(OPTION_SET);
        Map<String, String> oldValues;
        if (template == null) {
            oldValues = configurationGenerator.setProperties(changedParameters);
        } else {
            oldValues = configurationGenerator.setProperties(template, changedParameters);
        }
        log.debug("Old values: {}", oldValues);
    }

    /**
     * Call {@link #doStart(boolean)} with false as parameter.
     *
     * @see #doStart(boolean)
     * @return true if the server started successfully
     */
    public boolean doStart() {
        if (doStart(false).isAlive()) {
            if (!quiet) {
                log.info("Go to {}", this::getURL);
            }
            return true;
        }
        return false;
    }

    /**
     * Whereas {@link #doStart()} considers the server as started when the process is running, this method waits for
     * effective start by calling {@link #statusServletClient}.
     *
     * @return true if the server started successfully
     */
    public boolean doStartAndWait() {
        var nuxeoProcess = doStart(false);
        if (nuxeoProcess.isAlive()) {
            // noinspection unused
            try (var hook = new ShutdownHook(this)) {
                waitForEffectiveStart(nuxeoProcess);
                if (!quiet) {
                    log.info("Go to {}", this::getURL);
                }
                return true;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
        return false;
    }

    /**
     * @since 11.1
     */
    public void doConsole() {
        var hook = new ShutdownHook(this); // NOSONAR don't close the hook because doStart is not blocking
        try {
            doStart(true).onExit().thenAccept(p -> {
                hook.close();
                // always terminate, nuxeo process is not supposed to exit
                System.exit(EXIT_CODE_ERROR);
            });
        } catch (RuntimeException e) {
            // for errors in doStart
            hook.close();
            throw e;
        }
        if (!quiet) {
            log.info("Go to {}", this::getURL);
        }
    }

    /**
     * Starts the server in background.
     *
     * @return the nuxeo process
     * @throws NuxeoLauncherException if an error occurred
     */
    protected Process doStart(boolean logProcessOutput) {
        try {
            configure();
            configurationGenerator.verifyInstallation();

            log.debug("Check if install in progress...");
            int tries = 0;
            while (configurationGenerator.isInstallInProgress()) {
                tries++;
                if (!getConnectBroker().executePending(configurationGenerator.getInstallFile(), true, true,
                        ignoreMissing) || tries > 9) {
                    throw new NuxeoLauncherException(String.format(
                            "Start interrupted due to failure on pending actions. You can resume with a new start"
                                    + " or you can restore the file '%s', optionally using the '--%s' option.",
                            configurationGenerator.getInstallFile().getName(), OPTION_IGNORE_MISSING), EXIT_CODE_ERROR);
                }
                // reload configuration
                configurationGenerator = new ConfigurationGenerator(quiet, debug);
                configurationGenerator.init();
                configure();
                configurationGenerator.verifyInstallation();
            }
            return start(logProcessOutput);
        } catch (ConfigurationException e) {
            throw new NuxeoLauncherException("Could not run configuration: " + e.getMessage(), EXIT_CODE_NOT_CONFIGURED,
                    e);
        } catch (IOException e) {
            throw new NuxeoLauncherException("Could not start process: " + e.getMessage(), EXIT_CODE_ERROR, e);
        } catch (IllegalStateException e) {
            // in strict mode assume program is not configured because of http port binding conflict for exit value
            throw new NuxeoLauncherException("Could not start process: " + e.getMessage(),
                    strict ? EXIT_CODE_NOT_CONFIGURED : EXIT_CODE_ERROR, e);
        }
    }

    /**
     * Do not directly call this method without a call to {@link #checkNoRunningServer()}
     *
     * @see #doStart()
     * @throws IOException in case of issue with process.
     */
    protected Process start(boolean logProcessOutput) throws IOException {
        // build command to start nuxeo
        List<String> startCommand = new ArrayList<>();
        startCommand.add(getJavaExecutable().getPath());
        startCommand.addAll(getJavaOptsProperty(Function.identity()));
        startCommand.add("-cp");
        startCommand.add(getClassPath());
        startCommand.addAll(getNuxeoProperties());
        startCommand.addAll(getServerProperties());
        if (strict) {
            startCommand.add("-Dnuxeo.start.strict=true");
        }

        startCommand.add(TomcatConfigurator.STARTUP_CLASS);
        startCommand.add("start");
        startCommand.addAll(Arrays.asList(params));

        // build and start process
        ProcessBuilder pb = new ProcessBuilder(getOSCommand(startCommand));
        pb.directory(configurationGenerator.getNuxeoHome());
        if (logProcessOutput) {
            // don't redirect input as we want a graceful shutdown
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT).redirectError(ProcessBuilder.Redirect.INHERIT);
        }
        log.debug("Server command: {}", pb::command);
        Process nuxeoProcess = pb.start();
        nuxeoProcess.onExit().thenAccept(p -> {
            if (SystemUtils.IS_OS_WINDOWS && configurationGenerator.getNuxeoHome().getPath().contains(" ")) {
                // NXP-17679
                log.error("The server path must not contain spaces under Windows.");
            }
            int exitValue = p.exitValue();
            if (exitValue != 0) {
                log.error("Server stopped with status: {}", exitValue);
            }
        });

        // get pid and write it to the disk for later use
        Long pid;
        try {
            pid = nuxeoProcess.pid();
        } catch (UnsupportedOperationException e) {
            log.warn("Unable to get process ID from process: {}, please report it to Nuxeo", nuxeoProcess);
            // fallback on process manager
            pid = processManager.findPid()
                                .orElseThrow(() -> new NuxeoLauncherException(
                                        "Sent server start command but could not get process ID.", EXIT_CODE_ERROR, e));
        }
        log.info("Server started with process ID: {}", pid);
        File pidFile = new File(configurationGenerator.getPidDir(), "nuxeo.pid");
        try (FileWriter writer = new FileWriter(pidFile)) {
            writer.write(Long.toString(pid));
        }
        return nuxeoProcess;
    }

    protected void waitForEffectiveStart(Process nuxeoProcess) throws InterruptedException {
        int startMaxWait = Integer.parseInt(
                configurationGenerator.getUserConfig().getProperty(START_MAX_WAIT_PARAM, START_MAX_WAIT_DEFAULT));
        var startTime = Instant.now();
        var waitUntil = startTime.plusSeconds(startMaxWait);
        log.debug("Will wait for effective start during {} seconds.", startMaxWait);
        final StringBuilder startSummary = new StringBuilder();
        // Wait for effective start reported from status servlet
        boolean servletAvailable = false;
        int n = 0;
        while (Instant.now().isBefore(waitUntil) && nuxeoProcess.isAlive()) {
            try {
                // delay will be 1s 10 times, then 2s 10 times... until reaching maximum of 1min
                Thread.sleep(Math.min((++n / 10 + 1) * 1000, 60_000));
                if (servletAvailable && statusServletClient.isStarted()) {
                    if (!quiet) {
                        log.info(".");
                    }
                    break;
                } else if (statusServletClient.init()) {
                    servletAvailable = true;
                    n = 0;
                }
            } catch (SocketTimeoutException e) {
                if (!quiet) {
                    log.info(NO_NEW_LINE, ".");
                }
            }
        }

        if (Instant.now().isAfter(waitUntil)) {
            throw new NuxeoLauncherException("Starting process is taking too long - giving up.", EXIT_CODE_ERROR);
        }
        if (!nuxeoProcess.isAlive()) {
            // Nuxeo has crashed - try to get its System.out
            String logs;
            try {
                logs = IOUtils.toString(nuxeoProcess.getInputStream(), UTF_8);
            } catch (IOException e) {
                logs = "Unable to get process output, check server logs";
            }
            throw new NuxeoLauncherException("Nuxeo startup aborted, see startup logs:" + System.lineSeparator() + logs,
                    EXIT_CODE_ERROR);
        }

        startSummary.append(getStartupSummary());
        var duration = Duration.between(startTime, Instant.now());
        startSummary.append(String.format("Started in %dmin%02ds", duration.toMinutes(), duration.toSeconds() % 60));
        if (wasStartupFine()) {
            if (!quiet) {
                log.info(startSummary);
            }
        } else {
            log.error(startSummary);
            if (strict) {
                doStop();
                throw new NuxeoLauncherException("Shutting down because of unstarted component in strict mode...",
                        EXIT_CODE_ERROR);
            }
        }
    }

    /**
     * Must be called after {@link #getStartupSummary()}
     *
     * @since 5.5
     * @return last detected status of running Nuxeo server
     */
    public boolean wasStartupFine() {
        return statusServletClient.isStartupFine();
    }

    /**
     * @since 5.5
     * @return Nuxeo startup summary
     */
    public String getStartupSummary() {
        try {
            return statusServletClient.getStartupSummary();
        } catch (SocketTimeoutException e) {
            log.warn("Failed to contact Nuxeo for getting startup summary", e);
            return "";
        }
    }

    /**
     * @since 5.6
     */
    protected void printXMLOutput(CommandSetInfo cset) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(CommandSetInfo.class, CommandInfo.class,
                    PackageInfo.class, MessageInfo.class);
            printXMLOutput(jaxbContext, cset, System.out);
        } catch (JAXBException | XMLStreamException | FactoryConfigurationError e) {
            throw new NuxeoLauncherException("Output serialization failed: " + e.getMessage(), EXIT_CODE_NOT_RUNNING,
                    e);
        }
    }

    /**
     * @since 8.3
     */
    protected void printXMLOutput(JAXBContext context, Object object, OutputStream out)
            throws XMLStreamException, FactoryConfigurationError, JAXBException {
        XMLStreamWriter writer = jsonOutput ? jsonWriter(context, out) : xmlWriter(context, out);
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.marshal(object, writer);
    }

    protected XMLStreamWriter jsonWriter(JAXBContext context, OutputStream out) {
        JSONConfiguration config = JSONConfiguration.mapped()
                                                    .rootUnwrapping(true)
                                                    .attributeAsElement("key", "value")
                                                    .build();
        config = JSONConfiguration.createJSONConfigurationWithFormatted(config, true);
        return JsonXmlStreamWriter.createWriter(new OutputStreamWriter(out), config, "");
    }

    protected XMLStreamWriter xmlWriter(JAXBContext context, OutputStream out)
            throws XMLStreamException, FactoryConfigurationError {
        return XMLOutputFactory.newInstance().createXMLStreamWriter(out);
    }

    protected static class ShutdownHook extends Thread implements AutoCloseable {

        private final NuxeoLauncher launcher;

        public ShutdownHook(NuxeoLauncher launcher) {
            super();
            log.debug("Add shutdown hook");
            this.launcher = launcher;
            Runtime.getRuntime().addShutdownHook(this);
        }

        @Override
        public void run() {
            log.info("Shutting down...");
            if (launcher.isRunning()) {
                launcher.doStop();
            }
            log.info("Shutdown complete.");
        }

        @Override
        public void close() {
            try {
                Runtime.getRuntime().removeShutdownHook(this);
                log.debug("Removed shutdown hook");
            } catch (IllegalStateException e) {
                // the virtual machine is already in the process of shutting down
            }
        }
    }

    /**
     * Stops the server. Will try to call specific class for a clean stop, retry, waiting between each try, then kill
     * the process if still running.
     */
    public void doStop() {
        try {
            var nuxeoProcessOpt = processManager.findPid().flatMap(ProcessHandle::of);
            if (nuxeoProcessOpt.isEmpty()) {
                log.warn("Server is not running.");
                return;
            }
            if (!quiet) {
                log.info(NO_NEW_LINE, "Stopping server...");
            }
            int stopMaxWait = Integer.parseInt(
                    configurationGenerator.getUserConfig().getProperty(STOP_MAX_WAIT_PARAM, STOP_MAX_WAIT_DEFAULT));
            var startTime = Instant.now();
            var waitUntil = startTime.plusSeconds(stopMaxWait);
            var nuxeoProcess = nuxeoProcessOpt.get();
            while (Instant.now().isBefore(waitUntil) && nuxeoProcess.isAlive()) {
                Process stopProcess = stop();
                stopProcess.waitFor();
                // at this point Tomcat has received and acknowledged the stop command
                if (!quiet) {
                    log.info(NO_NEW_LINE, ".");
                }
                // don't send too many requests to Tomcat - we're going to re-try
                Thread.sleep(1000);
            }
            log.info(".");
            if (!nuxeoProcess.isAlive()) {
                Duration duration = Duration.between(startTime, Instant.now());
                log.info(String.format("Stopped in %dmin%02ds", duration.toMinutes(), duration.toSeconds() % 60));
            } else if (Instant.now().isAfter(waitUntil)) {
                log.info("No answer from server, try to kill process {}...", nuxeoProcess::pid);
                processManager.kill(nuxeoProcess);
                if (!nuxeoProcess.isAlive()) {
                    log.warn("Server forcibly stopped.");
                }
            }
        } catch (IOException e) {
            throw new NuxeoLauncherException("Error during process execution: " + e.getMessage(), EXIT_CODE_ERROR, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    protected Process stop() throws IOException {
        // build command to stop nuxeo
        List<String> stopCommand = new ArrayList<>();
        stopCommand.add(getJavaExecutable().getPath());
        stopCommand.add("-cp");
        stopCommand.add(getClassPath());
        stopCommand.addAll(getNuxeoProperties());
        stopCommand.addAll(getServerProperties());
        stopCommand.add(TomcatConfigurator.STARTUP_CLASS);
        stopCommand.add("stop");
        stopCommand.addAll(Arrays.asList(params));
        ProcessBuilder pb = new ProcessBuilder(getOSCommand(stopCommand));
        pb.directory(configurationGenerator.getNuxeoHome());
        log.debug("Server command: {}", pb::command);
        return pb.start();
    }

    /**
     * Configure the server after checking installation
     */
    public void configure() {
        try {
            checkNoRunningServer();
            configurationGenerator.checkJavaVersion();
            configurationGenerator.run();
            overrideJavaTmpDir = Boolean.parseBoolean(
                    configurationGenerator.getUserConfig().getProperty(OVERRIDE_JAVA_TMPDIR_PARAM, "true"));
        } catch (ConfigurationException e) {
            throw new NuxeoLauncherException("Could not run configuration: " + e.getMessage(), EXIT_CODE_NOT_CONFIGURED,
                    e);
        }
    }

    /**
     * Return process status (running or not) as String, depending on OS capability to manage processes. Set status
     * value following "http://refspecs.freestandards.org/LSB_4.1.0/LSB-Core-generic/LSB-Core- generic/iniscrptact.html"
     *
     * @see #getStatus()
     */
    public String status() {
        try {
            if (ProcessManager.class.equals(processManager.getClass())) {
                status = STATUS_CODE_UNKNOWN;
                return "Can't check server status on your OS.";
            }
            var pidOpt = processManager.findPid();
            if (pidOpt.isEmpty()) {
                status = STATUS_CODE_OFF;
                return "Server is not running.";
            } else {
                status = STATUS_CODE_ON;
                return "Server is running with process ID " + pidOpt.get() + ".";
            }
        } catch (IOException e) {
            status = STATUS_CODE_UNKNOWN;
            return "Could not check existing process (" + e.getMessage() + ").";
        }
    }

    /**
     * Last status value set by {@link #status()}.
     */
    public int getStatus() {
        return status;
    }

    /**
     * @return a NuxeoLauncher instance specific to current server ( Tomcat or Jetty).
     * @since 5.5
     */
    public static NuxeoLauncher createLauncher(String[] args) throws ParseException, IOException, PackageException {
        CommandLine cmdLine = parseOptions(args);
        ConfigurationGenerator cg = new ConfigurationGenerator(quiet, debug);
        if (cmdLine.hasOption(OPTION_HIDE_DEPRECATION)) {
            cg.hideDeprecationWarnings(true);
        }
        NuxeoLauncher launcher = new NuxeoLauncher(cg);
        launcher.connectBroker = new ConnectBroker(launcher.configurationGenerator.getEnv());
        launcher.setArgs(cmdLine);
        launcher.initConnectBroker();
        return launcher;
    }

    /**
     * Sets from program arguments the launcher command and additional parameters.
     *
     * @param cmdLine Program arguments; may be used by launcher implementation. Must not be null or empty.
     */
    private void setArgs(CommandLine cmdLine) {
        this.cmdLine = cmdLine;
        extractCommandAndParams(cmdLine.getArgs());
        // Use GUI?
        if (cmdLine.hasOption(OPTION_GUI)) {
            useGui = Boolean.parseBoolean(ConnectBroker.parseAnswer(cmdLine.getOptionValue(OPTION_GUI)));
            log.debug("GUI: {} -> {}", () -> cmdLine.getOptionValue(OPTION_GUI), () -> useGui);
        } else if (OPTION_GUI.equalsIgnoreCase(command)) {
            useGui = true;
            // Shift params and extract command if there is one
            extractCommandAndParams(params);
        } else {
            if (SystemUtils.IS_OS_WINDOWS) {
                useGui = true;
                log.debug("GUI: option not set - platform is Windows -> start GUI");
            } else {
                useGui = false;
                log.debug("GUI: option not set - platform is not Windows -> do not start GUI");
            }
        }
        // Output format
        if (cmdLine.hasOption(OPTION_XML)) {
            setXMLOutput();
        }
        if (cmdLine.hasOption(OPTION_JSON)) {
            setJSONOutput();
        }
        if (cmdLine.hasOption(OPTION_CLID)) {
            try {
                getConnectBroker().setCLID(cmdLine.getOptionValue(OPTION_CLID));
            } catch (NoCLID e) {
                throw new NuxeoLauncherException("CLID is invalid", EXIT_CODE_UNAUTHORIZED, e);
            }
        }
        if (cmdLine.hasOption(OPTION_IGNORE_MISSING)) {
            ignoreMissing = true;
        }
    }

    private void extractCommandAndParams(String[] args) {
        if (args.length > 0) {
            command = args[0];
            log.debug("Launcher command: {}", command);
            // Command parameters
            if (args.length > 1) {
                params = Arrays.copyOfRange(args, 1, args.length);
                log.debug("Command parameters: {}", () -> ArrayUtils.toString(params));
            } else {
                params = new String[0];
            }
        } else {
            command = null;
        }
    }

    /**
     * Set launcher in quiet mode
     *
     * @since 5.5
     */
    protected static void setQuiet() {
        quiet = true;
        Log4JHelper.setLevel(new String[] { ROOT_LOGGER_NAME }, Level.WARN, true);
    }

    /**
     * @param loggerNames the loggers names to switch DEBUG on
     * @since 7.4
     */
    protected static void setDebug(String[] loggerNames) {
        debug = true;
        if (loggerNames == null) {
            loggerNames = new String[] { "org.nuxeo.launcher" };
        }
        Log4JHelper.setLevel(loggerNames, Level.DEBUG, true);
    }

    /**
     * Relaxes the launcher strict option (ie: lenient mode).
     *
     * @since 11.1
     */
    protected static void relaxStrict() {
        NuxeoLauncher.strict = false;
    }

    protected void setXMLOutput() {
        xmlOutput = true;
    }

    protected void setJSONOutput() {
        jsonOutput = true;
        setXMLOutput();
    }

    public static void printShortHelp() {
        System.out.println();
        HelpFormatter help = new HelpFormatter();
        help.setSyntaxPrefix("USAGE\n");
        help.setOptionComparator(null);
        help.setWidth(1000);
        help.printHelp(OPTION_HELP_USAGE, "OPTIONS", options, null);
        System.out.println(OPTION_HELP_DESC_COMMANDS);
    }

    public static void printLongHelp() {
        System.out.println();
        HelpFormatter help = new HelpFormatter();
        help.setSyntaxPrefix("USAGE\n");
        help.setOptionComparator(null);
        help.setWidth(1000);
        help.printHelp(OPTION_HELP_USAGE, OPTION_HELP_HEADER, options, null);
        System.out.println(OPTION_HELP_DESC_ENV);
        System.out.println(OPTION_HELP_DESC_COMMANDS);
        System.out.println(OPTION_HELP_FOOTER);
    }

    /**
     * @return true if a running PID is found
     */
    public boolean isRunning() {
        try {
            return processManager.findPid().isPresent();
        } catch (IOException e) {
            log.error(e);
            return false;
        }
    }

    /**
     * Provides this instance info
     *
     * @since 8.3
     */
    public InstanceInfo getInfo() {
        return info;
    }

    /**
     * @since 5.5
     * @return true if Nuxeo finished starting
     */
    public boolean isStarted() {
        boolean isStarted;
        try {
            isStarted = isRunning() && statusServletClient.isStarted();
        } catch (SocketTimeoutException e) {
            isStarted = false;
        }
        return isStarted;
    }

    /**
     * @return Server URL
     */
    public String getURL() {
        return configurationGenerator.getUserConfig().getProperty(ConfigurationGenerator.PARAM_NUXEO_URL);
    }

    protected void initConnectBroker() {
        if (cmdLine.hasOption(OPTION_ACCEPT)) {
            connectBroker.setAccept(cmdLine.getOptionValue(OPTION_ACCEPT));
        }
        if (cmdLine.hasOption(OPTION_RELAX)) {
            connectBroker.setRelax(cmdLine.getOptionValue(OPTION_RELAX));
        }
        if (cmdLine.hasOption(OPTION_SNAPSHOT)) {
            connectBroker.setAllowSNAPSHOT(true);
        }
        try {
            clid = connectBroker.getCLID();
        } catch (NoCLID cause) {
            // optional
        }
        info = configurationGenerator.getServerConfigurator().getInfo(clid, connectBroker.getPkgList());
        if (new Version(info.distribution.version).isSnapshot()) {
            connectBroker.setAllowSNAPSHOT(true);
        }
        connectBroker.setPendingFile(configurationGenerator.getInstallFile().toPath());
    }

    protected ConnectBroker getConnectBroker() {
        return connectBroker;
    }

    protected ConnectRegistrationBroker getConnectRegistrationBroker() {
        if (connectRegistrationBroker == null) {
            connectRegistrationBroker = new ConnectRegistrationBroker();
        }
        return connectRegistrationBroker;
    }

    /**
     * List all local packages.
     */
    protected void pkgList() {
        getConnectBroker().listPending(configurationGenerator.getInstallFile());
        getConnectBroker().pkgList();
    }

    /**
     * List all packages including remote ones.
     *
     * @since 5.6
     */
    protected void pkgListAll() {
        getConnectBroker().listPending(configurationGenerator.getInstallFile());
        getConnectBroker().pkgListAll();
    }

    protected boolean pkgAdd(String[] pkgNames) {
        return getConnectBroker().pkgAdd(Arrays.asList(pkgNames), ignoreMissing);
    }

    protected boolean pkgInstall(String[] pkgIDs) {
        boolean cmdOK = true;
        if (configurationGenerator.isInstallInProgress()) {
            cmdOK = getConnectBroker().executePending(configurationGenerator.getInstallFile(), true,
                    !cmdLine.hasOption(OPTION_NODEPS), ignoreMissing);
        }
        return cmdOK && getConnectBroker().pkgInstall(Arrays.asList(pkgIDs), ignoreMissing);
    }

    protected boolean pkgUninstall(String[] pkgIDs) {
        return getConnectBroker().pkgUninstall(Arrays.asList(pkgIDs));
    }

    protected boolean pkgRemove(String[] pkgIDs) {
        return getConnectBroker().pkgRemove(Arrays.asList(pkgIDs));
    }

    protected boolean pkgReset() {
        return getConnectBroker().pkgReset();
    }

    /**
     * @since 5.6
     */
    protected void printInstanceXMLOutput(InstanceInfo instance) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(InstanceInfo.class, DistributionInfo.class,
                    PackageInfo.class, ConfigurationInfo.class, KeyValueInfo.class);
            printXMLOutput(jaxbContext, instance, System.out);
        } catch (JAXBException | XMLStreamException | FactoryConfigurationError e) {
            throw new NuxeoLauncherException("Output serialization failed: " + e.getMessage(), EXIT_CODE_NOT_RUNNING,
                    e);
        }
    }

    /**
     * @since 5.6
     */
    protected void showConfig() {
        log.info("***** Nuxeo instance configuration *****");
        log.info("NUXEO_CONF: {}", info.NUXEO_CONF);
        log.info("NUXEO_HOME: {}", info.NUXEO_HOME);
        if (info.clid != null) {
            log.info("Instance CLID: {}", info.clid);
        }
        // distribution.properties
        log.info("** Distribution");
        log.info("- name: {}", info.distribution.name);
        log.info("- server: {}", info.distribution.server);
        log.info("- version: {}", info.distribution.version);
        log.info("- date: {}", info.distribution.date);
        log.info("- packaging: {}", info.distribution.packaging);
        // packages
        log.info("** Packages:");
        for (PackageInfo pkg : info.packages) {
            log.info("- {} (version: {} - id: {}} - state: {})", pkg.name, pkg.version, pkg.id, pkg.state.getLabel());
        }
        // nuxeo.conf
        log.info("** Templates:");
        log.info("Database template: {}", info.config.dbtemplate);
        for (String template : info.config.pkgtemplates) {
            log.info("Package template: {}", template);
        }
        for (String template : info.config.usertemplates) {
            log.info("User template: {}", template);
        }
        for (String template : info.config.basetemplates) {
            log.info("Base template: {}", template);
        }
        log.info("** Settings from nuxeo.conf:");
        for (KeyValueInfo keyval : info.config.keyvals) {
            log.info("{}={}", keyval.key, keyval.value);
        }
        log.info("****************************************");
        if (xmlOutput) {
            printInstanceXMLOutput(info);
        }
    }

    /**
     * @since 5.6
     * @return true if request execution was fine
     */
    protected boolean pkgRequest(List<String> pkgsToAdd, List<String> pkgsToInstall, List<String> pkgsToUninstall,
            List<String> pkgsToRemove) {
        boolean cmdOK = true;
        if (configurationGenerator.isInstallInProgress()) {
            cmdOK = getConnectBroker().executePending(configurationGenerator.getInstallFile(), true, true,
                    ignoreMissing);
        }
        return cmdOK && getConnectBroker().pkgRequest(pkgsToAdd, pkgsToInstall, pkgsToUninstall, pkgsToRemove, true,
                ignoreMissing);
    }

    /**
     * Update the cached list of remote packages
     *
     * @since 5.6
     * @return true
     */
    protected boolean pkgRefreshCache() {
        getConnectBroker().refreshCache();
        return true;
    }

    /**
     * Add packages from the distribution to the local cache
     *
     * @since 5.6
     */
    protected boolean pkgInit() {
        log.warn("The 'mp-init' command is deprecated, no more Nuxeo Packages locally available in the distribution.");
        return true;
    }

    /**
     * Uninstall and remove all packages from the local cache
     *
     * @return {@code true} if command succeed
     * @since 5.6
     */
    protected boolean pkgPurge() throws PackageException {
        return getConnectBroker().pkgPurge();
    }

    /**
     * Install the hotfixes available for the instance
     *
     * @return {@code true} if command succeed
     * @since 5.6
     */
    protected boolean pkgHotfix() {
        return getConnectBroker().pkgHotfix();
    }

    /**
     * Upgrade the Nuxeo Packages (addons) available for the instance
     *
     * @return {@code true} if command succeed
     * @since 5.6
     */
    protected boolean pkgUpgrade() {
        return getConnectBroker().pkgUpgrade();
    }

    /**
     * Combined install/uninstall request
     *
     * @param request Space separated list of package names or IDs prefixed with + (install) or - (uninstall)
     * @since 5.6
     */
    protected boolean pkgCompoundRequest(List<String> request) {
        List<String> add = new ArrayList<>();
        List<String> install = new ArrayList<>();
        List<String> uninstall = new ArrayList<>();
        for (String param : request) {
            for (String subparam : param.split("[ ,]")) {
                if (subparam.charAt(0) == '-') {
                    uninstall.add(subparam.substring(1));
                } else if (subparam.charAt(0) == '+') {
                    install.add(subparam.substring(1));
                } else {
                    add.add(subparam);
                }
            }
        }
        return pkgRequest(add, install, uninstall, null);
    }

    protected boolean pkgSetRequest(List<String> request, boolean nodeps) {
        boolean cmdOK;
        if (nodeps) {
            cmdOK = getConnectBroker().pkgSet(request, ignoreMissing);
        } else {
            cmdOK = getConnectBroker().pkgRequest(null, request, null, null, false, ignoreMissing);
        }
        return cmdOK;
    }

    /**
     * dpkg-like command which returns package location, version, dependencies, conflicts, ...
     *
     * @param packages List of packages identified by their ID, name or local filename.
     * @return false if unable to show package information.
     * @since 5.7
     */
    protected boolean pkgShow(String[] packages) {
        return getConnectBroker().pkgShow(Arrays.asList(packages));
    }
}
