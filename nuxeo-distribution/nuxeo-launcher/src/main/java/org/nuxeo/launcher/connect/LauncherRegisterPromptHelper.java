/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo
 */

package org.nuxeo.launcher.connect;

import java.io.Console;
import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.artofsolving.jodconverter.util.PlatformUtils;
import org.nuxeo.connect.connector.NuxeoClientInstanceType;
import org.nuxeo.connect.data.ConnectProject;
import org.nuxeo.launcher.config.ConfigurationException;

import com.sun.istack.internal.NotNull;

/**
 * Helper methods to prompt different things to the user if the shell is in interactive mode.
 *
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 8.3
 */
public class LauncherRegisterPromptHelper {
    private static final Log log = LogFactory.getLog(LauncherRegisterPromptHelper.class);

    private static final int PAGE_SIZE = 20;

    public static final String CONNECT_TC_URL = "https://www.nuxeo.com/en/services/nuxeo-trial-tc";

    public static String promptMail() {
        return prompt("Mail Adress: ", "^.+@.+\\..{2,4}", "Mail is invalid.");
    }

    public static String promptUsername() {
        return prompt("Username: ", "^\\w+", "Username cannot be empty.");
    }

    public static String promptCompany() {
        return prompt("Company: ", "^\\w+", "Company cannot be empty.");
    }

    public static String promptProjectName() {
        return prompt("Project name: ", "^(?:[-\\w]+|)$",
                "Project name can only contain alphanumeric chars and dashes.");
    }

    public static String promptDescription() {
        return prompt("Description: ", "", null);
    }

    protected static String prompt(String message, String regex, String error) {
        if (StringUtils.isBlank(regex)) {
            regex = ".*";
        }

        Console console = System.console();
        String value = null;
        do {
            if (value != null) {
                console.printf(error + "\n", value);
            }
            value = console.readLine(message);
        } while (!value.matches(regex));

        return value;
    }

    public static String promptPassword(boolean confirmation) throws IOException, ConfigurationException {
        String pwd = promptPassword("Please enter your password: ");
        if (confirmation) {
            String pwdVerification = promptPassword("Please re-enter your password: ");
            if (!pwd.equals(pwdVerification)) {
                throw new ConfigurationException("Passwords do not match.");
            }
        }
        return pwd;
    }

    public static NuxeoClientInstanceType promptInstanceType() {
        NuxeoClientInstanceType ret;
        Console console = System.console();

        do {
            String s = console.readLine("Instance type (dev|preprod|prod) (default: dev): ");
            if (StringUtils.isBlank(s)) {
                ret = NuxeoClientInstanceType.DEV;
            } else {
                ret = NuxeoClientInstanceType.fromString(s);
            }
        } while (ret == null);

        return ret;
    }

    public static String promptPassword() throws IOException, ConfigurationException {
        return promptPassword(false);
    }

    protected static String promptPassword(String message) throws IOException {
        Console console = System.console();
        return new String(console.readPassword(message));
    }

    public static boolean promptAcceptTerms() {
        Console console = System.console();
        if (console != null) {
            String terms = console.readLine("Read and accept the Nuxeo Trial Terms and Conditions - " + CONNECT_TC_URL
                    + " (Y/n): ");
            return terms != null && (terms.isEmpty() || "y".equalsIgnoreCase(terms) || "yes".equalsIgnoreCase(terms));
        } else {
            log.info("Read Nuxeo Trial Terms and Conditions - " + CONNECT_TC_URL);
            return true;
        }
    }

    public static String promptProjectId(@NotNull List<ConnectProject> projects) throws ConfigurationException {
        Console console = System.console();
        if (console == null) {
            throw new ConfigurationException("Can't configure project in an non-interactive shell.");
        }
        if (projects.isEmpty()) {
            throw new ConfigurationException("You don't have access to any project.");
        }

        if (projects.size() == 1) {
            return projects.get(0).getUuid();
        }
        System.out.println("Available projects:");
        if (projects.size() > PAGE_SIZE) {
            int i = 0;
            boolean loop = true;
            String projectName = "";
            do {
                if (i > 0 && !PlatformUtils.isWindows()) {
                    // Remove "next page" line to only have projects
                    System.out.print("\33[1A\33[2K");
                }

                int fromIndex = i * PAGE_SIZE;
                int toIndex = (i + 1) * PAGE_SIZE;
                if (toIndex >= projects.size()) {
                    toIndex = projects.size();
                    loop = false;
                }

                projects.subList(fromIndex, toIndex).forEach(
                        project -> System.out.println("\t- " + project.getSymbolicName()));
                if (toIndex < projects.size()) {
                    int pageLeft = (int) Math.ceil((projects.size() - (i * PAGE_SIZE)) / PAGE_SIZE);
                    System.out.print("Press enter for next page (" + pageLeft
                            + " page(s) left) or enter a project name: ");
                }
                i++;
            } while (loop && (projectName = console.readLine()).isEmpty());

            if (!projectName.isEmpty()) {
                return projectName;
            }
        } else {
            projects.forEach(project -> System.out.println("\t- " + project.getSymbolicName()));
        }
        return console.readLine("Selected project name: ");
    }
}
