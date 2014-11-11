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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;

public class DirectoryServiceImpl extends DefaultComponent implements
        DirectoryService {

    private static final Log log = LogFactory.getLog(DirectoryServiceImpl.class);

    private Map<String, DirectoryFactory> factories;

    private Map<String, List<DirectoryFactory>> factoriesByDirectoryName;

    public Directory getDirectory(String name) throws DirectoryException {
        List<DirectoryFactory> potentialFactories = factoriesByDirectoryName.get(name);
        if (potentialFactories == null) {
            return null;
        }
        Directory dir = null;
        for (DirectoryFactory factory : potentialFactories) {
            dir = factory.getDirectory(name);
            if (null != dir) {
                break;
            }
        }
        return dir;
    }

    private Directory getDirectoryOrFail(String name) throws DirectoryException {
        Directory dir = getDirectory(name);
        if (null == dir) {
            throw new DirectoryException(String.format(
                    "no directory registered with name '%s'", name));
        }
        return dir;
    }

    public List<Directory> getDirectories() throws DirectoryException {
        List<Directory> directoryList = new ArrayList<Directory>();
        for (DirectoryFactory factory : factories.values()) {
            List<Directory> list = factory.getDirectories();
            directoryList.addAll(list);
        }
        return directoryList;
    }

    @Override
    public void activate(ComponentContext context) throws Exception {
        factories = new HashMap<String, DirectoryFactory>();
        factoriesByDirectoryName = new HashMap<String, List<DirectoryFactory>>();
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        for (DirectoryFactory factory : factories.values()) {
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
            factories.put(factoryName, new DirectoryFactoryProxy(factoryName));
            log.debug("registered factory: " + factoryName);
        }
    }

    @Override
    public void unregisterExtension(Extension extension) throws Exception {
        Object[] contribs = extension.getContributions();
        for (Object contrib : contribs) {
            DirectoryFactoryDescriptor factoryDescriptor = (DirectoryFactoryDescriptor) contrib;
            String factoryName = factoryDescriptor.getFactoryName();
            DirectoryFactory factoryToRemove = factories.get(factoryName);
            if (factoryToRemove == null) {
                log.warn("factory: " + factoryName + "was not registered");
                return;
            }
            factoryToRemove.shutdown();

            for (List<DirectoryFactory> potentialFactories : factoriesByDirectoryName.values()) {
                potentialFactories.remove(factoryToRemove);
            }

            factories.remove(factoryName);
            log.debug("unregistered factory: " + factoryName);
        }
    }

    public void registerDirectory(String directoryName, DirectoryFactory factory) {
        List<DirectoryFactory> existingFactories = factoriesByDirectoryName.get(directoryName);
        if (existingFactories == null) {
            existingFactories = new ArrayList<DirectoryFactory>();
            factoriesByDirectoryName.put(directoryName, existingFactories);
        }
        // remove existing occurrence of the factory if any to put factory in
        // the first position
        existingFactories.remove(factory);
        existingFactories.add(0, factory);
    }

    public void unregisterDirectory(String directoryName,
            DirectoryFactory factory) {
        List<DirectoryFactory> existingFactories = factoriesByDirectoryName.get(directoryName);
        if (existingFactories != null) {
            existingFactories.remove(factory);
        }
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

    public String getParentDirectoryName(String directoryName)
            throws DirectoryException {
        return getDirectoryOrFail(directoryName).getParentDirectory();
    }

}
