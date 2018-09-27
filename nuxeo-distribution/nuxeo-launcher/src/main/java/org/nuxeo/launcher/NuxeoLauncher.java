/*
 * (C) Copyright 2010-2018 Nuxeo (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.launcher;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.logging.log4j.LogManager.ROOT_LOGGER_NAME;

import java.io.Console;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

import javax.json.Json;
import javax.json.stream.JsonGenerator;
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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.SimpleLog;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.logging.log4j.Level;
import org.nuxeo.common.Environment;
import org.nuxeo.common.codec.Crypto;
import org.nuxeo.common.codec.CryptoProperties;
import org.nuxeo.connect.connector.NuxeoClientInstanceType;
import org.nuxeo.connect.data.ConnectProject;
import org.nuxeo.connect.identity.LogicalInstanceIdentifier.NoCLID;
import org.nuxeo.connect.registration.RegistrationException;
import org.nuxeo.connect.tools.report.client.ReportConnector;
import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.Version;
import org.nuxeo.launcher.config.ConfigurationException;
import org.nuxeo.launcher.config.ConfigurationGenerator;
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
import org.nuxeo.launcher.process.MacProcessManager;
import org.nuxeo.launcher.process.ProcessManager;
import org.nuxeo.launcher.process.PureJavaProcessManager;
import org.nuxeo.launcher.process.SolarisProcessManager;
import org.nuxeo.launcher.process.UnixProcessManager;
import org.nuxeo.launcher.process.WindowsProcessManager;
import org.nuxeo.log4j.Log4JHelper;
import org.nuxeo.log4j.ThreadedStreamGobbler;

import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.json.impl.writer.JsonXmlStreamWriter;

/**
 * @author jcarsique
 * @since 5.4.2
 */
public abstract class NuxeoLauncher {

    /**
     * @since 7.4
     */
    protected static final String OUTPUT_UNSET_VALUE = "<unset>";

    /**
     * @since 5.6
     */
    protected static final String OPTION_NODEPS = "nodeps";

    private static final String OPTION_NODEPS_DESC = "Ignore package dependencies and constraints.";

    /**
     * @since 5.6
     */
    protected static final String OPTION_GUI = "gui";

    private static final String OPTION_GUI_DESC = "Start graphical user interface (default is true on Windows and false on other platforms).";

    /**
     * @since 5.6
     */
    protected static final String OPTION_JSON = "json";

    private static final String OPTION_JSON_DESC = "Output JSON for mp-* commands.";

    /**
     * @since 5.6
     */
    protected static final String OPTION_XML = "xml";

    private static final String OPTION_XML_DESC = "Output XML for mp-* commands.";

    /**
     * @since 5.6
     */
    protected static final String OPTION_DEBUG = "debug";

    private static final String OPTION_DEBUG_DESC = "Activate debug messages.\n"
            + "<categories>: comma-separated Java categories to debug (default: \"org.nuxeo.launcher\").";

    /**
     * @since 7.4
     */
    private static final String OPTION_DEBUG_CATEGORY_ARG_NAME = "categories";

    /**
     * @since 5.6
     */
    protected static final String OPTION_DEBUG_CATEGORY = "dc";

    private static final String OPTION_DEBUG_CATEGORY_DESC = "Deprecated: see categories on '--debug' option.";

    /**
     * @since 5.6
     */
    protected static final String OPTION_QUIET = "quiet";

    private static final String OPTION_QUIET_DESC = "Suppress information messages.";

    /**
     * @since 5.6
     */
    protected static final String OPTION_HELP = "help";

    private static final String OPTION_HELP_DESC = "Show detailed help.";

    /**
     * @since 5.6
     */
    protected static final String OPTION_RELAX = "relax";

    private static final String OPTION_RELAX_DESC = "Allow relax constraint on current platform (default: "
            + ConnectBroker.OPTION_RELAX_DEFAULT + ").";

    /**
     * @since 5.6
     */
    protected static final String OPTION_ACCEPT = "accept";

    private static final String OPTION_ACCEPT_DESC = "Accept, refuse or ask confirmation for all changes (default: "
            + ConnectBroker.OPTION_ACCEPT_DEFAULT + ").\n"
            + "In non interactive mode, '--accept=true' also sets '--relax=true' if needed.";

    /**
     * @since 5.9.1
     */
    protected static final String OPTION_SNAPSHOT = "snapshot";

    private static final String OPTION_SNAPSHOT_DESC = "Allow use of SNAPSHOT Nuxeo Packages.\n"
            + "This option is implicit:\n" //
            + "\t- on SNAPSHOT distributions (daily builds),\n"
            + "\t- if the command explicitly requests a SNAPSHOT package.";

    /**
     * @since 5.9.1
     */
    protected static final String OPTION_FORCE = "force";

    private static final String OPTION_FORCE_DESC = "Deprecated: use '--strict' option instead.";

    /**
     * @since 7.4
     */
    protected static final String OPTION_STRICT = "strict";

    private static final String OPTION_STRICT_DESC = "Abort in error the start command when a component cannot "
            + "be activated or if a server is already running.";

    /**
     * @since 5.6
     */
    protected static final String OPTION_HIDE_DEPRECATION = "hide-deprecation-warnings";

    protected static final String OPTION_HIDE_DEPRECATION_DESC = "Hide deprecation warnings.";

    /**
     * @since 6.0
     */
    protected static final String OPTION_IGNORE_MISSING = "ignore-missing";

    protected static final String OPTION_IGNORE_MISSING_DESC = "Ignore unknown packages on mp-add, mp-install and mp-set commands.";

    /**
     * @since 6.0
     */
    protected static final String OPTION_CLID = "clid";

    private static final String OPTION_CLID_DESC = "Use the provided instance CLID file";

    /**
     * @since 8.10-HF15
     */
    protected static final String OPTION_RENEW = "renew";

    private static final String OPTION_RENEW_DESC = "Renew the current CLID";

    /**
     * @since 7.4
     */
    protected static final String OPTION_ENCRYPT = "encrypt";

    private static final String OPTION_ENCRYPT_ARG_NAME = "algorithm";

    private static final String OPTION_ENCRYPT_DESC = String.format("Activate key value symmetric encryption.\n"
            + "The algorithm can be configured: <%s> is a cipher transformation of the form: \"algorithm/mode/padding\" or \"algorithm\".\n"
            + "Default value is \"%s\" (Advanced Encryption Standard, Electronic Cookbook Mode, PKCS5-style padding).",
            OPTION_ENCRYPT, Crypto.DEFAULT_ALGO);

    /**
     * @since 7.4
     */
    protected static final String OPTION_SET = "set";

    private static final String OPTION_SET_ARG_NAME = "template";

    private static final String OPTION_SET_DESC = String.format("Set the value for a given key.\n"
            + "The value is stored in {{%s}} by default unless a template name is provided; if so, it is then stored in the template's {{%s}} file.\n"
            + "If the value is empty (''), then the property is unset.\n"
            + "This option is implicit if no '--get' or '--get-regexp' option is used and there are exactly two parameters (key value).",
            ConfigurationGenerator.NUXEO_CONF, ConfigurationGenerator.NUXEO_DEFAULT_CONF);

    /**
     * @since 7.4
     */
    protected static final String OPTION_GET = "get";

    private static final String OPTION_GET_DESC = "Get the value for a given key. Returns error code 6 if the key was not found.\n"
            + "This option is implicit if '--set' option is not used and there are more or less than two parameters.";

    /**
     * @since 7.4
     */
    protected static final String OPTION_GET_REGEXP = "get-regexp";

    private static final String OPTION_GET_REGEXP_DESC = "Get the value for all keys matching the given regular expression(s).";

    /**
     * @since 8.3
     */
    protected static final String OPTION_GZIP_OUTPUT = "gzip";

    private static final String OPTION_GZIP_DESC = "Compress the output.";

    /**
     * @since 8.3
     */
    protected static final String OPTION_OUTPUT = "output";

    private static final String OPTION_OUTPUT_DESC = "Write output in specified file.";

    /**
     * @since 8.3
     */
    protected static final String OPTION_PRETTY_PRINT = "pretty-print";

    private static final String OPTION_PRETTY_PRINT_DESC = "Pretty print the output.";

    // Fallback to avoid an error when the log dir is not initialized
    static {
        if (System.getProperty(Environment.NUXEO_LOG_DIR) == null) {
            System.setProperty(Environment.NUXEO_LOG_DIR, ".");
        }
    }

    /**
     * @since 5.6
     */
    private static final String DEFAULT_NUXEO_CONTEXT_PATH = "/nuxeo";

    static final Log log = LogFactory.getLog(NuxeoLauncher.class);

    private static Options options = initParserOptions();

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

    /**
     * Number of try to cleanly stop server before killing process
     */
    private static final int STOP_NB_TRY = 5;

    private static final int STOP_SECONDS_BEFORE_NEXT_TRY = 2;

    private static final long STREAM_MAX_WAIT = 3000;

    private static final String PACK_TOMCAT_CLASS = "org.nuxeo.runtime.deployment.preprocessor.PackWar";

    private static final String PARAM_UPDATECENTER_DISABLED = "nuxeo.updatecenter.disabled";

    private static final String[] COMMANDS_NO_GUI = { "configure", "mp-init", "mp-purge", "mp-add", "mp-install",
            "mp-uninstall", "mp-request", "mp-remove", "mp-hotfix", "mp-upgrade", "mp-reset", "mp-list", "mp-listall",
            "mp-update", "status", "showconf", "mp-show", "mp-set", "config", "encrypt", "decrypt", OPTION_HELP,
            "register", "register-trial", "connect-report" };

    private static final String[] COMMANDS_NO_RUNNING_SERVER = { "pack", "mp-init", "mp-purge", "mp-add", "mp-install",
            "mp-uninstall", "mp-request", "mp-remove", "mp-hotfix", "mp-upgrade", "mp-reset", "mp-update", "mp-set" };

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

    /**
     * @since 5.7
     */
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
            + "\tJAVA\t\t\tPath to the {{java}} executable.\n"
            + "        JAVA_HOME\t\tPath to the Java home directory. Can also be defined in {{nuxeo.conf}}.\n"
            + "        JAVA_OPTS\t\tOptional values passed to the JVM. Can also be defined in {{nuxeo.conf}}.\n"
            + "        REQUIRED_JAVA_VERSION\tNuxeo requirement on Java version.\n" //
            + "\nJAVA USAGE\n"
            + String.format(
                    "        java [-D%s=\"JVM options\"] [-D%s=\"/path/to/nuxeo\"] [-D%s=\"/path/to/nuxeo.conf\"]"
                            + " [-Djvmcheck=nofail] -jar \"path/to/nuxeo-launcher.jar\" \\\n"
                            + "        \t[options] <command> [command parameters]\n\n",
                    JAVA_OPTS_PROPERTY, Environment.NUXEO_HOME, ConfigurationGenerator.NUXEO_CONF)
            + String.format("        %s\tParameters for the server JVM (default are %s).\n", JAVA_OPTS_PROPERTY,
                    JAVA_OPTS_DEFAULT)
            + String.format("        %s\t\tNuxeo server root path (default is parent of called script).\n",
                    Environment.NUXEO_HOME)
            + String.format("        %s\t\tPath to {{%1$s}} file (default is \"$NUXEO_HOME/bin/%1$s\").\n",
                    ConfigurationGenerator.NUXEO_CONF)
            + "        jvmcheck\t\tIf set to \"nofail\", ignore JVM version validation errors.\n";

    private static final String OPTION_HELP_DESC_COMMANDS = "\nCOMMANDS\n" + "        help\t\t\tPrint this message.\n"
            + "        gui\t\t\tDeprecated: use '--gui' option instead.\n"
            + "        start\t\t\tStart Nuxeo server in background, waiting for effective start. Useful for batch executions requiring the server being immediately available after the script returned.\n"
            + "        stop\t\t\tStop any Nuxeo server started with the same {{nuxeo.conf}} file.\n"
            + "        restart\t\t\tRestart Nuxeo server.\n"
            + "        config\t\t\tGet and set template or global parameters.\n"
            + "        encrypt\t\t\tOutput encrypted value for a given parameter.\n"
            + "        decrypt\t\t\tOutput decrypted value for a given parameter.\n"
            + "        configure\t\tConfigure Nuxeo server with parameters from {{nuxeo.conf}}.\n"
            + "        wizard\t\t\tStart the wizard.\n"
            + "        console\t\t\tStart Nuxeo server in a console mode. Ctrl-C will stop it.\n"
            + "        status\t\t\tPrint server running status.\n"
            + "        startbg\t\t\tStart Nuxeo server in background, without waiting for effective start. Useful for starting Nuxeo as a service.\n"
            + "        restartbg\t\tRestart Nuxeo server with a call to \"startbg\" after \"stop\".\n"
            + "        pack\t\t\tBuild a static archive.\n"
            + "        showconf\t\tDisplay the instance configuration.\n"
            + "        connect-report\t\tDump a JSON report about the running server (which being used by Nuxeo support).\n"
            + "        mp-list\t\t\tList local Nuxeo Packages.\n"
            + "        mp-listall\t\tList all Nuxeo Packages.\n"
            + "        mp-init\t\t\tPre-cache Nuxeo Packages locally available in the distribution.\n"
            + "        mp-update\t\tUpdate cache of Nuxeo Packages list.\n"
            + "        mp-add\t\t\tAdd Nuxeo Package(s) to local cache. You must provide the package file(s), name(s) or ID(s) as parameter.\n"
            + "        mp-install\t\tRun Nuxeo Package installation. It is automatically called at startup if {{installAfterRestart.log}} file exists in data directory. Else you must provide the package file(s), name(s) or ID(s) as parameter.\n"
            + "        mp-uninstall\t\tUninstall Nuxeo Package(s). You must provide the package name(s) or ID(s) as parameter (see \"mp-list\" command).\n"
            + "        mp-remove\t\tRemove Nuxeo Package(s) from the local cache. You must provide the package name(s) or ID(s) as parameter (see \"mp-list\" command).\n"
            + "        mp-reset\t\tReset all packages to DOWNLOADED state. May be useful after a manual server upgrade.\n"
            + "        mp-set\t\t\tInstall a list of Nuxeo Packages and remove those not in the list.\n"
            + "        mp-request\t\tInstall and uninstall Nuxeo Package(s) in one command. You must provide a *quoted* list of package names or IDs prefixed with + (install) or - (uninstall).\n"
            + "        mp-purge\t\tUninstall and remove all packages from the local cache.\n"
            + "        mp-hotfix\t\tInstall all the available hotfixes for the current platform but do not upgrade already installed ones (requires a registered instance).\n"
            + "        mp-upgrade\t\tGet all the available upgrades for the Nuxeo Packages currently installed (requires a registered instance).\n"
            + "        mp-show\t\t\tShow Nuxeo Package(s) information. You must provide the package file(s), name(s) or ID(s) as parameter.\n"
            + "        register\t\tRegister your instance with an existing Connect account. You must provide the credentials, the project name or ID, its type and a description.\n"
            + "        register-trial\t\tThis command is deprecated. To register for a free 30 day trial on Nuxeo Online Services, please visit https://connect.nuxeo.com/register\n"
            + "\nThe following commands are always executed in console/headless mode (no GUI): "
            + "\"configure\", \"mp-init\", \"mp-purge\", \"mp-add\", \"mp-install\", \"mp-uninstall\", \"mp-request\", "
            + "\"mp-remove\", \"mp-hotfix\", \"mp-upgrade\", \"mp-reset\", \"mp-list\", \"mp-listall\", \"mp-update\", "
            + "\"status\", \"showconf\", \"mp-show\", \"mp-set\", \"config\", \"encrypt\", \"decrypt\", \"help\".\n"
            + "\nThe following commands cannot be executed on a running server: \"pack\", \"mp-init\", \"mp-purge\", "
            + "\"mp-add\", \"mp-install\", \"mp-uninstall\", \"mp-request\", \"mp-remove\", \"mp-hotfix\", \"mp-upgrade\", "
            + "\"mp-reset\".\n"
            + "\nThe following commands can only be executed on a running server: \"connect-report\"\n"
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
            + "        nuxeoctl wizard [-d [<categories>]|-q|--clid <arg>|--gui <true|false|yes|no>]\n\n"
            + "        nuxeoctl stop [-d [<categories>]|-q|--gui <true|false|yes|no>]\n\n"
            + "        nuxeoctl start|restart|console|startbg|restartbg [-d [<categories>]|-q|--clid <arg>|--gui <true|false|yes|no>|--strict|-hdw]\n\n"
            + "        nuxeoctl mp-show [command parameters] [-d [<categories>]|-q|--clid <arg>|--xml|--json]\n\n"
            + "        nuxeoctl mp-list|mp-listall|mp-init|mp-update [command parameters] [-d [<categories>]|-q|--clid <arg>|--relax <true|false|yes|no>|--xml|--json]\n\n"
            + "        nuxeoctl mp-reset|mp-purge|mp-hotfix|mp-upgrade [command parameters] [-d [<categories>]|-q|--clid <arg>|--xml|--json|--accept <true|false|yes|no|ask>]\n\n"
            + "        nuxeoctl mp-add|mp-install|mp-uninstall|mp-remove|mp-set|mp-request [command parameters] [-d [<categories>]|-q|--clid <arg>|--xml|--json|--nodeps|--relax <true|false|yes|no|ask>|--accept <true|false|yes|no|ask>|-s|-im]\n\n"
            + "        nuxeoctl register [<username> [<project> [<type> <description>] [<pwd>]]]\n"
            + "                Register an instance with Nuxeo Online Services.\n\n"
            + "        nuxeoctl register --clid <arg>\n"
            + "                Register an instance according to the given CLID file.\n\n"
            + "        nuxeoctl register --renew [--clid <arg>]\n"
            + "                Renew an instance registration with Nuxeo Online Services.\n\n"
            + "        nuxeoctl pack <target> [-d [<categories>]|-q]\n\n" //
            + "        nuxeoctl connect-report [--output <file>|--gzip <*true|false|yes|no>|--pretty-print <true|*false|yes|no>]\n\n"
            + "OPTIONS";

    private static final String OPTION_HELP_FOOTER = "\nSee online documentation \"ADMINDOC/nuxeoctl and Control Panel Usage\": https://doc.nuxeo.com/x/FwNc";

    private static final int PAGE_SIZE = 20;

    public static final String CONNECT_TC_URL = "https://www.nuxeo.com/legal/nuxeo-trial-terms-conditions";

    protected ConfigurationGenerator configurationGenerator;

    public final ConfigurationGenerator getConfigurationGenerator() {
        return configurationGenerator;
    }

    protected ProcessManager processManager;

    protected Process nuxeoProcess;

    private String processRegex;

    protected String pid;

    private ExecutorService executor = Executors.newSingleThreadExecutor(
            new DaemonThreadFactory("NuxeoProcessThread", false));

    private ShutdownThread shutdownHook;

    protected String[] params;

    protected String command;

    public String getCommand() {
        return command;
    }

    /**
     * @since 7.4
     */
    public boolean commandIs(String aCommand) {
        return StringUtils.equalsIgnoreCase(command, aCommand);
    }

    public CommandSetInfo cset = new CommandSetInfo();

    private boolean useGui = false;

    /**
     * @since 5.5
     */
    public boolean isUsingGui() {
        return useGui;
    }

    private boolean reloadConfiguration = false;

    private int status = STATUS_CODE_UNKNOWN;

    private int errorValue = EXIT_CODE_OK;

    private StatusServletClient statusServletClient;

    private static boolean quiet = false;

    private static boolean debug = false;

    private static boolean strict = false;

    private boolean xmlOutput = false;

    private boolean jsonOutput = false;

    private ConnectBroker connectBroker = null;

    private String clid = null;

    private ConnectRegistrationBroker connectRegistrationBroker = null;

    private InstanceInfo info;

    CommandLine cmdLine;

    private boolean ignoreMissing = false;

    /**
     * @since 5.5
     * @return true if quiet mode is active
     */
    public boolean isQuiet() {
        return quiet;
    }

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
        processManager = getOSProcessManager();
        processRegex = "^(?!/bin/sh).*" + Pattern.quote(configurationGenerator.getNuxeoConf().getPath()) + ".*"
                + Pattern.quote(getServerPrint()) + ".*$";
        // Set OS-specific decorations
        if (SystemUtils.IS_OS_MAC) {
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "NuxeoCtl");
        }
    }

    private ProcessManager getOSProcessManager() {
        if (SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_AIX) {
            return new UnixProcessManager();
        } else if (SystemUtils.IS_OS_MAC) {
            return new MacProcessManager();
        } else if (SystemUtils.IS_OS_SUN_OS) {
            return new SolarisProcessManager();
        } else if (SystemUtils.IS_OS_WINDOWS) {
            WindowsProcessManager windowsProcessManager = new WindowsProcessManager();
            return windowsProcessManager.isUsable() ? windowsProcessManager : new PureJavaProcessManager();
        } else {
            return new PureJavaProcessManager();
        }
    }

    /**
     * Do not directly call this method without a call to {@link #checkNoRunningServer()}
     *
     * @see #doStart()
     * @throws IOException In case of issue with process.
     * @throws InterruptedException If any thread has interrupted the current thread.
     */
    protected void start(boolean logProcessOutput) throws IOException, InterruptedException {
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
        setServerStartCommand(startCommand);
        startCommand.addAll(Arrays.asList(params));
        ProcessBuilder pb = new ProcessBuilder(getOSCommand(startCommand));
        pb.directory(configurationGenerator.getNuxeoHome());
        log.debug("Server command: " + pb.command());
        nuxeoProcess = pb.start();
        Thread.sleep(1000);
        boolean processExited = false;
        // Check if process exited early
        if (nuxeoProcess == null) {
            log.error(String.format("Server start failed with command: %s", pb.command()));
            if (SystemUtils.IS_OS_WINDOWS && configurationGenerator.getNuxeoHome().getPath().contains(" ")) {
                // NXP-17679
                log.error("The server path must not contain spaces under Windows.");
            }
            return;
        }
        try {
            int exitValue = nuxeoProcess.exitValue();
            if (exitValue != 0) {
                log.error(String.format("Server start failed (%d).", exitValue));
            }
            processExited = true;
        } catch (IllegalThreadStateException e) {
            // Normal case
        }
        logProcessStreams(nuxeoProcess, processExited || logProcessOutput);
        if (!processExited) {
            if (getPid() != null) {
                log.warn("Server started with process ID " + pid + ".");
            } else {
                log.warn("Sent server start command but could not get process ID.");
            }
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
     * Check if some server is already running (from another thread) and throw a Runtime exception if it finds one. That
     * method will work where {@link #isRunning()} won't.
     *
     * @throws IllegalThreadStateException Thrown if a server is already running.
     */
    public void checkNoRunningServer() throws IllegalStateException {
        try {
            String existingPid = getPid();
            if (existingPid != null) {
                errorValue = EXIT_CODE_OK;
                throw new IllegalStateException("A server is running with process ID " + existingPid);
            }
        } catch (IOException e) {
            log.warn("Could not check existing process: " + e.getMessage());
        }
    }

    /**
     * @return (since 5.5) Array list with created stream gobbler threads.
     */
    public ArrayList<ThreadedStreamGobbler> logProcessStreams(Process process, boolean logProcessOutput) {
        ArrayList<ThreadedStreamGobbler> sgArray = new ArrayList<>();
        ThreadedStreamGobbler inputSG;
        ThreadedStreamGobbler errorSG;
        if (logProcessOutput) {
            inputSG = new ThreadedStreamGobbler(process.getInputStream(), System.out);
            errorSG = new ThreadedStreamGobbler(process.getErrorStream(), System.err);
        } else {
            inputSG = new ThreadedStreamGobbler(process.getInputStream(), SimpleLog.LOG_LEVEL_OFF);
            errorSG = new ThreadedStreamGobbler(process.getErrorStream(), SimpleLog.LOG_LEVEL_OFF);
        }
        inputSG.start();
        errorSG.start();
        sgArray.add(inputSG);
        sgArray.add(errorSG);
        return sgArray;
    }

    protected abstract String getServerPrint();

    /**
     * Will wrap, if necessary, the command within a Shell command
     *
     * @param roughCommand Java command which will be run
     * @return wrapped command depending on the OS
     */
    private List<String> getOSCommand(List<String> roughCommand) {
        if (SystemUtils.IS_OS_UNIX) {
            return getUnixCommand(roughCommand);
        }
        if (SystemUtils.IS_OS_WINDOWS) {
            return getWindowsCommand(roughCommand);
        }
        throw new IllegalStateException("Unknown os, can't launch server");
    }

    private List<String> getWindowsCommand(List<String> roughCommand) {
        ArrayList<String> osCommand = new ArrayList<>();
        for (String commandToken : roughCommand) {
            if (StringUtils.isBlank(commandToken)) {
                continue;
            }
            osCommand.add("\"" + commandToken + "\"");
        }
        return osCommand;
    }

    private List<String> getUnixCommand(List<String> roughCommand) {
        ArrayList<String> osCommand = new ArrayList<>();
        StringBuilder linearizedCommand = new StringBuilder("exec");
        for (String commandToken : roughCommand) {
            if (StringUtils.isBlank(commandToken)) {
                continue;
            }
            if (commandToken.contains(" ")) {
                commandToken = commandToken.replaceAll(" ", "\\\\ ");
            }
            linearizedCommand.append(' ').append(commandToken);
        }
        osCommand.add("/bin/sh");
        osCommand.add("-c");
        osCommand.add(linearizedCommand.toString());
        return osCommand;
    }

    protected abstract Collection<? extends String> getServerProperties();

    protected abstract void setServerStartCommand(List<String> command);

    private File getJavaExecutable() {
        return new File(System.getProperty("java.home"), "bin" + File.separator + "java");
    }

    protected abstract String getClassPath();

    /**
     * @since 5.6
     */
    protected abstract String getShutdownClassPath();

    protected Collection<String> getNuxeoProperties() {
        ArrayList<String> nuxeoProperties = new ArrayList<>();
        nuxeoProperties.add(
                String.format("-D%s=%s", Environment.NUXEO_HOME, configurationGenerator.getNuxeoHome().getPath()));
        nuxeoProperties.add(String.format("-D%s=%s", ConfigurationGenerator.NUXEO_CONF,
                configurationGenerator.getNuxeoConf().getPath()));
        nuxeoProperties.add(getNuxeoProperty(Environment.NUXEO_LOG_DIR));
        nuxeoProperties.add(getNuxeoProperty(Environment.NUXEO_DATA_DIR));
        nuxeoProperties.add(getNuxeoProperty(Environment.NUXEO_TMP_DIR));
        nuxeoProperties.add(getNuxeoProperty(Environment.NUXEO_MP_DIR));
        if (!DEFAULT_NUXEO_CONTEXT_PATH.equals(
                configurationGenerator.getUserConfig().getProperty(Environment.NUXEO_CONTEXT_PATH))) {
            nuxeoProperties.add(getNuxeoProperty(Environment.NUXEO_CONTEXT_PATH));
        }
        if (overrideJavaTmpDir) {
            nuxeoProperties.add("-Djava.io.tmpdir="
                    + configurationGenerator.getUserConfig().getProperty(Environment.NUXEO_TMP_DIR));
        }
        return nuxeoProperties;
    }

    private String getNuxeoProperty(String property) {
        return "-D" + property + "=" + configurationGenerator.getUserConfig().getProperty(property);
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
            setStrict(true);
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
            log.error("Invalid command line. " + e.getMessage());
            log.debug(e, e);
            printShortHelp();
            System.exit(
                    launcher == null || launcher.errorValue == EXIT_CODE_OK ? EXIT_CODE_INVALID : launcher.errorValue);
        } catch (IOException | PackageException | ConfigurationException | GeneralSecurityException e) {
            log.error(e.getMessage());
            log.debug(e, e);
            System.exit(
                    launcher == null || launcher.errorValue == EXIT_CODE_OK ? EXIT_CODE_INVALID : launcher.errorValue);
        } catch (Exception e) {
            log.error("Cannot execute command. " + e.getMessage(), e);
            log.debug(e, e);
            System.exit(EXIT_CODE_ERROR);
        }
    }

    /**
     * @since 5.5
     */
    public static void launch(final NuxeoLauncher launcher)
            throws IOException, PackageException, ConfigurationException, ParseException, GeneralSecurityException {
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
            launcher.errorValue = launcher.getStatus();
            if (!quiet) {
                log.warn(statusMsg);
                if (launcher.isStarted()) {
                    log.info("Go to " + launcher.getURL());
                    log.info(launcher.getStartupSummary());
                }
            }
        } else if (launcher.commandIs("startbg")) {
            commandSucceeded = launcher.doStart();
        } else if (launcher.commandIs("start")) {
            if (launcher.useGui) {
                launcher.getGUI().start();
            } else {
                commandSucceeded = launcher.doStartAndWait();
            }
        } else if (launcher.commandIs("console")) {
            launcher.executor.execute(() -> {
                launcher.addShutdownHook();
                try {
                    if (!launcher.doStart(true)) {
                        launcher.removeShutdownHook();
                        System.exit(1);
                    } else if (!quiet) {
                        log.info("Go to " + launcher.getURL());
                    }
                } catch (PackageException e) {
                    log.error("Could not initialize the packaging subsystem", e);
                    launcher.removeShutdownHook();
                    System.exit(EXIT_CODE_ERROR);
                }
            });
        } else if (launcher.commandIs("stop")) {
            if (launcher.useGui) {
                launcher.getGUI().stop();
            } else {
                launcher.stop();
            }
        } else if (launcher.commandIs("restartbg")) {
            launcher.stop();
            commandSucceeded = launcher.doStart();
        } else if (launcher.commandIs("restart")) {
            launcher.stop();
            commandSucceeded = launcher.doStartAndWait();
        } else if (launcher.commandIs("wizard")) {
            commandSucceeded = launcher.startWizard();
        } else if (launcher.commandIs("configure")) {
            launcher.configure();
        } else if (launcher.commandIs("pack")) {
            launcher.pack();
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
                throw new ParseException("The command mp-request is not available with the --nodeps option");
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
        } else if (launcher.commandIs("connect-report")) {
            boolean gzip = Boolean.parseBoolean(launcher.cmdLine.getOptionValue(OPTION_GZIP_OUTPUT, "true"));
            boolean prettyprinting = Boolean.parseBoolean(
                    launcher.cmdLine.getOptionValue(OPTION_PRETTY_PRINT, "false"));
            Path outputpath;
            if (launcher.cmdLine.hasOption(OPTION_OUTPUT)) {
                outputpath = Paths.get(launcher.cmdLine.getOptionValue(OPTION_OUTPUT));
            } else {
                Path dir = Paths.get(
                        launcher.configurationGenerator.getUserConfig().getProperty(Environment.NUXEO_TMP_DIR));
                outputpath = dir.resolve("nuxeo-connect-tools-report.json".concat(gzip ? ".gz" : ""));
            }
            log.info("Dumping connect report in " + outputpath);
            try (OutputStream output = openOutput(outputpath, gzip)) {
                commandSucceeded = launcher.dumpConnectReport(output, prettyprinting);
            }
        } else {
            log.error("Unknown command " + launcher.command);
            printLongHelp();
            launcher.errorValue = EXIT_CODE_INVALID;
        }
        if (launcher.xmlOutput && launcher.command.startsWith("mp-")) {
            launcher.printXMLOutput();
        }
        commandSucceeded = commandSucceeded && launcher.errorValue == EXIT_CODE_OK;
        if (!commandSucceeded && !quiet || debug) {
            launcher.cset.log(commandSucceeded);
        }
        if (!commandSucceeded) {
            System.exit(launcher.errorValue);
        }
    }

    protected static OutputStream openOutput(Path path, boolean gzip) throws IOException {
        OutputStream output = Files.newOutputStream(path, StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.CREATE);
        if (gzip) {
            output = new GZIPOutputStream(output);
        }
        return output;
    }

    /**
     * Prompts for a valid email address according to RFC 822 standards. The remote service may apply stricter
     * constraints on email validation such as some black listed domains.
     *
     * @return the user input. Never null.
     * @throws ConfigurationException If the user input is read from stdin and is {@code null} or does not match the
     *             {@code regex}
     * @since 8.3
     */
    public String promptEmail() throws IOException, ConfigurationException {
        EmailValidator validator = EmailValidator.getInstance();
        final String message = "Email Address: ";
        final String error = "Invalid email address.";
        return prompt(message, validator::isValid, error);
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
     * @param confirmation if true, password is asked twice.
     * @since 8.3
     */
    public char[] promptPassword(boolean confirmation) throws IOException, ConfigurationException {
        char[] pwd = promptPassword("Please enter your password: ");
        if (confirmation) {
            char[] pwdVerification = promptPassword("Please re-enter your password: ");
            if (!Arrays.equals(pwd, pwdVerification)) {
                throw new ConfigurationException("Passwords do not match.");
            }
        }
        return pwd;
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
            throws ConfigurationException, IOException, PackageException {
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
     * nuxeoctl register [<username> [<project> [<type> <description>] [pwd]]]
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
    public boolean register() throws IOException, ConfigurationException, PackageException {
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

        return registerRemoteInstance();
    }

    protected boolean registerRenew() throws IOException {
        try {
            getConnectRegistrationBroker().remoteRenewRegistration();
        } catch (RegistrationException e) {
            log.debug(e, e);
            errorValue = EXIT_CODE_ERROR;
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

    protected boolean registerRemoteInstance() throws IOException, ConfigurationException, PackageException {
        // 0/1 param: [<username>]
        // 2/3 params: <username> <project> [pwd]
        // 4/5 params: <username> <project> <type> <description> [pwd]
        if (params.length > 5) {
            throw new ConfigurationException("Wrong number of arguments.");
        }
        String username;
        if (params.length > 0) {
            username = params[0];
        } else {
            username = prompt("Username: ", StringUtils::isNotBlank, "Username cannot be empty.");
        }
        char[] password;
        if (params.length == 3 || params.length == 5) {
            password = params[params.length - 1].toCharArray();
        } else {
            password = promptPassword(false);
        }
        ConnectProject project;
        List<ConnectProject> projs = getConnectRegistrationBroker().getAvailableProjects(username, password);
        if (params.length > 1) {
            String projectName = params[1];
            project = getConnectRegistrationBroker().getProjectByName(projectName, projs);
            if (project == null) {
                throw new ConfigurationException("Unknown project: " + projectName);
            }
        } else {
            project = promptProject(projs);
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

        return registerRemoteInstance(username, password, project, type, description);
    }

    protected boolean registerRemoteInstance(String username, char[] password, ConnectProject project,
            NuxeoClientInstanceType type, String description) throws IOException, ConfigurationException {
        getConnectRegistrationBroker().registerRemote(username, password, project.getUuid(), type, description);
        log.info(String.format("Server registered to %s for project %s\nType: %s\nDescription: %s", username, project,
                type, description));
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
    public boolean registerTrial() throws IOException, ConfigurationException, PackageException {
        String msg = "This command is deprecated. To register for a free 30 day trial on Nuxeo Online Services,"
                + " please visit https://connect.nuxeo.com/register";
        throw new UnsupportedOperationException(msg);
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
                    log.debug(e, e);
                    errorValue = EXIT_CODE_ERROR;
                    return;
                }
            }
            System.out.println(encryptedString);
        } else {
            for (String strToEncrypt : params) {
                String encryptedString = crypto.encrypt(algorithm, strToEncrypt.getBytes());
                System.out.println(encryptedString);
            }
        }
    }

    /**
     * @since 7.4
     */
    protected void decrypt() {
        Crypto crypto = configurationGenerator.getCrypto();
        askCryptoKeyAndDecrypt(crypto, params).forEach(System.out::println);
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
                log.debug(e, e);
                errorValue = EXIT_CODE_ERROR;
                return null;
            }
        }
        if (!validKey) {
            errorValue = EXIT_CODE_INVALID;
            return null;
        }
        return Stream.of(values).map(crypto::decrypt).map(Crypto::getChars).map(String::new).collect(
                Collectors.toList());
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
            if (keys.isEmpty()) {
                errorValue = EXIT_CODE_NOT_CONFIGURED;
            }
        } else {
            keys = Arrays.asList(params);
        }

        Crypto crypto = userConfig.getCrypto();
        boolean keyChecked = false; // Secret key is asked only once
        boolean raw = true;
        StringBuilder sb = new StringBuilder();
        final String newLine = System.getProperty("line.separator");
        for (String key : keys) {
            String value = userConfig.getProperty(key, raw);
            if (value == null) {
                errorValue = EXIT_CODE_NOT_CONFIGURED;
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
        System.out.print(sb.toString());
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
            if (iterator.hasNext()) {
                value = iterator.next();
                if (doEncrypt) {
                    value = crypto.encrypt(algorithm, value.getBytes());
                } else if (Environment.CRYPT_KEY.equals(key) || Environment.CRYPT_KEYSTORE_PASS.equals(key)) {
                    value = Base64.encodeBase64String(value.getBytes());
                }
            } else {
                Console console = System.console();
                if (console != null) {
                    final String fmt = "Please enter the value for %s: ";
                    if (doEncrypt) {
                        value = crypto.encrypt(algorithm, Crypto.getBytes(console.readPassword(fmt, key)));
                    } else if (Environment.CRYPT_KEY.equals(key) || Environment.CRYPT_KEYSTORE_PASS.equals(key)) {
                        value = Base64.encodeBase64String(Crypto.getBytes(console.readPassword(fmt, key)));
                    } else {
                        value = console.readLine(fmt, key);
                    }
                } else { // try reading from stdin
                    try {
                        if (doEncrypt) {
                            value = crypto.encrypt(algorithm, IOUtils.toByteArray(System.in));
                        } else if (Environment.CRYPT_KEY.equals(key) || Environment.CRYPT_KEYSTORE_PASS.equals(key)) {
                            value = Base64.encodeBase64String(IOUtils.toByteArray(System.in));
                        } else {
                            value = IOUtils.toString(System.in, UTF_8);
                        }
                    } catch (IOException e) {
                        log.debug(e, e);
                        errorValue = EXIT_CODE_ERROR;
                        return;
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
        log.debug("Old values: " + oldValues);
    }

    /**
     * Since 5.5
     */
    protected boolean pack() {
        try {
            configurationGenerator.setProperty(PARAM_UPDATECENTER_DISABLED, "true");
            List<String> startCommand = new ArrayList<>();
            startCommand.add(getJavaExecutable().getPath());
            startCommand.addAll(getJavaOptsProperty(Function.identity()));
            startCommand.add("-cp");
            String classpath = getClassPath();
            classpath = addToClassPath(classpath, "bin" + File.separator + "nuxeo-launcher.jar");
            classpath = getClassPath(classpath, configurationGenerator.getServerConfigurator().getServerLibDir());
            classpath = getClassPath(classpath, configurationGenerator.getServerConfigurator().getNuxeoLibDir());
            classpath = getClassPath(classpath, new File(configurationGenerator.getRuntimeHome(), "bundles"));
            startCommand.add(classpath);
            startCommand.addAll(getNuxeoProperties());
            if (configurationGenerator.isTomcat) {
                startCommand.add(PACK_TOMCAT_CLASS);
            } else {
                errorValue = EXIT_CODE_ERROR;
                return false;
            }
            startCommand.add(configurationGenerator.getRuntimeHome().getPath());
            startCommand.addAll(Arrays.asList(params));
            ProcessBuilder pb = new ProcessBuilder(getOSCommand(startCommand));
            pb.directory(configurationGenerator.getNuxeoHome());
            log.debug("Pack command: " + pb.command());
            Process process = pb.start();
            ArrayList<ThreadedStreamGobbler> sgArray = logProcessStreams(process, true);
            Thread.sleep(100);
            process.waitFor();
            waitForProcessStreams(sgArray);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (IOException e) {
            errorValue = EXIT_CODE_ERROR;
            log.error("Could not start process", e);
        } catch (ConfigurationException e) {
            errorValue = EXIT_CODE_ERROR;
            log.error(e);
        }
        return errorValue == EXIT_CODE_OK;
    }

    protected boolean startWizard() throws PackageException {
        if (!configurationGenerator.getServerConfigurator().isWizardAvailable()) {
            log.error("Sorry, the wizard is not available within that server.");
            return false;
        }
        if (isRunning()) {
            log.error("Server already running. " + "Please stop it before calling \"wizard\" command "
                    + "or use the Admin Center instead of the wizard.");
            return false;
        }
        if (reloadConfiguration) {
            configurationGenerator = new ConfigurationGenerator(quiet, debug);
            configurationGenerator.init();
            reloadConfiguration = false;
        }
        configurationGenerator.getUserConfig().setProperty(ConfigurationGenerator.PARAM_WIZARD_DONE, "false");
        return doStart();
    }

    /**
     * @see #doStartAndWait(boolean)
     */
    public boolean doStartAndWait() throws PackageException {
        boolean started = doStartAndWait(false);
        if (started && !quiet) {
            log.info("Go to " + getURL());
        }
        return started;
    }

    /**
     * @see #stop(boolean)
     */
    public void stop() {
        stop(false);
    }

    /**
     * Call {@link #doStart(boolean)} with false as parameter.
     *
     * @see #doStart(boolean)
     * @return true if the server started successfully
     */
    public boolean doStart() throws PackageException {
        boolean started = doStart(false);
        if (started && !quiet) {
            log.info("Go to " + getURL());
        }
        return started;
    }

    /**
     * Whereas {@link #doStart()} considers the server as started when the process is running, {@link #doStartAndWait()}
     * waits for effective start by watching the logs
     *
     * @param logProcessOutput Must process output stream must be logged or not.
     * @return true if the server started successfully
     */
    public boolean doStartAndWait(boolean logProcessOutput) throws PackageException {
        boolean commandSucceeded = false;
        if (doStart(logProcessOutput)) {
            addShutdownHook();
            try {
                if (configurationGenerator.isWizardRequired() || waitForEffectiveStart()) {
                    commandSucceeded = true;
                }
                removeShutdownHook();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
        return commandSucceeded;
    }

    protected void removeShutdownHook() {
        try {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
            log.debug("Removed shutdown hook");
        } catch (IllegalStateException e) {
            // the virtual machine is already in the process of shutting down
        }
    }

    /**
     * @return true if Nuxeo is ready
     */
    protected boolean waitForEffectiveStart() throws InterruptedException {
        long startTime = new Date().getTime();
        int startMaxWait = Integer.parseInt(
                configurationGenerator.getUserConfig().getProperty(START_MAX_WAIT_PARAM, getDefaultMaxWait()));
        log.debug("Will wait for effective start during " + startMaxWait + " seconds.");
        final StringBuilder startSummary = new StringBuilder();
        final String newLine = System.getProperty("line.separator");
        boolean isReady = false;
        long deltaTime;
        // Wait for status servlet ready
        do {
            try {
                isReady = statusServletClient.init();
            } catch (SocketTimeoutException e) {
                if (!quiet) {
                    System.out.print(".");
                }
            }
            deltaTime = (new Date().getTime() - startTime) / 1000;
        } while (!isReady && deltaTime < startMaxWait && isRunning());
        // Wait for effective start reported from status servlet
        do {
            isReady = isStarted();
            if (!isReady) {
                if (!quiet) {
                    System.out.print(".");
                }
                Thread.sleep(1000);
            }
            deltaTime = (new Date().getTime() - startTime) / 1000;
        } while (!isReady && deltaTime < startMaxWait && isRunning());
        if (isReady) {
            startSummary.append(newLine).append(getStartupSummary());
            long duration = (new Date().getTime() - startTime) / 1000;
            startSummary.append(String.format("Started in %dmin%02ds", duration / 60, duration % 60));
            if (wasStartupFine()) {
                if (!quiet) {
                    System.out.println(startSummary);
                }
            } else {
                System.err.println(startSummary);
                if (strict) {
                    errorValue = EXIT_CODE_ERROR;
                    log.error("Shutting down because of unstarted component in strict mode...");
                    stop();
                    return false;
                }
            }
            return true;
        } else if (deltaTime >= startMaxWait) {
            if (!quiet) {
                System.out.println();
            }
            log.error("Starting process is taking too long - giving up.");
        }
        errorValue = EXIT_CODE_ERROR;
        return false;
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
     * Starts the server in background.
     *
     * @return true if server successfully started
     */
    public boolean doStart(boolean logProcessOutput) throws PackageException {
        errorValue = EXIT_CODE_OK;
        boolean serverStarted = false;
        try {
            if (reloadConfiguration) {
                configurationGenerator = new ConfigurationGenerator(quiet, debug);
                configurationGenerator.init();
            } else {
                // Ensure reload on next start
                reloadConfiguration = true;
            }
            configure();
            configurationGenerator.verifyInstallation();

            if (configurationGenerator.isWizardRequired()) {
                if (!configurationGenerator.isForceGeneration()) {
                    log.error("Cannot start setup wizard with " + ConfigurationGenerator.PARAM_FORCE_GENERATION
                            + "=false. Either set it to true or once, either set "
                            + ConfigurationGenerator.PARAM_WIZARD_DONE + "=true to skip the wizard.");
                    errorValue = EXIT_CODE_NOT_CONFIGURED;
                    return false;
                }
                StringBuilder paramsStr = new StringBuilder();
                for (String param : params) {
                    paramsStr.append(' ').append(param);
                }
                System.setProperty(ConfigurationGenerator.PARAM_WIZARD_RESTART_PARAMS, paramsStr.toString());
                configurationGenerator.prepareWizardStart();
            } else {
                configurationGenerator.cleanupPostWizard();
            }

            log.debug("Check if install in progress...");
            if (configurationGenerator.isInstallInProgress()) {
                if (!getConnectBroker().executePending(configurationGenerator.getInstallFile(), true, true,
                        ignoreMissing)) {
                    errorValue = EXIT_CODE_ERROR;
                    log.error(String.format(
                            "Start interrupted due to failure on pending actions. You can resume with a new start;"
                                    + " or you can restore the file '%s', optionally using the '--%s' option.",
                            configurationGenerator.getInstallFile().getName(), OPTION_IGNORE_MISSING));
                    return false;
                }

                // configuration will be reloaded, keep wizard value
                System.setProperty(ConfigurationGenerator.PARAM_WIZARD_DONE,
                        configurationGenerator.getUserConfig().getProperty(ConfigurationGenerator.PARAM_WIZARD_DONE,
                                "true"));
                return doStart(logProcessOutput);
            }

            start(logProcessOutput);
            serverStarted = isRunning();
            if (pid != null) {
                File pidFile = new File(configurationGenerator.getPidDir(), "nuxeo.pid");
                try (FileWriter writer = new FileWriter(pidFile)) {
                    writer.write(pid);
                }
            }
        } catch (ConfigurationException e) {
            errorValue = EXIT_CODE_NOT_CONFIGURED;
            log.error("Could not run configuration: " + e.getMessage());
            log.debug(e, e);
        } catch (IOException e) {
            errorValue = EXIT_CODE_ERROR;
            log.error("Could not start process: " + e.getMessage());
            log.debug(e, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (IllegalStateException e) {
            if (strict) {
                // assume program is not configured because of http port binding
                // conflict
                errorValue = EXIT_CODE_NOT_CONFIGURED;
            }
            log.error(e.getMessage());
        }
        return serverStarted;
    }

    /**
     * @since 5.6
     */
    protected void printXMLOutput() {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(CommandSetInfo.class, CommandInfo.class,
                    PackageInfo.class, MessageInfo.class);
            printXMLOutput(jaxbContext, cset);
        } catch (JAXBException e) {
            log.error("Output serialization failed: " + e.getMessage(), e);
            errorValue = EXIT_CODE_NOT_RUNNING;
        }
    }

    /**
     * @since 5.6
     */
    protected void printXMLOutput(JAXBContext jaxbContext, Object objectToOutput) {
        try {
            printXMLOutput(jaxbContext, objectToOutput, System.out);
        } catch (JAXBException | XMLStreamException | FactoryConfigurationError e) {
            log.error("Output serialization failed: " + e.getMessage(), e);
            errorValue = EXIT_CODE_NOT_RUNNING;
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

    /**
     * Stop stream gobblers contained in the given ArrayList
     *
     * @since 5.5
     * @see #logProcessStreams(Process, boolean)
     */
    public void waitForProcessStreams(ArrayList<ThreadedStreamGobbler> sgArray) {
        for (ThreadedStreamGobbler streamGobbler : sgArray) {
            try {
                streamGobbler.join(STREAM_MAX_WAIT);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                streamGobbler.interrupt();
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * @since 5.5
     * @return classpath with all jar files in baseDir
     */
    protected String getClassPath(String classpath, File baseDir) {
        File[] files = getFilename(baseDir, ".*");
        StringBuilder sb = new StringBuilder(classpath);
        for (File file : files) {
            sb.append(System.getProperty("path.separator")).append(file.getPath());
        }
        return sb.toString();
    }

    /**
     * @since 5.5
     * @return filename matching filePattern in baseDir
     */
    protected File[] getFilename(File baseDir, final String filePattern) {
        return baseDir.listFiles((basedir, filename) -> filename.matches(filePattern + "(-[0-9].*)?\\.jar"));
    }

    protected class ShutdownThread extends Thread {

        private NuxeoLauncher launcher;

        public ShutdownThread(NuxeoLauncher launcher) {
            super();
            this.launcher = launcher;
        }

        @Override
        public void run() {
            log.debug("Shutting down...");
            if (launcher.isRunning()) {
                launcher.stop();
            }
            log.debug("Shutdown complete.");
        }
    }

    protected void addShutdownHook() {
        log.debug("Add shutdown hook");
        shutdownHook = new ShutdownThread(this);
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    /**
     * Stops the server. Will try to call specific class for a clean stop, retry, waiting between each try, then kill
     * the process if still running.
     */
    public void stop(boolean logProcessOutput) {
        long startTime = new Date().getTime();
        long deltaTime;
        try {
            if (!isRunning()) {
                log.warn("Server is not running.");
                return;
            }
            if (!quiet) {
                System.out.print("\nStopping server...");
            }
            int nbTry = 0;
            boolean retry = false;
            int stopMaxWait = Integer.parseInt(
                    configurationGenerator.getUserConfig().getProperty(STOP_MAX_WAIT_PARAM, STOP_MAX_WAIT_DEFAULT));
            do {
                List<String> stopCommand = new ArrayList<>();
                stopCommand.add(getJavaExecutable().getPath());
                stopCommand.add("-cp");
                stopCommand.add(getShutdownClassPath());
                stopCommand.addAll(getNuxeoProperties());
                stopCommand.addAll(getServerProperties());
                setServerStopCommand(stopCommand);
                stopCommand.addAll(Arrays.asList(params));
                ProcessBuilder pb = new ProcessBuilder(getOSCommand(stopCommand));
                pb.directory(configurationGenerator.getNuxeoHome());
                log.debug("Server command: " + pb.command());
                try {
                    Process stopProcess = pb.start();
                    ArrayList<ThreadedStreamGobbler> sgArray = logProcessStreams(stopProcess, logProcessOutput);
                    stopProcess.waitFor();
                    waitForProcessStreams(sgArray);
                    boolean wait = true;
                    while (wait) {
                        try {
                            if (stopProcess.exitValue() == 0) {
                                // Successful call for server stop
                                retry = false;
                            } else {
                                // Failed to call for server stop
                                retry = ++nbTry < STOP_NB_TRY;
                                if (!quiet) {
                                    System.out.print(".");
                                }
                                Thread.sleep(STOP_SECONDS_BEFORE_NEXT_TRY * 1000);
                            }
                            wait = false;
                        } catch (IllegalThreadStateException e) {
                            // Stop call is still running
                            wait = true;
                            if (!quiet) {
                                System.out.print(".");
                            }
                            Thread.sleep(1000);
                        }
                    }
                    // Exit if there's no way to check for server stop
                    if (!processManager.canFindPid()) {
                        log.warn("Can't check server status on your OS.");
                        return;
                    }
                    // Wait a few seconds for effective stop
                    do {
                        if (!quiet) {
                            System.out.print(".");
                        }
                        Thread.sleep(1000);
                        deltaTime = (new Date().getTime() - startTime) / 1000;
                    } while (!retry && getPid() != null && deltaTime < stopMaxWait);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            } while (retry);
            if (getPid() == null) {
                log.warn("Server stopped.");
            } else {
                log.info("No answer from server, try to kill process " + pid + "...");
                processManager.kill(nuxeoProcess, pid);
                if (getPid() == null) {
                    log.warn("Server forcibly stopped.");
                }
            }
        } catch (IOException e) {
            log.error("Could not manage process!", e);
        }
    }

    protected abstract void setServerStopCommand(List<String> command);

    private String getPid() throws IOException {
        pid = processManager.findPid(processRegex);
        log.debug("regexp: " + processRegex + " pid:" + pid);
        return pid;
    }

    /**
     * Configure the server after checking installation
     *
     * @throws ConfigurationException If an installation error is detected or if configuration fails
     */
    public void configure() throws ConfigurationException {
        try {
            checkNoRunningServer();
            configurationGenerator.checkJavaVersion();
            configurationGenerator.run();
            overrideJavaTmpDir = Boolean.parseBoolean(
                    configurationGenerator.getUserConfig().getProperty(OVERRIDE_JAVA_TMPDIR_PARAM, "true"));
        } catch (ConfigurationException e) {
            errorValue = EXIT_CODE_NOT_CONFIGURED;
            throw e;
        }
    }

    /**
     * @return Default max wait depending on server (ie JBoss takes much more time than Tomcat)
     */
    private String getDefaultMaxWait() {
        return START_MAX_WAIT_DEFAULT;
    }

    /**
     * Return process status (running or not) as String, depending on OS capability to manage processes. Set status
     * value following "http://refspecs.freestandards.org/LSB_4.1.0/LSB-Core-generic/LSB-Core- generic/iniscrptact.html"
     *
     * @see #getStatus()
     */
    public String status() {
        try {
            if (processManager instanceof PureJavaProcessManager) {
                status = STATUS_CODE_UNKNOWN;
                return "Can't check server status on your OS.";
            }
            if (getPid() == null) {
                status = STATUS_CODE_OFF;
                return "Server is not running.";
            } else {
                status = STATUS_CODE_ON;
                return "Server is running with process ID " + getPid() + ".";
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
     * Last error value set by any method. Exit code values are following the Linux Standard Base Core Specification
     * 4.1.
     */
    public int getErrorValue() {
        return errorValue;
    }

    /**
     * @return a NuxeoLauncher instance specific to current server ( Tomcat or Jetty).
     * @throws ConfigurationException If server cannot be identified
     * @since 5.5
     */
    public static NuxeoLauncher createLauncher(String[] args)
            throws ConfigurationException, ParseException, IOException, PackageException {
        CommandLine cmdLine = parseOptions(args);
        ConfigurationGenerator cg = new ConfigurationGenerator(quiet, debug);
        if (cmdLine.hasOption(OPTION_HIDE_DEPRECATION)) {
            cg.hideDeprecationWarnings(true);
        }
        NuxeoLauncher launcher;
        if (cg.isJetty) {
            launcher = new NuxeoJettyLauncher(cg);
        } else if (cg.isTomcat) {
            launcher = new NuxeoTomcatLauncher(cg);
        } else {
            throw new ConfigurationException("Unknown server!");
        }
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
    private void setArgs(CommandLine cmdLine) throws ConfigurationException {
        this.cmdLine = cmdLine;
        extractCommandAndParams(cmdLine.getArgs());
        // Use GUI?
        if (cmdLine.hasOption(OPTION_GUI)) {
            useGui = Boolean.parseBoolean(ConnectBroker.parseAnswer(cmdLine.getOptionValue(OPTION_GUI)));
            log.debug("GUI: " + cmdLine.getOptionValue(OPTION_GUI) + " -> " + useGui);
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
                throw new ConfigurationException(e);
            }
        }
        if (cmdLine.hasOption(OPTION_IGNORE_MISSING)) {
            ignoreMissing = true;
        }
    }

    private void extractCommandAndParams(String[] args) {
        if (args.length > 0) {
            command = args[0];
            log.debug("Launcher command: " + command);
            // Command parameters
            if (args.length > 1) {
                params = Arrays.copyOfRange(args, 1, args.length);
                if (log.isDebugEnabled()) {
                    log.debug("Command parameters: " + ArrayUtils.toString(params));
                }
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
     * @param isStrict if {@code true}, set the launcher strict option
     * @since 7.4
     * @see #OPTION_STRICT_DESC
     */
    protected static void setStrict(boolean isStrict) {
        strict = isStrict;
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
     * Work best with current nuxeoProcess. If nuxeoProcess is null or has exited, then will try to get process ID (so,
     * result in that case depends on OS capabilities).
     *
     * @return true if current process is running or if a running PID is found
     */
    public boolean isRunning() {
        if (nuxeoProcess != null) {
            try {
                nuxeoProcess.exitValue();
                // Previous process has exited
                nuxeoProcess = null;
            } catch (IllegalThreadStateException exception) {
                return true;
            }
        }
        try {
            return (getPid() != null);
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
        if (configurationGenerator.isWizardRequired()) {
            isStarted = isRunning();
        } else {
            try {
                isStarted = isRunning() && statusServletClient.isStarted();
            } catch (SocketTimeoutException e) {
                isStarted = false;
            }
        }
        return isStarted;
    }

    /**
     * @return Server log file
     */
    public File getLogFile() {
        return new File(configurationGenerator.getLogDir(), "server.log");
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
        List<CommandInfo> csetCommands = cset.commands;
        cset = connectBroker.getCommandSet();
        cset.commands.addAll(0, csetCommands);
        try {
            clid = connectBroker.getCLID();
        } catch (NoCLID cause) {
            ;
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
            getConnectBroker(); // Ensure ConnectBroker is instantiated too.
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
        boolean cmdOK = getConnectBroker().pkgAdd(Arrays.asList(pkgNames), ignoreMissing);
        if (!cmdOK) {
            errorValue = EXIT_CODE_ERROR;
        }
        return cmdOK;
    }

    protected boolean pkgInstall(String[] pkgIDs) {
        boolean cmdOK = true;
        if (configurationGenerator.isInstallInProgress()) {
            cmdOK = getConnectBroker().executePending(configurationGenerator.getInstallFile(), true,
                    !cmdLine.hasOption(OPTION_NODEPS), ignoreMissing);
        }
        cmdOK = cmdOK && getConnectBroker().pkgInstall(Arrays.asList(pkgIDs), ignoreMissing);
        if (!cmdOK) {
            errorValue = EXIT_CODE_ERROR;
        }
        return cmdOK;
    }

    protected boolean pkgUninstall(String[] pkgIDs) {
        boolean cmdOK = getConnectBroker().pkgUninstall(Arrays.asList(pkgIDs));
        if (!cmdOK) {
            errorValue = EXIT_CODE_ERROR;
        }
        return cmdOK;
    }

    protected boolean pkgRemove(String[] pkgIDs) {
        boolean cmdOK = getConnectBroker().pkgRemove(Arrays.asList(pkgIDs));
        if (!cmdOK) {
            errorValue = EXIT_CODE_ERROR;
        }
        return cmdOK;
    }

    protected boolean pkgReset() {
        boolean cmdOK = getConnectBroker().pkgReset();
        if (!cmdOK) {
            errorValue = EXIT_CODE_ERROR;
        }
        return cmdOK;
    }

    /**
     * @since 5.6
     */
    protected void printInstanceXMLOutput(InstanceInfo instance) {
        try {
            printInstanceXMLOutput(instance, System.out);
        } catch (JAXBException | XMLStreamException | FactoryConfigurationError e) {
            log.error("Output serialization failed: " + e.getMessage());
            log.debug(e, e);
            errorValue = EXIT_CODE_NOT_RUNNING;
        }
    }

    protected void printInstanceXMLOutput(InstanceInfo instance, OutputStream out)
            throws JAXBException, XMLStreamException, FactoryConfigurationError {
        JAXBContext jaxbContext = JAXBContext.newInstance(InstanceInfo.class, DistributionInfo.class, PackageInfo.class,
                ConfigurationInfo.class, KeyValueInfo.class);
        printXMLOutput(jaxbContext, instance, out);
    }

    /**
     * @since 5.6
     */
    protected void showConfig() {
        log.info("***** Nuxeo instance configuration *****");
        log.info("NUXEO_CONF: " + info.NUXEO_CONF);
        log.info("NUXEO_HOME: " + info.NUXEO_HOME);
        if (info.clid != null) {
            log.info("Instance CLID: " + info.clid);
        }
        // distribution.properties
        log.info("** Distribution");
        log.info("- name: " + info.distribution.name);
        log.info("- server: " + info.distribution.server);
        log.info("- version: " + info.distribution.version);
        log.info("- date: " + info.distribution.date);
        log.info("- packaging: " + info.distribution.packaging);
        // packages
        log.info("** Packages:");
        for (PackageInfo pkg : info.packages) {
            log.info(String.format("- %s (version: %s - id: %s - state: %s)", pkg.name, pkg.version, pkg.id,
                    pkg.state.getLabel()));
        }
        // nuxeo.conf
        log.info("** Templates:");
        log.info("Database template: " + info.config.dbtemplate);
        for (String template : info.config.pkgtemplates) {
            log.info("Package template: " + template);
        }
        for (String template : info.config.usertemplates) {
            log.info("User template: " + template);
        }
        for (String template : info.config.basetemplates) {
            log.info("Base template: " + template);
        }
        log.info("** Settings from nuxeo.conf:");
        for (KeyValueInfo keyval : info.config.keyvals) {
            log.info(String.format("%s=%s", keyval.key, keyval.value));
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
        cmdOK = cmdOK && getConnectBroker().pkgRequest(pkgsToAdd, pkgsToInstall, pkgsToUninstall, pkgsToRemove, true,
                ignoreMissing);
        if (!cmdOK) {
            errorValue = EXIT_CODE_ERROR;
        }
        return cmdOK;
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
        return getConnectBroker().addDistributionPackages();
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
        if (!cmdOK) {
            errorValue = EXIT_CODE_ERROR;
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
        boolean cmdOK = getConnectBroker().pkgShow(Arrays.asList(packages));
        if (!cmdOK) {
            errorValue = EXIT_CODE_ERROR;
        }
        return cmdOK;
    }

    protected boolean dumpConnectReport(OutputStream out, boolean prettyPrint) {
        try (JsonGenerator generator = Json.createGeneratorFactory(
                Collections.singletonMap(JsonGenerator.PRETTY_PRINTING, prettyPrint)).createGenerator(out)) {
            generator.writeStartObject();
            ReportConnector.of().feed(generator);
            generator.writeEnd();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (IOException | ExecutionException cause) {
            log.error("Cannot dump connect report", cause);
            errorValue = EXIT_CODE_ERROR;
            return false;
        }
        return true;
    }
}
