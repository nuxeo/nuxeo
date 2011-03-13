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
package org.nuxeo.connect.update.impl.task.ant;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.input.DefaultInputHandler;
import org.apache.tools.ant.input.InputHandler;
import org.nuxeo.connect.update.LocalPackage;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class AntRunner {

    protected final ClassLoader loader;

    protected Project project;

    Map<String, String> globalProperties;

    public AntRunner() {
        this(null);
    }

    public AntRunner(ClassLoader loader) {
        if (loader == null) {
            loader = Thread.currentThread().getContextClassLoader();
            if (loader == null) {
                loader = getClass().getClassLoader();
            }
        }
        this.loader = loader;
    }

    public void setGlobalProperties(Map<String, String> globalProperties) {
        this.globalProperties = globalProperties;
    }

    public Project getProject() {
        return project;
    }

    public void run(LocalPackage pkg, File buildFile) throws BuildException {
        run(pkg, buildFile, (List<String>) null);
    }

    public void run(LocalPackage pkg, File buildFile, List<String> targets)
            throws BuildException {

        project = new Project();
        project.setCoreLoader(loader);

        InputHandler handler = new DefaultInputHandler();
        project.setInputHandler(handler);
        project.setKeepGoingMode(false);

        project.setBaseDir(pkg.getData().getRoot());
        project.setUserProperty("ant.file", buildFile.getPath());
        project.setUserProperty("ant.version",
                org.apache.tools.ant.Main.getAntVersion());

        if (globalProperties != null) {
            for (Map.Entry<String, String> entry : globalProperties.entrySet()) {
                project.setUserProperty(entry.getKey(), entry.getValue());
            }
        }

        // TODO add package install context reference?
        // project.addReference("package_install_ctx", ctx);

        // TODO add user defined properties
        // project.setUserProperty(arg, value);

        // Add the default listener
        // project.addBuildListener(createLogger());

        project.fireBuildStarted();

        try {
            project.init();
            ProjectHelper.configureProject(project, buildFile);

            if (targets != null) {
                project.getExecutor().executeTargets(project,
                        targets.toArray(new String[targets.size()]));
            } else {
                project.getExecutor().executeTargets(project,
                        new String[]{project.getDefaultTarget()});
            }

            project.fireBuildFinished(null);
        } catch (BuildException e) {
            project.fireBuildFinished(e);
            throw e;
        }
    }

}
