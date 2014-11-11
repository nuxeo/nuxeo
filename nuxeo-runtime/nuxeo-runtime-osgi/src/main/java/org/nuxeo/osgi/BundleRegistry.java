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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class BundleRegistry {

    private static final Log log = LogFactory.getLog(BundleRegistry.class);

    private final Map<Long, BundleRegistration> bundlesById;

    private final Map<String, BundleRegistration> bundles;

    private final Map<String, Set<BundleRegistration>> pendings;

    public BundleRegistry() {
        bundlesById = new HashMap<Long, BundleRegistration>();
        bundles = new LinkedHashMap<String, BundleRegistration>();
        pendings = new HashMap<String, Set<BundleRegistration>>();
    }

    public void addBundleAlias(String alias, String symbolicName) {
        BundleRegistration breg = bundles.get(symbolicName);
        if (breg != null) {
            bundles.put(alias, breg);
        }
    }

    public synchronized BundleImpl getBundle(long id) {
        BundleRegistration reg = bundlesById.get(id);
        return reg == null ? null : reg.bundle;
    }

    public synchronized BundleImpl getBundle(String symbolicName) {
        BundleRegistration reg = bundles.get(symbolicName);
        return reg == null ? null : reg.bundle;
    }

    /**
     *
     * @since 5.6
     */
    public synchronized BundleImpl[] getFragments(String symbolicName) {
        BundleRegistration reg = bundles.get(symbolicName);

        ArrayList<BundleImpl> fragments = new ArrayList<BundleImpl>();
        for (String id:reg.extendsMe) {
            fragments.add(getBundle(id));
        }
        return fragments.toArray(new BundleImpl[fragments.size()]);
    }

    public synchronized BundleImpl[] getInstalledBundles() {
        BundleImpl[] bundles = new BundleImpl[this.bundles.size()];
        int i = 0;
        for (BundleRegistration reg : this.bundles.values()) {
            bundles[i++] = reg.bundle;
        }
        return bundles;
    }

    public synchronized void install(BundleImpl bundle) throws BundleException {
        if (bundle.getState() == Bundle.UNINSTALLED) {
            BundleRegistration reg = bundles.get(bundle.getSymbolicName());
            if (reg == null) {
                register(new BundleRegistration(bundle));
            } else {
                register(reg);
            }
        }
    }

    public synchronized void uninstall(BundleImpl bundle)
            throws BundleException {
        if (bundle.getState() != Bundle.UNINSTALLED) {
            BundleRegistration reg = bundles.get(bundle.getSymbolicName());
            if (reg != null) {
                unregister(reg);
            }
        }
    }

    private void register(BundleRegistration reg) throws BundleException {
        String hostBundleId = getFragmentHost(reg);
        if (hostBundleId != null) {
            BundleRegistration host = bundles.get(hostBundleId);
            if (host == null) {
                reg.addUnresolvedDependency(hostBundleId);
            }
        }
        if (reg.hasUnresolvedDependencies()) {
            doPostpone(reg);
        } else {
            doRegister(reg);
        }
    }

    protected void unregister(BundleRegistration reg) throws BundleException {
        if (getFragmentHost(reg) == null) {
            reg.bundle.stop();
        }

        reg.bundle.setUnResolved();
        bundles.remove(reg.bundle.getSymbolicName());
        bundlesById.remove(reg.bundle.getBundleId());
        reg.bundle.setUninstalled();
        for (String depOnMe : reg.dependsOnMe) {
            BundleRegistration depReg = bundles.get(depOnMe);
            if (depReg != null) { // set to unresolved
                depReg.bundle.setUnResolved();
            }
        }
    }

    protected void doPostpone(BundleRegistration reg) {
        String name = reg.bundle.getSymbolicName();
        log.info("Registering unresolved bundle: " + name);
        bundles.put(name, reg);
        bundlesById.put(reg.bundle.getBundleId(), reg);

        for (String dep : reg.waitingFor) {
            Set<BundleRegistration> regs = pendings.get(dep);
            if (regs == null) {
                regs = new HashSet<BundleRegistration>();
                pendings.put(dep, regs);
            }
            regs.add(reg);
        }
        reg.bundle.setInstalled();
    }

    protected void doRegister(BundleRegistration reg) throws BundleException {
        String name = reg.bundle.getSymbolicName();
        log.info("Registering resolved bundle: " + name);
        bundles.put(name, reg);
        bundlesById.put(reg.bundle.getBundleId(), reg);
        reg.bundle.setInstalled();
        reg.bundle.setResolved();

        String hostBundleId = getFragmentHost(reg);
        if (hostBundleId != null) {
            BundleRegistration host = bundles.get(hostBundleId);
            host.addFragment(reg.bundle.getSymbolicName());
        } else {
            // TODO how to lazy start the bundle?
            reg.bundle.start();
        }

        // check if there are objects waiting for me
        Set<BundleRegistration> regs = pendings.remove(name);
        if (regs != null) {
            for (BundleRegistration pendingReg : regs) {
                pendingReg.removeUnresolvedDependency(name);
                if (!pendingReg.hasUnresolvedDependencies()) {
                    doRegister(pendingReg);
                }
            }
        }
    }

    private String getFragmentHost(BundleRegistration reg) {
        String hostBundleId = reg.bundle.getHeaders().get(Constants.FRAGMENT_HOST);
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
        BundleRegistration[] regs = bundles.values().toArray(
                new BundleRegistration[bundles.size()]);
        for (BundleRegistration reg : regs) {
            try {
                if (reg.bundle != null) {
                    reg.bundle.shutdown();
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

}
