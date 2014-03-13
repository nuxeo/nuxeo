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
 *     Thierry Delprat
 */
package org.nuxeo.ecm.core;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.versioning.DefaultVersionRemovalPolicy;
import org.nuxeo.ecm.core.versioning.OrphanVersionRemovalFilter;
import org.nuxeo.ecm.core.versioning.VersionRemovalPolicy;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;

/**
 * Service used to register version removal policies.
 */
public class CoreService extends DefaultComponent {

    private static final Log log = LogFactory.getLog(CoreService.class);

    private ComponentContext context;

    private VersionRemovalPolicy versionRemovalPolicy;

    private List<OrphanVersionRemovalFilter> orphanVersionRemovalFilters = new ArrayList<OrphanVersionRemovalFilter>();

    public List<OrphanVersionRemovalFilter> getOrphanVersionRemovalFilters() {
        return orphanVersionRemovalFilters;
    }

    public VersionRemovalPolicy getVersionRemovalPolicy() {
        if (versionRemovalPolicy == null) {
            versionRemovalPolicy = new DefaultVersionRemovalPolicy();
        }
        return versionRemovalPolicy;
    }

    @Override
    public void activate(ComponentContext context) {
        this.context = context;
    }

    @Override
    public void deactivate(ComponentContext context) {
        this.context = null;
    }

    @Override
    public void registerExtension(Extension extension) {
        String point = extension.getExtensionPoint();
        if ("versionRemovalPolicy".equals(point)) {
            for (Object contrib : extension.getContributions()) {
                if (contrib instanceof CoreServicePolicyDescriptor) {
                    registerVersionRemovalPolicy((CoreServicePolicyDescriptor) contrib);
                } else {
                    log.error("Invalid contribution to extension point 'versionRemovalPolicy': "
                            + contrib.getClass().getName());
                }
            }
        } else if ("orphanVersionRemovalFilter".equals(point)) {
            for (Object contrib : extension.getContributions()) {
                if (contrib instanceof CoreServiceOrphanVersionRemovalFilterDescriptor) {
                    registerOrphanVersionRemovalFilter((CoreServiceOrphanVersionRemovalFilterDescriptor) contrib);
                } else {
                    log.error("Invalid contribution to extension point 'orphanVersionRemovalFilter': "
                            + contrib.getClass().getName());
                }
            }
        } else {
            log.error("Unknown extension point: " + point);
        }
    }

    @Override
    public void unregisterExtension(Extension extension) {
    }

    private void registerVersionRemovalPolicy(CoreServicePolicyDescriptor desc) {
        String klass = desc.getKlass();
        try {
            versionRemovalPolicy = (VersionRemovalPolicy) context.getRuntimeContext().loadClass(
                    klass).newInstance();
        } catch (Exception e) {
            log.error("Failed to instantiate versionRemovalPolicy: " + klass, e);
        }
    }

    private void registerOrphanVersionRemovalFilter(
            CoreServiceOrphanVersionRemovalFilterDescriptor desc) {
        String klass = desc.getKlass();
        try {
            orphanVersionRemovalFilters.add((OrphanVersionRemovalFilter) context.getRuntimeContext().loadClass(
                    klass).newInstance());
        } catch (Exception e) {
            log.error("Failed to instantiate versionRemovalPolicy: " + klass, e);
        }
    }

}
