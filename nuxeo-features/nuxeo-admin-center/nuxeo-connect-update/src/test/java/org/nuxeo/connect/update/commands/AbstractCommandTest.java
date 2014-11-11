/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
package org.nuxeo.connect.update.commands;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.common.Environment;
import org.nuxeo.connect.update.LocalPackage;
import org.nuxeo.connect.update.PackageState;
import org.nuxeo.connect.update.PackageTestCase;
import org.nuxeo.connect.update.PackageType;
import org.nuxeo.connect.update.ValidationStatus;
import org.nuxeo.connect.update.impl.xml.XmlWriter;
import org.nuxeo.connect.update.task.Task;
import org.nuxeo.connect.update.util.PackageBuilder;

/**
 * A base test case for testing command execution.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public abstract class AbstractCommandTest extends PackageTestCase {

    @Override
    @Before
    public void setUp() throws Exception {
        // be sure these directories exists
        Environment.getDefault().getConfig().mkdirs();
        new File(Environment.getDefault().getHome(), "bundles").mkdirs();
        new File(Environment.getDefault().getHome(), "lib").mkdirs();
    }

    /**
     * Override this method to add to the package any resources required y the
     * command execution.
     */
    protected abstract void updatePackage(PackageBuilder builder)
            throws Exception;

    /**
     * Override this method to write the command XML definition to test.
     */
    protected abstract void writeCommand(XmlWriter writer);

    /**
     * Override this method to check the install outcome. If the
     * <code>error</code> argument is not null then a rollback was done. In that
     * case you must check the rollback outcome.
     *
     * @param task the executed task.
     * @param error always null if task successfully executed. Not null if a
     *            rollback occurred.
     */
    protected abstract void installDone(Task task, Throwable error)
            throws Exception;

    /**
     * Override this method to check the uninstall outcome. If the
     * <code>error</code> argument is not null then a rollback was done. In that
     * case you must check the rollback outcome.
     *
     * @param task the executed task.
     * @param error always null if task successfully executed. Not null if a
     *            rollback occurred.
     */
    protected abstract void uninstallDone(Task task, Throwable error)
            throws Exception;

    /**
     * Override this method if the install execution is expected to not be
     * validated - in that case check the execution status for consistency. The
     * default implementation expects valid task and will fails if any errors
     * are found in the validation status.
     *
     * @param task the task to execute
     * @param status the validation status to check
     */
    protected boolean validateInstall(Task task, ValidationStatus status) {
        if (status.hasErrors()) {
            fail("Unexpected Validation Errors: " + status.getErrors());
        }
        return true;
    }

    /**
     * Override this method if the uninstall execution is expected to not be
     * validated - in that case check the execution status for consistency. The
     * default implementation expects valid task and will fails if any errors
     * are found in the validation status.
     *
     * @param task the task to execute
     * @param status the validation status to check
     */
    protected boolean validateUninstall(Task task, ValidationStatus status) {
        if (status.hasErrors()) {
            fail("Unexpected Validation Errors: " + status.getErrors());
        }
        return true;
    }

    /* test methods */

    protected File createPackage() throws Exception {
        PackageBuilder builder = new PackageBuilder();
        builder.name("nuxeo-automation").version("5.3.2").type(
                PackageType.ADDON);
        builder.platform("dm-5.3.2");
        builder.dependency("nuxeo-automation:5.3.2");
        builder.title("Test Package");
        builder.description("A test package");
        builder.classifier("Open Source");
        builder.vendor("Nuxeo");
        // builder.installer(InstallTask.class.getName(), false);
        // builder.uninstaller(UninstallTask.class.getName(), false);
        builder.addLicense("My test license. All rights reserved.");
        updatePackage(builder);
        XmlWriter writer = new XmlWriter();
        writer.start("install");
        writer.startContent();
        writeCommand(writer);
        writer.end("install");
        builder.addInstallScript(writer.toString());
        // System.out.println(builder.buildManifest());
        return builder.build();
    }

    protected Map<String, String> getUserProperties() {
        return new HashMap<String, String>();
    }

    public boolean install(LocalPackage pkg) throws Exception {
        Map<String, String> props = getUserProperties();
        Task task = pkg.getInstallTask();
        ValidationStatus status = task.validate();
        if (!validateInstall(task, status)) {
            return false;
        }
        try {
            task.run(props);
            installDone(task, null);
            return true;
        } catch (Throwable t) {
            task.rollback();
            installDone(task, t);
            return false;
        }
    }

    public boolean uninstall(LocalPackage pkg) throws Exception {
        Map<String, String> props = getUserProperties();
        Task task = pkg.getUninstallTask();
        ValidationStatus status = task.validate();
        if (!validateUninstall(task, status)) {
            return false;
        }
        try {
            task.run(props);
            uninstallDone(task, null);
            return true;
        } catch (Throwable t) {
            task.rollback();
            uninstallDone(task, t);
            return false;
        }
    }

    @Test
    public void testInstallThenUninstall() throws Exception {
        File zip = createPackage();
        zip.deleteOnExit();
        LocalPackage pkg = service.addPackage(zip);
        if (install(pkg)) {
            // check package installed
            assertEquals(PackageState.STARTED, pkg.getState());
            if (uninstall(pkg)) {
                // check package uninstalled
                assertEquals(PackageState.DOWNLOADED, pkg.getState());
            }
        }
    }

}
