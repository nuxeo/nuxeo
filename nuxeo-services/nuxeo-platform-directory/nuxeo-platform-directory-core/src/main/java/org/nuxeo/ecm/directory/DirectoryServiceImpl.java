/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     George Lefter
 *     Olivier Grisel
 *     Benjamin Jalon
 *     Florent Guillaume
 */
package org.nuxeo.ecm.directory;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.nuxeo.ecm.directory.BaseDirectoryDescriptor.DATA_LOADING_POLICY_NEVER_LOAD;
import static org.nuxeo.ecm.directory.localconfiguration.DirectoryConfigurationConstants.DIRECTORY_CONFIGURATION_FACET;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.utils.DurationUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.localconfiguration.LocalConfigurationService;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.directory.localconfiguration.DirectoryConfiguration;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.cluster.ClusterService;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

public class DirectoryServiceImpl extends DefaultComponent implements DirectoryService {

    /** @since 11.1 */
    public static final String CLUSTER_START_DURATION_PROP = "org.nuxeo.directory.cluster.start.duration";

    protected static final String DELIMITER_BETWEEN_DIRECTORY_NAME_AND_SUFFIX = "_";

    private static final Logger log = LogManager.getLogger(DirectoryServiceImpl.class);

    protected DirectoryRegistry registry = new DirectoryRegistry();

    @Override
    public void activate(ComponentContext context) {
    }

    @Override
    public void deactivate(ComponentContext context) {
        registry.shutdown();
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        DirectoryFactoryDescriptor factoryDescriptor = (DirectoryFactoryDescriptor) contribution;
        String factoryName = factoryDescriptor.getFactoryName();
        log.warn("No need to register factoryDescriptor anymore: {}", factoryName);
    }

    @Override
    public void registerDirectoryDescriptor(BaseDirectoryDescriptor descriptor) {
        registry.addContribution(descriptor);
    }

    @Override
    public void unregisterDirectoryDescriptor(BaseDirectoryDescriptor descriptor) {
        registry.removeContribution(descriptor);
    }

    @Override
    public void loadFromCSV(String directoryName, Blob dataBlob, String dataLoadingPolicy) {
        if (isBlank(dataLoadingPolicy) || DATA_LOADING_POLICY_NEVER_LOAD.equals(dataLoadingPolicy)) {
            throw new DirectoryException("Illegal dataLoadingPolicy: " + dataLoadingPolicy, SC_BAD_REQUEST);
        }
        Directory directory = getDirectoryOrFail(directoryName);
        directory.loadFromCSV(dataBlob, dataLoadingPolicy);
    }

    @Override
    public int getApplicationStartedOrder() {
        // earlier than the repository init, which has order 100,
        // but later than the cache service, which has order 95 (100-5)
        return 97;
    }

    @Override
    public void start(ComponentContext context) {
        ClusterService clusterService = Framework.getService(ClusterService.class);
        Duration duration;
        try {
            String prop = Framework.getProperty(CLUSTER_START_DURATION_PROP);
            if (isBlank(prop)) {
                duration = Duration.ZERO;
            } else {
                duration = DurationUtils.parse(prop);
            }
        } catch (DateTimeParseException e) {
            duration = Duration.ZERO;
        }
        if (duration.isZero() || duration.isNegative()) {
            duration = Duration.ofMinutes(1);
        }
        Duration pollDelay = Duration.ofSeconds(1);
        clusterService.runAtomically("start-directories", duration, pollDelay, this::start);
    }

    protected void start() {
        List<Directory> directories = getDirectories();
        directories.forEach(Directory::initialize);
        directories.forEach(Directory::initializeReferences);
        directories.forEach(Directory::initializeInverseReferences);
    }

    protected DirectoryConfiguration getDirectoryConfiguration(DocumentModel documentContext) {
        LocalConfigurationService localConfigurationService = Framework.getService(LocalConfigurationService.class);

        if (localConfigurationService == null) {
            log.info("Local configuration not deployed, will use default configuration");
            return null;
        }

        return localConfigurationService.getConfiguration(DirectoryConfiguration.class, DIRECTORY_CONFIGURATION_FACET,
                documentContext);
    }

    /**
     * This will return the local directory name according the local configuration. If the local configuration is null
     * or the suffix value is null or the suffix value trimmed is an empty string the returned value is the
     * directoryName given in parameter. If not this is directoryName + DELIMITER_BETWEEN_DIRECTORY_NAME_AND_SUFFIX +
     * suffix. if directoryName is null, return null.
     */
    protected String getWaitingLocalDirectoryName(String directoryName, DirectoryConfiguration configuration) {
        if (directoryName == null) {
            return null;
        }

        if (configuration != null && configuration.getDirectorySuffix() != null) {
            String suffix = configuration.getDirectorySuffix().trim();
            if (!"".equals(suffix)) {
                return directoryName + DELIMITER_BETWEEN_DIRECTORY_NAME_AND_SUFFIX + suffix;
            }
            log.warn("The local configuration detected is an empty value, we consider it as no configuration set.");
            log.debug("Directory Local Configuration is on : {}", configuration::getDocumentRef);
        }

        return directoryName;
    }

    @Override
    public BaseDirectoryDescriptor getDirectoryDescriptor(String id) {
        return registry.getDirectoryDescriptor(id);
    }

    @Override
    public Directory getDirectory(String id) {
        if (id == null) {
            // TODO throw an exception
            return null;
        }
        return registry.getDirectory(id);
    }

    @Override
    public Directory getDirectory(String id, DocumentModel documentContext) {
        if (id == null) {
            // TODO throw an exception
            return null;
        }
        String localDirectoryName = getWaitingLocalDirectoryName(id, getDirectoryConfiguration(documentContext));
        Directory dir = getDirectory(localDirectoryName);
        if (dir == null && !id.equals(localDirectoryName)) {
            log.debug("The local directory named '{}' was not found. Look for the default one named: {}",
                    localDirectoryName, id);
            dir = getDirectory(id);
        }
        return dir;
    }

    protected Directory getDirectoryOrFail(String name) {
        return getDirectoryOrFail(name, null);
    }

    protected Directory getDirectoryOrFail(String id, DocumentModel documentContext) {
        Directory dir = getDirectory(id, documentContext);
        if (dir == null) {
            throw new DirectoryException("No directory registered with name: " + id);
        }
        return dir;
    }

    @Override
    public List<Directory> getDirectories() {
        return registry.getDirectories();
    }

    @Override
    public List<String> getDirectoryNames() {
        return registry.getDirectoryIds();
    }

    @Override
    public String getDirectorySchema(String directoryName) {
        return getDirectoryOrFail(directoryName).getSchema();
    }

    @Override
    public String getDirectoryIdField(String directoryName) {
        return getDirectoryOrFail(directoryName).getIdField();
    }

    @Override
    public String getDirectoryPasswordField(String directoryName) {
        return getDirectoryOrFail(directoryName).getPasswordField();
    }

    @Override
    public String getParentDirectoryName(String directoryName) {
        return getDirectoryOrFail(directoryName).getParentDirectory();
    }

    @Override
    public Session open(String directoryName) {
        return getDirectoryOrFail(directoryName).getSession();
    }

    @Override
    public Session open(String directoryName, DocumentModel documentContext) {
        return getDirectoryOrFail(directoryName, documentContext).getSession();
    }

}
