/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.connect.update.task.live.commands;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.PackageUpdateComponent;
import org.nuxeo.connect.update.task.Command;
import org.nuxeo.connect.update.task.Task;
import org.nuxeo.connect.update.task.standalone.commands.UninstallPlaceholder;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @deprecated since 5.6, use {@link Undeploy} instead
 */
@Deprecated
public class Uninstall extends UninstallPlaceholder {

    public Uninstall() {
        super();
    }

    public Uninstall(File file) {
        super(file);
    }

    @Override
    protected Command doRun(Task task, Map<String, String> prefs)
            throws PackageException {
        BundleContext ctx = PackageUpdateComponent.getContext().getBundle().getBundleContext();
        JarFile jar = null;
        try {
            jar = new JarFile(file);
            Manifest mf = jar.getManifest();
            String name = mf.getMainAttributes().getValue("Bundle-SymbolicName");
            if (name != null) { // ignore errors
                for (Bundle bundle : ctx.getBundles()) {
                    if (name.equals(bundle.getSymbolicName())) {
                        try {
                            if (bundle.getState() == Bundle.ACTIVE) {
                                bundle.uninstall();
                            }
                        } catch (Throwable t) {
                            // ignore uninstall -> this may break the entire
                            // chain. Usually uninstall is done only when
                            // rollbacking or uninstalling - force restart
                            // required
                            task.setRestartRequired(true);
                        }
                        break;
                    }
                }
            }
        } catch (IOException e) {
            throw new PackageException("Failed to uninstall bundle: "
                    + file.getName(), e);
        } finally {
            if (jar != null) {
                try {
                    jar.close();
                } catch (IOException e) {
                    // nothing to do
                }
            }
        }
        return new Install(file);
    }

}
