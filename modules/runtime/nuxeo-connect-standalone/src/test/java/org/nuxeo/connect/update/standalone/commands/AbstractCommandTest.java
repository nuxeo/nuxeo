/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     jcarsique
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.connect.update.standalone.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.nuxeo.common.Environment;
import org.nuxeo.connect.update.LocalPackage;
import org.nuxeo.connect.update.PackageState;
import org.nuxeo.connect.update.PackageType;
import org.nuxeo.connect.update.ValidationStatus;
import org.nuxeo.connect.update.standalone.PackageTestCase;
import org.nuxeo.connect.update.task.Task;
import org.nuxeo.connect.update.util.PackageBuilder;
import org.nuxeo.connect.update.xml.XmlWriter;
import org.nuxeo.runtime.test.runner.ConditionalIgnoreRule;

/**
 * A base test case for testing command execution.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public abstract class AbstractCommandTest extends PackageTestCase {

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        // be sure these directories exists
        Environment.getDefault().getConfig().mkdirs();
        new File(Environment.getDefault().getHome(), "bundles").mkdirs();
        new File(Environment.getDefault().getHome(), "lib").mkdirs();
    }

    /**
     * Override this method to add to the package any resources required y the command execution.
     * <p>
     * Override if createPackage is not overridden.
     */
    protected void updatePackage(PackageBuilder builder) throws Exception {
    }

    /**
     * Override this method to write the command XML definition to test.
     * <p>
     * Override if createPackage is not overridden.
     */
    protected void writeCommand(XmlWriter writer) {
    }

    /**
     * Override this method to check the install outcome. If the <code>error</code> argument is not null then a rollback
     * was done. In that case you must check the rollback outcome.
     *
     * @param task the executed task.
     * @param error always null if task successfully executed. Not null if a rollback occurred.
     */
    protected void installDone(Task task, Throwable error) throws Exception {
        if (error != null) {
            log.error("Unexpected Rollback on Install Task", error);
            fail("Unexpected Rollback on Install Task");
        }
    }

    /**
     * Override this method to check the uninstall outcome. If the <code>error</code> argument is not null then a
     * rollback was done. In that case you must check the rollback outcome.
     *
     * @param task the executed task.
     * @param error always null if task successfully executed. Not null if a rollback occurred.
     */
    protected void uninstallDone(Task task, Throwable error) throws Exception {
        if (error != null) {
            log.error("Unexpected Rollback on uninstall Task", error);
            fail("Unexpected Rollback on uninstall Task");
        }
    }

    /**
     * Override this method if the install execution is expected to not be validated - in that case check the execution
     * status for consistency. The default implementation expects valid task and will fails if any errors are found in
     * the validation status.
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
     * Override this method if the uninstall execution is expected to not be validated - in that case check the
     * execution status for consistency. The default implementation expects valid task and will fails if any errors are
     * found in the validation status.
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
        builder.name("nuxeo-automation").version("5.3.2").type(PackageType.ADDON);
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
        return new HashMap<>();
    }

    public boolean install(LocalPackage pkg) throws Exception {
        Map<String, String> props = getUserProperties();
        Task task = pkg.getInstallTask();
        ValidationStatus status = task.validate();
        if (!validateInstall(task, status)) {
            return false;
        }

        Throwable error = null;
        try {
            task.run(props);
        } catch (Throwable t) {
            error = t;
            task.rollback();
        }
        installDone(task, error);
        return error == null;
    }

    public boolean uninstall(LocalPackage pkg) throws Exception {
        Map<String, String> props = getUserProperties();
        Task task = pkg.getUninstallTask();
        ValidationStatus status = task.validate();
        if (!validateUninstall(task, status)) {
            return false;
        }

        Throwable error = null;
        try {
            task.run(props);
        } catch (Throwable t) {
            error = t;
            task.rollback();
        }
        uninstallDone(task, error);
        return error == null;
    }

    @Test
    @ConditionalIgnoreRule.Ignore(condition = ConditionalIgnoreRule.IgnoreWindows.class, cause = "NXP-9086")
    public void testInstallThenUninstall() throws Exception {
        File zip = createPackage();
        LocalPackage pkg = service.addPackage(zip);
        zip.delete();
        if (install(pkg)) {
            // check package installed
            assertEquals(PackageState.STARTED, pkg.getPackageState());
            if (uninstall(pkg)) {
                // check package uninstalled
                assertEquals(PackageState.DOWNLOADED, pkg.getPackageState());
            }
        }
    }

}
