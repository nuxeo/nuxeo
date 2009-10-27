/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
package org.nuxeo.build.ant;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.apache.tools.ant.BuildLogger;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.DemuxInputStream;
import org.apache.tools.ant.DemuxOutputStream;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.input.DefaultInputHandler;
import org.apache.tools.ant.input.InputHandler;
import org.nuxeo.build.maven.MavenClient;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class AntClient {

    public final static String MAVEN_CLIENT_REF = "maven.client.ref";

    protected ClassLoader loader;
    protected Project project;
    protected MavenClient maven;
    Map<String, String> globalProperties;

    public AntClient() {
        this (null);
    }

    public AntClient(ClassLoader loader) {
        if (loader == null) {
            loader = Thread.currentThread().getContextClassLoader();
            if (loader == null) {
                loader = AntClient.class.getClassLoader();
            }
        }
        this.loader = loader;
    }

    public void setGlobalProperties(Map<String, String> globalProperties) {
        this.globalProperties = globalProperties;
    }
    
    public void setMavenClient(MavenClient maven) {
        this.maven = maven;
    }

    public MavenClient getMavenClient() {
        return maven;
    }

    public Project getProject() {
        return project;
    }


    public void run(File buildFile) {
        run(buildFile, (List<String>)null);
    }

    public void run(File buildFile, List<String> targets) {
        run(buildFile.getParentFile(), buildFile, targets);
    }

    public void run(File cwd, URL buildFile) {
        run(cwd, buildFile, null);
    }

    public void run(URL buildFile) {
        run(new File("."), saveURL(buildFile), null);
    }

    public void run(URL buildFile, List<String> targets) {
        run(new File("."), saveURL(buildFile), targets);
    }

    public void run(File cwd, URL buildFile, List<String> targets) {
        run(cwd, saveURL(buildFile), targets);
    }


    public void run(File cwd, File buildFile, List<String> targets) {
        PrintStream err = System.err;
        PrintStream out = System.out;
        InputStream in = System.in;

        project = new Project();
        project.setCoreLoader(loader);

        InputHandler handler =  new DefaultInputHandler();
        project.setInputHandler(handler);
        configureIO(false);
        project.setKeepGoingMode(false);

        project.setBaseDir(cwd);
        project.setUserProperty("ant.file",
                buildFile.getPath());
        project.setUserProperty("ant.version", org.apache.tools.ant.Main.getAntVersion());

        if (globalProperties != null) {
            for (Map.Entry<String, String> entry : globalProperties.entrySet()) {
                project.setUserProperty(entry.getKey(), entry.getValue());
            }
        }
        
        // add maven reference
        if (maven != null) {
            project.addReference(MAVEN_CLIENT_REF, maven);
        }

        //TODO add user defined properties
        //project.setUserProperty(arg, value);

        // Add the default listener
        project.addBuildListener(createLogger());

        project.fireBuildStarted();

        try {
            project.init();
            ProjectHelper.configureProject(project, buildFile);

            if (targets != null) {
                project.getExecutor().executeTargets(project, targets.toArray(new String[targets.size()]));
            } else {
                project.getExecutor().executeTargets(project, new String[] {project.getDefaultTarget()});
            }

            project.fireBuildFinished(null);
        } catch (Throwable t) {
            project.fireBuildFinished(t);
            t.printStackTrace();
        } finally {
            System.setOut(out);
            System.setErr(err);
            System.setIn(in);
        }

    }

    protected void configureIO(boolean allowInput) {
        if (allowInput) {
            project.setDefaultInputStream(System.in);
        }
        System.setIn(new DemuxInputStream(project));
        System.setOut(new PrintStream(new DemuxOutputStream(project, false)));
        System.setErr(new PrintStream(new DemuxOutputStream(project, true)));
    }


    /** Stream to use for logging. */
    private static PrintStream out = System.out;

    /** Stream that we are using for logging error messages. */
    private static PrintStream err = System.err;

    private BuildLogger createLogger() {
        BuildLogger logger =  new DefaultLogger();
        logger.setMessageOutputLevel(Project.MSG_INFO);
        logger.setOutputPrintStream(out);
        logger.setErrorPrintStream(err);
        //logger.setEmacsMode(false);
        return logger;
    }


    public static File saveURL(URL url) {
        InputStream in = null;
        try {
            File file = File.createTempFile("ant_client_url_", ".tmp");
            file.deleteOnExit();
            in = url.openStream();
            copyToFile(in, file);
            return file;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (in != null) {
                try {in.close();} catch (IOException e) {}
            }
        }
    }


    public static void copyToFile(InputStream in, File file) throws IOException {
        OutputStream out = null;
        try {
            out = new FileOutputStream(file);
            byte[] buffer = createBuffer(in.available());
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    private static byte[] createBuffer(int preferredSize) {
        if (preferredSize < 1) {
            preferredSize = BUFFER_SIZE;
        }
        if (preferredSize > MAX_BUFFER_SIZE) {
            preferredSize = MAX_BUFFER_SIZE;
        } else if (preferredSize < MIN_BUFFER_SIZE) {
            preferredSize = MIN_BUFFER_SIZE;
        }
        return new byte[preferredSize];
    }

    private static final int BUFFER_SIZE = 1024 * 64; // 64K
    private static final int MAX_BUFFER_SIZE = 1024 * 1024; // 64K
    private static final int MIN_BUFFER_SIZE = 1024 * 8; // 64K
}
