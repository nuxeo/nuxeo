/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *     bstefanescu, atchertchian, jcarsique
 */

package org.nuxeo.osgi;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class OSGiBundleRegistry implements BundleListener {

    private static final Log log = LogFactory.getLog(OSGiBundleRegistry.class);

    protected final OSGiSystemContext osgi;

    protected final OSGiBundleIdGenerator bundleIds = new OSGiBundleIdGenerator();

    protected final Map<Long, OSGiBundleRegistration> bundlesById = new HashMap<Long, OSGiBundleRegistration>();

    protected final Map<Path, OSGiBundleRegistration> bundlesByPath = new HashMap<Path, OSGiBundleRegistration>();

    protected final Map<String, OSGiBundleRegistration> bundlesByName = new LinkedHashMap<String, OSGiBundleRegistration>();

    protected final Map<String, Set<OSGiBundleRegistration>> pendings = new HashMap<String, Set<OSGiBundleRegistration>>();

    public OSGiBundleRegistry(OSGiSystemContext osgi) {
        this.osgi = osgi;
    }

    public void addBundleAlias(String alias, String symbolicName) {
        OSGiBundleRegistration breg = bundlesByName.get(symbolicName);
        if (breg != null) {
            bundlesByName.put(alias, breg);
        }
    }

    public OSGiBundle getBundle(long id) {
        OSGiBundleRegistration reg = bundlesById.get(Long.valueOf(id));
        return reg == null ? null : reg.bundle;
    }

    public OSGiBundle getBundleByName(String symbolicName) {
        OSGiBundleRegistration reg = bundlesByName.get(symbolicName);
        return reg == null ? null : reg.bundle;
    }

    public OSGiBundle getBundleByLocation(String location) {
        File file = new File(URI.create(location));
        Path path = file.toPath().normalize();
        OSGiBundleRegistration reg = bundlesByPath.get(path);
        return reg == null ? null : reg.bundle;
    }

    /**
     *
     * @since 5.6
     */
    public synchronized OSGiBundle[] getFragments(String symbolicName) {
        OSGiBundleRegistration reg = bundlesByName.get(symbolicName);
        List<OSGiBundleFragment> fragments = ((OSGiBundleHost)reg.bundle).fragments;
        return fragments.toArray(new OSGiBundle[fragments.size()]);
    }

    public synchronized OSGiBundle[] getInstalledBundles() {
        OSGiBundle[] bundles = new OSGiBundle[bundlesByName.size()];
        int i = 0;
        for (OSGiBundleRegistration reg : bundlesByName.values()) {
            bundles[i++] = reg.bundle;
        }
        return bundles;
    }

    public void register(OSGiBundle bundle) throws BundleException {
        OSGiBundleRegistration reg = bundlesByName.get(bundle.getSymbolicName());
        if (reg != null) {
            throw new BundleException(bundle + " is already registered");
        }
        register(new OSGiBundleRegistration(bundle));
    }



    protected void register(OSGiBundleRegistration reg) throws BundleException {
        bundlesByName.put(reg.bundle.symbolicName, reg);
        bundlesById.put(
                reg.bundle.id = bundleIds.addBundle(reg.bundle.symbolicName),
                reg);
        bundlesByPath.put(reg.bundle.file.path, reg);
        reg.bundle.osgi = osgi;
        reg.bundle.setInstalled();
        installNested(reg);
        if (OSGiBundleFragment.class.isAssignableFrom(reg.bundle.getClass())) {
            String hostBundleId = getFragmentHost(reg);
            OSGiBundleRegistration host = bundlesByName.get(hostBundleId);
            if (host == null) {
                reg.pendings.add(hostBundleId);
                for (String dep : reg.waitingFor) {
                    Set<OSGiBundleRegistration> regs = pendings.get(dep);
                    if (regs == null) {
                        regs = new HashSet<OSGiBundleRegistration>();
                        pendings.put(dep, regs);
                    }
                    regs.add(reg);
                }
            }
        }

    }

    public synchronized void unregister(OSGiBundle bundle)
            throws BundleException {
        OSGiBundleRegistration reg = bundlesByName.get(bundle.getSymbolicName());
        unregister(reg);
    }

    protected void unregister(OSGiBundleRegistration reg)
            throws BundleException {
        if (getFragmentHost(reg) == null) {
            reg.bundle.stop();
        }

        if (OSGiBundleHost.class.isAssignableFrom(reg.bundle.getClass())) {
            ((OSGiBundleHost) reg.bundle).setUnResolved();
        }
        bundlesByName.remove(reg.bundle.getSymbolicName());
        bundlesById.remove(reg.bundle.getBundleId());
        bundlesByPath.remove(reg.bundle.file.path);
        reg.bundle.setUninstalled();
        for (OSGiBundleRegistration depOnMe : reg.dependsOnMe) {
            if (OSGiBundleHost.class.isAssignableFrom(depOnMe.bundle.getClass())) {
                ((OSGiBundleHost) depOnMe.bundle).setUnResolved();
            }
        }
        uninstallNestedBundles(reg);
    }

    public void resolve() throws BundleException {
        for (long i = 1; i < bundleIds.count; ++i) {
            OSGiBundleRegistration reg = bundlesById.get(Long.valueOf(i));
            if (!pendings.containsKey(reg.bundle.symbolicName)) {
                resolve(reg);
            }
        }
    }

    protected void installNested(OSGiBundleRegistration reg)
            throws BundleException {
        OSGiCompoundBundleExceptionBuilder errors = new OSGiCompoundBundleExceptionBuilder();
        OSGiBundleFile[] files = osgi.getNestedFiles(reg.bundle.file);
        for (OSGiBundleFile file : files) {
            try {
                reg.nested.add(osgi.installBundle(file));
            } catch (BundleException error) {
                errors.add(error);
            }
        }
        errors.throwOnError();
    }

    protected void uninstallNestedBundles(OSGiBundleRegistration reg)
            throws BundleException {
        OSGiCompoundBundleExceptionBuilder errors = new OSGiCompoundBundleExceptionBuilder();
        for (Bundle nestedBundle : reg.nested) {
            try {
                nestedBundle.uninstall();
            } catch (BundleException error) {
                errors.add(error);
            }
        }
        errors.throwOnError();
    }

    protected void resolve(OSGiBundleRegistration reg) throws BundleException {
        String name = reg.bundle.getSymbolicName();

        log.info("Resolved bundle: " + reg.bundle);

        Class<? extends OSGiBundle> bundleType = reg.bundle.getClass();
        if (OSGiBundleFragment.class.isAssignableFrom(bundleType)) {
            String hostBundleId = getFragmentHost(reg);
            OSGiBundleRegistration host = bundlesByName.get(hostBundleId);
            ((OSGiBundleHost)host.bundle).fragments.add((OSGiBundleFragment)reg.bundle);
        } else if (OSGiBundleHost.class.isAssignableFrom(bundleType)) {
            ((OSGiBundleHost) reg.bundle).setResolved();
        }

        // check if there are objects waiting for me
        Set<OSGiBundleRegistration> regs = pendings.remove(name);
        if (regs != null) {
            for (OSGiBundleRegistration pendingReg : regs) {
                pendingReg.pendings.remove(name);
                if (!pendingReg.pendings.isEmpty()) {
                    resolve(pendingReg);
                }
            }
        }
    }

    private String getFragmentHost(OSGiBundleRegistration reg) {
        String hostBundleId = reg.bundle.getHeaders().get(
                Constants.FRAGMENT_HOST);
        if (hostBundleId == null) {
            return null;
        }
        int p = hostBundleId.indexOf(';');
        if (p > -1) { // remove version or other extra information if any
            hostBundleId = hostBundleId.substring(0, p);
        }
        return hostBundleId;
    }

    public void shutdown() {
        OSGiBundleRegistration[] regs = bundlesByName.values().toArray(
                new OSGiBundleRegistration[bundlesByName.size()]);
        for (OSGiBundleRegistration reg : regs) {
            try {
                if (reg.bundle != null) {
                    reg.bundle.uninstall();
                }
            } catch (BundleException e) {
                log.error(
                        "Failed to stop bundle " + reg.bundle.getSymbolicName(),
                        e);
            } catch (RuntimeException e) {
                log.error(
                        "Failed to stop bundle " + reg.bundle.getSymbolicName(),
                        e);
            }
        }
    }

    @Override
    public void bundleChanged(BundleEvent event) {
        OSGiBundle bundle = (OSGiBundle) event.getBundle();
        OSGiBundleRegistration reg = bundlesById.get(bundle.id);
        if ((bundle.getState() & Bundle.RESOLVED) != 0) {
            for (String depName : reg.pendings) {
                reg.resolvedDependencies.add(bundle.osgi.registry.bundlesByName.get(depName));
            }
        } else if ((bundle.getState() & Bundle.INSTALLED) != 0) {
            reg.resolvedDependencies.clear();
        }
    }

}
