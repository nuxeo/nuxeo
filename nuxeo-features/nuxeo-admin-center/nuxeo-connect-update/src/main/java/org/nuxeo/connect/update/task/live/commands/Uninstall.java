/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
import org.osgi.framework.BundleException;

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
    protected Command doRun(Task task, Map<String, String> prefs) throws PackageException {
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
                        } catch (BundleException e) {
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
            throw new PackageException("Failed to uninstall bundle: " + file.getName(), e);
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
