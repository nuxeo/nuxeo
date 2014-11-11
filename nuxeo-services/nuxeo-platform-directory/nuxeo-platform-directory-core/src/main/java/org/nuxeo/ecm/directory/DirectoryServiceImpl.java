/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 * $Id$
 */

package org.nuxeo.ecm.directory;

import static org.nuxeo.ecm.directory.localconfiguration.DirectoryConfigurationConstants.DIRECTORY_CONFIGURATION_FACET;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.localconfiguration.LocalConfigurationService;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.directory.localconfiguration.DirectoryConfiguration;
import org.nuxeo.ecm.directory.memory.MemoryDirectoryFactory;
import org.nuxeo.ecm.directory.registry.DirectoryFactoryMapper;
import org.nuxeo.ecm.directory.registry.DirectoryFactoryMapperRegistry;
import org.nuxeo.ecm.directory.registry.DirectoryFactoryRegistry;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;

public class DirectoryServiceImpl extends DefaultComponent implements
        DirectoryService {

    protected static final String DELIMITER_BETWEEN_DIRECTORY_NAME_AND_SUFFIX = "_";

    private static final Log log = LogFactory.getLog(DirectoryServiceImpl.class);

    protected DirectoryFactoryRegistry factories;

    protected DirectoryFactoryMapperRegistry factoriesByDirectoryName;

    @Override
    public void applicationStarted(ComponentContext context) throws Exception {
        if (Framework.isTestModeSet()) {
            // when testing, DatabaseHelper init hasn't occurred yet,
            // so keep to lazy initialization
            return;
        }
        // open all directories at application startup, so that
        // their tables are created (outside a transaction) if needed
        for (Directory dir : getDirectories()) {
            try {
                dir.getName(); // enough to create tables for SQL directories
            } catch (ClientException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    protected DirectoryConfiguration getDirectoryConfiguration(
            DocumentModel documentContext) {
        DirectoryConfiguration configuration = null;

        try {
            LocalConfigurationService localConfigurationService = Framework.getService(LocalConfigurationService.class);

            if (localConfigurationService == null) {
                log.info("Local configuration not deployed, will use default configuration");
                return null;
            }

            configuration = localConfigurationService.getConfiguration(
                    DirectoryConfiguration.class,
                    DIRECTORY_CONFIGURATION_FACET, documentContext);
        } catch (Exception e) {
            log.error(e, e);
        }
        return configuration;
    }

    /**
     * This will return the local directory name according the local
     * configuration. If the local configuration is null or the suffix value is
     * null or the suffix value trimmed is an empty string the returned value
     * is the directoryName given in parameter. If not this is directoryName +
     * DELIMITER_BETWEEN_DIRECTORY_NAME_AND_SUFFIX + suffix. if directoryName
     * is null, return null.
     */
    protected String getWaitingLocalDirectoryName(String directoryName,
            DirectoryConfiguration configuration) {
        if (directoryName == null) {
            return null;
        }

        if (configuration != null && configuration.getDirectorySuffix() != null) {
            String suffix = configuration.getDirectorySuffix().trim();
            if (!"".equals(suffix)) {
                return directoryName
                        + DELIMITER_BETWEEN_DIRECTORY_NAME_AND_SUFFIX + suffix;
            }
            log.warn("The local configuration detected is an empty value, we consider it as no configuration set.");
            log.debug("Directory Local Configuration is on : "
                    + configuration.getDocumentRef());
        }

        return directoryName;
    }

    public Directory getDirectory(String directoryName)
            throws DirectoryException {
        if (directoryName == null) {
            return null;
        }
        List<String> factoryNames = factoriesByDirectoryName.getFactoriesForDirectory(directoryName);
        if (factoryNames == null || factoryNames.isEmpty()) {
            return null;
        }
        for (String factoryName : factoryNames) {
            DirectoryFactory targetFactory = factories.getFactory(factoryName);
            if (targetFactory != null) {
                return targetFactory.getDirectory(directoryName);
            }
        }
        return null;
    }

    public Directory getDirectory(String name, DocumentModel documentContext)
            throws DirectoryException {
        if (name == null) {
            return null;
        }

        String localDirectoryName = getWaitingLocalDirectoryName(name,
                getDirectoryConfiguration(documentContext));

        Directory directory = getDirectory(localDirectoryName);

        if (directory == null && !name.equals(localDirectoryName)) {
            log.debug(String.format("The local directory named '%s' was"
                    + " not found. Look for the default one named: %s",
                    localDirectoryName, name));
            directory = getDirectory(name);
        }

        return directory;
    }

    private Directory getDirectoryOrFail(String name) throws DirectoryException {
        return getDirectoryOrFail(name, null);
    }

    private Directory getDirectoryOrFail(String name,
            DocumentModel documentContext) throws DirectoryException {

        Directory dir = getDirectory(name, documentContext);
        if (null == dir) {
            throw new DirectoryException(String.format(
                    "no directory registered with name '%s'", name));
        }
        return dir;
    }

    public List<Directory> getDirectories() throws DirectoryException {
        List<Directory> directoryList = new ArrayList<Directory>();
        for (DirectoryFactory factory : factories.getFactories()) {
            List<Directory> list = factory.getDirectories();
            directoryList.addAll(list);
        }
        return directoryList;
    }

    @Override
    public void activate(ComponentContext context) throws Exception {
        factories = new DirectoryFactoryRegistry();
        factoriesByDirectoryName = new DirectoryFactoryMapperRegistry();
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        for (DirectoryFactory factory : factories.getFactories()) {
            factory.shutdown();
        }
        factories = null;
        factoriesByDirectoryName = null;
    }

    @Override
    public void registerExtension(Extension extension) throws Exception {
        Object[] contribs = extension.getContributions();
        for (Object contrib : contribs) {
            DirectoryFactoryDescriptor factoryDescriptor = (DirectoryFactoryDescriptor) contrib;
            String factoryName = factoryDescriptor.getFactoryName();
            factories.addContribution(new DirectoryFactoryProxy(factoryName));
            log.debug("registered factory: " + factoryName);
        }
    }

    @Override
    public void unregisterExtension(Extension extension) throws Exception {
        Object[] contribs = extension.getContributions();
        for (Object contrib : contribs) {
            DirectoryFactoryDescriptor factoryDescriptor = (DirectoryFactoryDescriptor) contrib;
            String factoryName = factoryDescriptor.getFactoryName();
            DirectoryFactory factoryToRemove = factories.getFactory(factoryName);
            if (factoryToRemove == null) {
                log.warn(String.format("Factory '%s' was not registered",
                        factoryName));
                return;
            }
            factoryToRemove.shutdown();
            // XXX: do not cleanup mappers, lookup will ignore non-registered
            // factories anyway
            factories.removeContribution(factoryToRemove);
            log.debug("unregistered factory: " + factoryName);
        }
    }

    public void registerDirectory(String directoryName, DirectoryFactory factory) {
        // compatibility code to add otherwise missing memory factory (as it's
        // not registered via extension points)
        if (factory instanceof MemoryDirectoryFactory) {
            factories.addContribution(factory);
        }
        DirectoryFactoryMapper contrib = new DirectoryFactoryMapper(
                directoryName, factory.getName());
        factoriesByDirectoryName.addContribution(contrib);
    }

    public void unregisterDirectory(String directoryName,
            DirectoryFactory factory) {
        DirectoryFactoryMapper contrib = new DirectoryFactoryMapper(
                directoryName, factory.getName());
        factoriesByDirectoryName.removeContribution(contrib);
    }

    public List<String> getDirectoryNames() throws DirectoryException {
        List<Directory> directories = getDirectories();
        List<String> directoryNames = new ArrayList<String>();
        for (Directory directory : directories) {
            directoryNames.add(directory.getName());
        }
        return directoryNames;
    }

    public String getDirectorySchema(String directoryName)
            throws DirectoryException {
        return getDirectoryOrFail(directoryName).getSchema();
    }

    public String getDirectoryIdField(String directoryName)
            throws DirectoryException {
        return getDirectoryOrFail(directoryName).getIdField();
    }

    public String getDirectoryPasswordField(String directoryName)
            throws DirectoryException {
        return getDirectoryOrFail(directoryName).getPasswordField();
    }

    public Session open(String directoryName) throws DirectoryException {
        return getDirectoryOrFail(directoryName).getSession();
    }

    public Session open(String directoryName, DocumentModel documentContext)
            throws DirectoryException {
        return getDirectoryOrFail(directoryName, documentContext).getSession();
    }

    public String getParentDirectoryName(String directoryName)
            throws DirectoryException {
        return getDirectoryOrFail(directoryName).getParentDirectory();
    }

}
