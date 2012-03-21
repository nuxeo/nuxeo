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
package org.nuxeo.connect.update.standalone.task.commands;

import java.io.File;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.PackageUpdateComponent;
import org.nuxeo.connect.update.ValidationStatus;
import org.nuxeo.connect.update.xml.XmlWriter;
import org.nuxeo.connect.update.task.Task;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.w3c.dom.Element;

/**
 * Un-Deploy an OSGi bundle from the running platform. The bundle is specified
 * using the absolute bundle file path. The inverse of this command is the
 * Deploy command.
 *
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Uninstall extends AbstractCommand {

    public static final String ID = "uninstall";

    protected File file;

    public Uninstall() {
        super(ID);
    }

    public Uninstall(File file) {
        super(ID);
        this.file = file;
    }

    @Override
    protected void doValidate(Task task, ValidationStatus status)
            throws PackageException {
        if (file == null) {
            status.addError("Invalid uninstall syntax: No file specified");
        }
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
        } catch (Exception e) {
            throw new PackageException("Failed to uninstall bundle: "
                    + file.getName(), e);
        } finally {
            if (jar != null) {
                try {
                    jar.close();
                } catch (Exception e) {
                    // do nothing
                }
            }
        }
        return new Install(file);
    }

    public void readFrom(Element element) throws PackageException {
        String v = element.getAttribute("file");
        if (v.length() > 0) {
            file = new File(v);
            guardVars.put("file", file);
        }
    }

    public void writeTo(XmlWriter writer) {
        writer.start(ID);
        if (file != null) {
            writer.attr("file", file.getAbsolutePath());
        }
        writer.end();
    }
}
