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

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.connect.NuxeoConnectClient;
import org.nuxeo.connect.connector.NuxeoClientInstanceType;
import org.nuxeo.connect.data.ConnectProject;
import org.nuxeo.connect.identity.LogicalInstanceIdentifier;
import org.nuxeo.connect.registration.ConnectRegistrationService;
import org.nuxeo.connect.registration.RegistrationException;
import org.nuxeo.connect.registration.RegistrationHelper;
import org.nuxeo.connect.update.PackageUtils;
import org.nuxeo.launcher.config.ConfigurationException;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 8.3
 */
public class ConnectRegistrationBroker {

    private static final Log log = LogFactory.getLog(ConnectRegistrationBroker.class);

    protected static List<ConnectProject> studioProjects;

    protected static ConnectRegistrationService registration() {
        return NuxeoConnectClient.getConnectRegistrationService();
    }

    public void registerTrial(Map<String, String> parameters) throws IOException, RegistrationException,
            ConfigurationException {
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

    public void registerRemote(String username, String password, String project, NuxeoClientInstanceType type,
            String description) throws IOException, ConfigurationException {
        if (!PackageUtils.isValidPackageId(project)) {
            final String finalProject = project;
            Stream<ConnectProject> projectStream = getAvailableProjects(username, password).stream();
            Optional<ConnectProject> pkg = projectStream.filter(
                    availProject -> finalProject.equalsIgnoreCase(availProject.getSymbolicName())).findFirst();

            if (!pkg.isPresent()) {
                throw new ConfigurationException("Unable to find corresponding project: " + project);
            }
            project = pkg.get().getUuid();
        }

        String strCLID = RegistrationHelper.remoteRegisterInstance(username, password, project, type, description);
        registerLocal(strCLID, description);
    }

    public List<ConnectProject> getAvailableProjects(String username, String password) throws ConfigurationException {
        if (studioProjects == null) {
            studioProjects = registration().getAvailableProjectsForRegistration(username, password);
            if (studioProjects.isEmpty()) {
                throw new ConfigurationException("Wrong login and password.");
            }
        }

        return studioProjects;
    }
}
