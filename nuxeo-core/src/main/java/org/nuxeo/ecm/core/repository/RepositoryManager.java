/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.repository;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.model.Repository;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class RepositoryManager {

    public static final String REPOSITORY_EVENT = "repository";

    private static final Log log = LogFactory.getLog(RepositoryManager.class);

    // repository descriptors - used to prevent registering repositories twice
    private final Set<RepositoryDescriptor> descriptors = new HashSet<RepositoryDescriptor>();

    // registered repositories - map the repository name to the repository
    // reference
    private final Map<String, Ref> repositories = new HashMap<String, Ref>();

    private final RepositoryService repositoryService;

    // TODO: is this really needed?
    // currently is used to remove dependency between jca adapter and
    // nxframework

    public RepositoryManager(RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    /**
     * Gets a repository given its name.
     * <p>
     * Null is returned if no repository with that name was registered.
     *
     * @param name the repository name
     * @return the repository instance or null if no repository with that name
     *         was registered
     * @throws Exception if any error occurs when trying to initialize the
     *             repository
     */
    public Repository getRepository(String name) throws Exception {
        if (log.isTraceEnabled()) {
            log.trace("Entering getRepository : " + name);
        }
        Ref ref;
        synchronized (repositories) {
            ref = repositories.get(name);
        }
        if (ref != null) {
            return ref.get();
        } else {
            return null;
        }
    }

    /**
     * Releases the reference to the repository.
     * <p>
     * When the repository reference count becomes 0, the repository is
     * shutdown.
     *
     * @param name the repository name
     */
    public void releaseRepository(String name) {
        if (log.isTraceEnabled()) {
            log.trace("Entering releaseRepository : " + name);
        }
        Ref ref;
        synchronized (repositories) {
            ref = repositories.remove(name);
        }
        if (ref != null) {
            ref.release();
        }
    }

    public Collection<RepositoryDescriptor> getDescriptors() {
        return Collections.unmodifiableCollection(descriptors);
    }

    public String[] getRepositoryNames() {
        synchronized (repositories) {
            return repositories.keySet().toArray(
                    new String[repositories.size()]);
        }
    }

    public RepositoryDescriptor getDescriptor(String name) {
        synchronized (repositories) {
            Ref ref = repositories.get(name);
            if (ref != null) {
                return ref.descriptor;
            }
        }
        return null;
    }

    public void registerRepository(RepositoryDescriptor rd) {
        log.info("Registering repository: " + rd.getName());
        if (!descriptors.contains(rd)) {
            synchronized (repositories) {
                if (!descriptors.contains(rd)) {
                    String name = rd.getName();
                    descriptors.add(rd);
                    repositories.put(name, new Ref(rd));
                    repositoryService.fireRepositoryRegistered(rd);
                }
            }
        }
    }

    public Repository getOrRegisterRepository(RepositoryDescriptor rd)
            throws Exception {
        synchronized (repositories) {
            Ref ref = repositories.get(rd.getName());
            if (ref == null) {
                String name = rd.getName();
                log.info("Registering repository: " + name);
                descriptors.add(rd);
                ref = new Ref(rd);
                repositories.put(name, ref);
                repositoryService.fireRepositoryRegistered(rd);
            }
            return ref.get();
        }
    }

    public void unregisterRepository(RepositoryDescriptor rd) {
        log.info("Unregistering repository: " + rd.getName());
        if (descriptors.contains(rd)) {
            synchronized (repositories) {
                descriptors.remove(rd);
                Ref ref = repositories.remove(rd.getName());
                if (ref != null) {
                    log.info("Unregistering repository: " + rd.getName());
                    repositoryService.fireRepositoryUnRegistered(rd);
                    ref.dispose();
                }
            }
        }
    }

    public void shutdown() {
        log.info("Shutting down repository manager");
        synchronized (repositories) {
            Iterator<Ref> it = repositories.values().iterator();
            while (it.hasNext()) {
                Ref ref = it.next();
                ref.dispose();
                it.remove();
            }
            descriptors.clear();
        }
    }

    public static final class Ref {
        private int refcnt;

        private RepositoryDescriptor descriptor;

        private Repository repository;

        private Ref(RepositoryDescriptor descriptor) {
            this.descriptor = descriptor;
        }

        public synchronized Repository get() throws Exception {
            if (repository == null) {
                refcnt = 0;
                repository = descriptor.create();
            }
            refcnt++;
            return repository;
        }

        public synchronized void release() {
            if (repository != null) {
                if (--refcnt == 0) {
                    repository.shutdown();
                    repository = null;
                }
            }
        }

        public void dispose() {
            if (repository != null) {
                repository.shutdown();
                repository = null;
            }
            refcnt = 0;
            descriptor = null;
        }
    }

}
