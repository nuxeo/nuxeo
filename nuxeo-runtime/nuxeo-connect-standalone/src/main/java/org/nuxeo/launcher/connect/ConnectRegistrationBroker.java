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
 *     akervern, jcarsique
 */

package org.nuxeo.launcher.connect;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.validator.routines.EmailValidator;
import org.nuxeo.connect.NuxeoConnectClient;
import org.nuxeo.connect.connector.NuxeoClientInstanceType;
import org.nuxeo.connect.data.ConnectProject;
import org.nuxeo.connect.identity.LogicalInstanceIdentifier;
import org.nuxeo.connect.registration.ConnectRegistrationService;
import org.nuxeo.connect.registration.RegistrationException;
import org.nuxeo.connect.registration.RegistrationHelper;
import org.nuxeo.launcher.config.ConfigurationException;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 8.3
 */
public class ConnectRegistrationBroker {

    /**
     * @since 9.2
     */
    public enum TrialField {
        FIRST_NAME( //
                "firstName", //
                "First name", //
                input -> Pattern.matches("^(\\p{Alnum}+)([\\s-]\\p{Alnum}+)*", input), //
                "Invalid first name: only letters (without accents), space, and hyphen '-' are accepted." //
        ), LAST_NAME( //
                "lastName", //
                "Last name", //
                input -> Pattern.matches("^(\\p{Alnum}+)([\\s-]\\p{Alnum}+)*", input), //
                "Invalid last name: only letters (without accents), space, and hyphen '-' are accepted." //
        ), EMAIL( //
                "email", //
                "Email", //
                input -> EmailValidator.getInstance().isValid(input), //
                "Invalid email address." //
        ), COMPANY( //
                "company", //
                "Company", //
                input -> Pattern.matches("^(\\p{Alnum}+)([\\s-]\\p{Alnum}+)*", input),
                "Invalid company name: only alphanumeric (without accents), space, and hyphen '-' are accepted." //
        ), PROJECT( //
                "connectreg:projectName", //
                "Project name", //
                input -> Pattern.matches("^(?:[-\\w]+|)$", input), //
                "Project name can only contain alphanumeric characters and dashes." //
        ), TERMS_AND_CONDITIONS( //
                "termsAndConditions", //
                "Terms and conditions", //
                input -> true, // always valid
                "Unused message." //
        );

        private String id;

        private String name;

        private Predicate<String> predicate;

        private String errorMessage;

        TrialField(String id, String name, Predicate<String> predicate, String errorMessage) {
            this.id = id;
            this.name = name;
            this.predicate = predicate;
            this.errorMessage = errorMessage;
        }

        /**
         * @since 9.2
         */
        public String getId() {
            return id;
        }

        /**
         * @since 9.2
         */
        public String getPromptMessage() {
            return name + ": ";
        }

        /**
         * @since 9.2
         */
        public Predicate<String> getPredicate() {
            return predicate;
        }

        /**
         * @since 9.2
         */
        public String getErrorMessage() {
            return errorMessage;
        }
    }

    private static final Log log = LogFactory.getLog(ConnectRegistrationBroker.class);

    protected static ConnectRegistrationService registration() {
        return NuxeoConnectClient.getConnectRegistrationService();
    }

    public void registerTrial(Map<String, String> parameters)
            throws IOException, RegistrationException, ConfigurationException {
        try {
            registration().remoteTrialInstanceRegistration(parameters);
        } catch (LogicalInstanceIdentifier.InvalidCLID e) {
            log.debug(e, e);
            throw new ConfigurationException("Instance registration failed.", e);
        }
    }

    public void registerLocal(String strCLID, String description) throws IOException, ConfigurationException {
        try {
            registration().localRegisterInstance(strCLID, description);
        } catch (LogicalInstanceIdentifier.InvalidCLID e) {
            log.debug(e, e);
            throw new ConfigurationException("Instance registration failed.", e);
        }
    }

    public void registerRemote(String username, char[] password, String projectId, NuxeoClientInstanceType type,
            String description) throws IOException, ConfigurationException {
        String strCLID = RegistrationHelper.remoteRegisterInstance(username, new String(password), projectId, type,
                description);
        registerLocal(strCLID, description);
    }

    /**
     * Find a project by its symbolic name, ignoring case
     *
     * @param projectName project symbolic name
     * @param availableProjects projects in which to to look for {@code project}
     * @return the project or null if not found
     */
    public ConnectProject getProjectByName(final String projectName, List<ConnectProject> availableProjects) {
        Stream<ConnectProject> projectStream = availableProjects.stream();
        Optional<ConnectProject> pkg = projectStream.filter(
                availProject -> projectName.equalsIgnoreCase(availProject.getSymbolicName())).findFirst();
        if (!pkg.isPresent()) {
            return null;
        }
        return pkg.get();
    }

    public List<ConnectProject> getAvailableProjects(String username, char[] password) throws ConfigurationException {
        List<ConnectProject> studioProjects = registration().getAvailableProjectsForRegistration(username,
                new String(password));
        if (studioProjects.isEmpty()) {
            throw new ConfigurationException("Wrong login or password.");
        }
        return studioProjects;
    }
}
