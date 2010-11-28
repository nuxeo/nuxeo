/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.admin.runtime;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.PropertyResourceBundle;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.nuxeo.osgi.BundleFile;
import org.nuxeo.osgi.BundleImpl;
import org.nuxeo.osgi.JarBundleFile;
import org.nuxeo.osgi.jboss.JBossBundleFile;
import org.nuxeo.runtime.RuntimeService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.RegistrationInfo;
import org.osgi.framework.Bundle;

/**
 * Extracts information about the Bundles currently deployed in Nuxeo Runtime
 *
 * @author tiry
 */
public class RuntimeInstrospection {

    protected static SimplifiedServerInfo info;

    public static synchronized SimplifiedServerInfo getInfo() {
        if (info == null) {
            RuntimeService runtime = Framework.getRuntime();
            Collection<RegistrationInfo> registrations = runtime.getComponentManager().getRegistrations();

            List<String> bundleIds = new ArrayList<String>();

            List<SimplifiedBundleInfo> bundles = new ArrayList<SimplifiedBundleInfo>();

            for (RegistrationInfo ri : registrations) {
                Bundle bundle = ri.getContext().getBundle();
                if (bundle != null
                        && !bundleIds.contains(bundle.getSymbolicName())) {
                    SimplifiedBundleInfo bi = getBundleSimplifiedInfo(bundle);
                    bundleIds.add(bundle.getSymbolicName());
                    if (bi != null) {
                        bundles.add(bi);
                    }
                }
            }

            Collections.sort(bundles);
            info = new SimplifiedServerInfo();
            info.setBundleInfos(bundles);
            info.setRuntimeVersion(runtime.getVersion().toString());
            info.setWarnings(runtime.getWarnings());
        }
        return info;
    }

    @SuppressWarnings("unchecked")
    protected static SimplifiedBundleInfo getBundleSimplifiedInfo(Bundle bundle) {
        SimplifiedBundleInfo result = null;
        if (bundle instanceof BundleImpl) {
            BundleImpl nxBundle = (BundleImpl) bundle;
            BundleFile file = nxBundle.getBundleFile();
            File jarFile = null;
            if (file instanceof JarBundleFile) {
                JarBundleFile jar = (JarBundleFile) file;
                jarFile = jar.getFile();
            } else if (file instanceof JBossBundleFile) {
                JBossBundleFile jar = (JBossBundleFile) file;
                jarFile = jar.getFile();
            }
            if (jarFile != null) {
                if (jarFile.isDirectory()) {
                    // XXX
                } else {
                    try {
                        ZipFile zFile = new ZipFile(jarFile);
                        Enumeration<ZipEntry> entries = (Enumeration<ZipEntry>) zFile.entries();
                        while (entries.hasMoreElements()) {
                            ZipEntry entry = entries.nextElement();
                            if (entry.getName().endsWith("pom.properties")) {
                                InputStream pomStream = zFile.getInputStream(entry);
                                PropertyResourceBundle prb = new PropertyResourceBundle(
                                        pomStream);
                                String version = prb.getString("version");
                                result = new SimplifiedBundleInfo(
                                        bundle.getSymbolicName(), version);
                                pomStream.close();
                                break;
                            }
                        }
                    } catch (Exception e) {
                        // NOP
                    }

                }
            }
        }
        return result;
    }
}
