/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.repository;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.NXCore;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.local.LocalSession;
import org.nuxeo.ecm.core.model.NoSuchRepositoryException;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;
import org.nuxeo.runtime.services.event.Event;
import org.nuxeo.runtime.services.event.EventListener;
import org.nuxeo.runtime.services.event.EventService;

/**
 * @author Bogdan Stefanescu
 * @author Florent Guillaume
 */
@SuppressWarnings({"SuppressionAnnotation"})
public class RepositoryService extends DefaultComponent implements EventListener {

    public static final ComponentName NAME = new ComponentName("org.nuxeo.ecm.core.repository.RepositoryService");

    private static final Log log = LogFactory.getLog(RepositoryService.class);

    // event IDs
    public static final String REPOSITORY = "repository";
    public static final String REPOSITORY_REGISTERED = "registered";
    public static final String REPOSITORY_UNREGISTERED = "unregistered";

    private RepositoryManager repositoryMgr;
    private EventService eventService;


    @Override
    public void activate(ComponentContext context) throws Exception {
        repositoryMgr = new RepositoryManager(this);
        eventService = (EventService) context.getRuntimeContext().getRuntime().getComponent(EventService.NAME);
        if (eventService == null) {
            throw new Exception("Event Service was not found");
        }
        eventService.addListener(REPOSITORY, this);
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        repositoryMgr.shutdown();
        repositoryMgr = null;
    }

    void fireRepositoryRegistered(RepositoryDescriptor rd) {
        eventService.sendEvent(new Event(REPOSITORY, REPOSITORY_REGISTERED, this, rd.getName()));
    }

    void fireRepositoryUnRegistered(RepositoryDescriptor rd) {
        eventService.sendEvent(new Event(REPOSITORY, REPOSITORY_UNREGISTERED, this, rd.getName()));
    }

    // org.nuxeo.runtime.services.event.EventListener
    @Override
    public boolean aboutToHandleEvent(Event event) {
        return false;
    }

    // org.nuxeo.runtime.services.event.EventListener
    @Override
    public void handleEvent(Event event) {
        if (event.getId().equals(REPOSITORY_UNREGISTERED)) {
            String name = (String) event.getData();
            try {
                Repository repo = NXCore.getRepository(name);
                log.info("Closing repository: " + name);
                repo.shutdown();
            } catch (NoSuchRepositoryException e) {
                // already torn down
            } catch (Exception e) {
                log.error("Failed to close repository: " + name, e);
            }
        }
    }

    @Override
    public void registerExtension(Extension extension) throws Exception {
        Object[] repos = extension.getContributions();
        if (repos != null) {
            for (Object repo : repos) {
                repositoryMgr.registerRepository((RepositoryDescriptor) repo);
            }
        }
    }

    @Override
    public void unregisterExtension(Extension extension) throws Exception {
        super.unregisterExtension(extension);
        Object[] repos = extension.getContributions();
        for (Object repo : repos) {
            repositoryMgr.unregisterRepository((RepositoryDescriptor) repo);
        }
    }

    public RepositoryManager getRepositoryManager() {
        return repositoryMgr;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(CoreSession.class)) {
            return (T) LocalSession.createInstance();
        }
        return null;
    }

}
