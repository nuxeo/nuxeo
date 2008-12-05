/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.nuxeo.ecm.core.jca;

import java.io.PrintWriter;
import java.util.Set;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.security.auth.Subject;

import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.repository.RepositoryDescriptor;
import org.nuxeo.ecm.core.repository.RepositoryFactory;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.runtime.api.Framework;

/**
 * Implements the JCA ManagedConnectionFactory contract.
 * <p>
 * These sources are based on the JackRabbit JCA implementation
 * (http://jackrabbit.apache.org/).
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public final class JCAManagedConnectionFactory
        implements ManagedConnectionFactory {

    private static final long serialVersionUID = -8240471437665772556L;

    // the repository name - needed to be able to lazy load the repository descriptor
    // (the repository may not be yet registered at time of data source deployment)
    private String name;

    // loaded lazily when the repository is registered through nuxeo runtime extensions
    private RepositoryDescriptor descriptor;

    final RepositoryService repositoryService;

    /**
     * Repository.
     */
    private transient Repository repository;

    /**
     * Log writer.
     */
    private transient PrintWriter logWriter;


    public JCAManagedConnectionFactory() {
        repositoryService = (RepositoryService) Framework.getRuntime()
            .getComponent(RepositoryService.NAME);
        assert repositoryService != null;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /**
     * Returns the repository descriptor.
     */
    public RepositoryDescriptor getDescriptor() {
        if (descriptor == null) {
            descriptor = repositoryService.getRepositoryManager().getDescriptor(name);
            descriptor.setName(name);
            if (descriptor == null) {
                throw new IllegalArgumentException(
                        "Could not find any registered repository with that name: "
                        + name);
            }
        }
        return descriptor;
    }

    /**
     * Gets the log writer.
     */
    public PrintWriter getLogWriter() {
        return logWriter;
    }

    /**
     * Sets the log writer.
     */
    public void setLogWriter(PrintWriter logWriter) {
        this.logWriter = logWriter;
    }

    /**
     * Creates a Connection Factory instance.
     */
    public Object createConnectionFactory() {
        return createConnectionFactory(new JCAConnectionManager());
    }

    /**
     * Creates a Connection Factory instance.
     */
    public Object createConnectionFactory(ConnectionManager cm) {
        JCAConnectionFactory handle = new JCAConnectionFactory(this, cm);
        log("Created repository handle (" + handle + ')');
        return handle;
    }

    /**
     * Creates a new physical connection to the underlying EIS resource manager.
     */
    public ManagedConnection createManagedConnection(Subject subject,
            ConnectionRequestInfo cri) {
        return createManagedConnection((JCAConnectionRequestInfo) cri);
    }

    /**
     * Creates a new physical connection to the underlying EIS resource manager.
     */
    private ManagedConnection createManagedConnection(JCAConnectionRequestInfo cri) {
        return new JCAManagedConnection(this, cri);
    }

    /**
     * Returns a matched connection from the candidate set of connections.
     */
    public ManagedConnection matchManagedConnections(Set set, Subject subject,
            ConnectionRequestInfo cri)
            throws ResourceException {
        for (Object next : set) {
            if (next instanceof JCAManagedConnection) {
                JCAManagedConnection mc = (JCAManagedConnection) next;
                if (equals(mc.getManagedConnectionFactory())) {
                    if (!mc.isHandleValid()) { // reuse the first inactive mc
                        // reinitialize the connection
                        mc.initializeHandle((JCAConnectionRequestInfo) cri);
                        return mc;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Gets the repository.
     * Lazy create it if not yet created.
     *
     * @return the repository.
     */
    public Repository getRepository() {
        if (repository == null) {
            try {
                createRepository();
            } catch (ResourceException e) { // fatal error
                throw new RuntimeException("Failed to intialize repository: "+name, e);
            }
        }
        return repository;
    }

    /**
     * Logs a message.
     */
    public void log(String message) {
        log(message, null);
    }

    /**
     * Logs a message.
     */
    public void log(String message, Throwable exception) {
        if (logWriter != null) {
            logWriter.println(message);

            if (exception != null) {
                exception.printStackTrace(logWriter);
            }
        }
    }

    /**
     * Returns the hash code.
     */
    @Override
    public int hashCode() {
        assert name != null;
        return name.hashCode();
    }

    /**
     * Returns true if equals.
     */
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof JCAManagedConnectionFactory) {
            return equals((JCAManagedConnectionFactory) o);
        } else {
            return false;
        }
    }

    /**
     * Returns true if equals.
     */
    private boolean equals(JCAManagedConnectionFactory o) {
        assert name != null;
        return name.equals(o.name);
    }


    /**
     * Shuts down the repository.
     */
    @Override
    protected void finalize() {
        shutdownRepository();
    }

    /**
     * Creates repository.
     */
    private void createRepository() throws ResourceException {
        RepositoryDescriptor descriptor = getDescriptor();
        // Check the home directory
        String homeDir = descriptor.getHomeDirectory();
        if (homeDir == null || homeDir.equals("")) {
            log("Error: Property 'homeDir' not set");
            throw new ResourceException("Property 'homeDir' not set");
        }

        // Check the config file
        String configFile = descriptor.getConfigurationFile();
        if (configFile == null || configFile.equals("")) {
            log("Error: Property 'configFile' not set");
            throw new ResourceException("Property 'configFile' not set");
        }

        // Check the factory class
        Class<RepositoryFactory> factoryClass = descriptor.getFactoryClass();
        if (factoryClass == null) {
            log("Error: Property 'factory' not set");
            throw new ResourceException("Property 'factory' not set");
        }

        // Check the name
        String name = descriptor.getName();
        if (name == null || name.equals("")) {
            log("Error: Property 'name' not set");
            throw new ResourceException("Property 'name' not set");
        }

        try {
            // register if needed and get a reference to the repository
            repository = repositoryService.getRepositoryManager()
            .getOrRegisterRepository(descriptor);
            log("Created repository " + descriptor.getName() + " (" + repository + ')');
        } catch (Exception e) {
            log("Failed to create repository", e);
            throw new ResourceException(e);
        }
    }

    /**
     * Shuts down the repository.
     */
    void shutdownRepository() {
        if (repository != null && name != null) {
            log("Shutdown repository connection");
            repositoryService.getRepositoryManager()
                .releaseRepository(getDescriptor().getName());
            repository = null;
        }
    }

}
