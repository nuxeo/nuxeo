package org.nuxeo.maven.plugin;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

/**
 * Mojo to run Nuxeo.
 */
@Mojo(name = "run",
      requiresProject = true,
      defaultPhase = LifecyclePhase.VALIDATE,
      requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
@Execute(phase = LifecyclePhase.TEST_COMPILE)
public class RunMojo extends AbstractMojo {

    /**
     * The Maven project.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;


    @Parameter(defaultValue = "${project.build.outputDirectory}", required = true)
    private File classesDirectory;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        logLogo();

        IsolatedThreadGroup threadGroup = new IsolatedThreadGroup("nuxeo-runner");
        Thread runnerThread = new Thread(threadGroup, new NuxeoRunner(), "nuxeo-runner");
        runnerThread.setContextClassLoader(new URLClassLoader(getClassPathUrls()));
        runnerThread.start();
        join(threadGroup);
        threadGroup.rethrowUncaughtException();
    }

    protected URL[] getClassPathUrls() throws MojoExecutionException {
        try {
            List<URL> urls = new ArrayList<>();
            addResources(urls);
            addProjectClasses(urls);
            addDependencies(urls);
            return urls.toArray(new URL[0]);
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to build classpath", e);
        }
    }

    protected void addResources(List<URL> urls) throws IOException {
//        if (this.addResources) {
            for (Resource resource : this.project.getResources()) {
                File directory = new File(resource.getDirectory());
                urls.add(directory.toURI().toURL());
//                FileUtils.removeDuplicatesFromOutputDirectory(this.classesDirectory,
//                        directory);
            }
//        }
    }
    private void addProjectClasses(List<URL> urls) throws MalformedURLException {
        urls.add(this.classesDirectory.toURI().toURL());
    }

    protected void addDependencies(List<URL> urls) throws MalformedURLException {
        for (Artifact artifact : project.getArtifacts()) {
            if (artifact.getFile() != null) {
                urls.add(artifact.getFile().toURI().toURL());
            }
        }
    }

    protected void join(ThreadGroup threadGroup) {
        boolean hasNonDaemonThreads;
        do {
            hasNonDaemonThreads = false;
            Thread[] threads = new Thread[threadGroup.activeCount()];
            threadGroup.enumerate(threads);
            for (Thread thread : threads) {
                if (thread != null && !thread.isDaemon()) {
                    try {
                        hasNonDaemonThreads = true;
                        thread.join();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        } while (hasNonDaemonThreads);
    }

    private void logLogo() {
        getLog().info("dxxxxxxxxxxc    oxxo       lxxx lkkl       ;kkk");
        getLog().info("dxxxxxxxxxxxd;  oxxo       lxxx lkkkx:.  ,dkkkx");
        getLog().info("dxxc       lxxo oxxo       lxxx   okkkkokkkkd, ");
        getLog().info("dxxc       lxxo oxxo       lxxx    .dkkkkkk.   ");
        getLog().info("dxxc       lxxo oxxo       lxxx   ,dkkkkkkkk,  ");
        getLog().info("dxxc       lxxo oxxcccccccdxxx   ,kkkkx okkkk, ");
        getLog().info("loo;       :ooc   cooooooooool  xkko       ckko");
        getLog().info("");
        getLog().info(":cc,       ;cc;                 oxxxxxxxxxxxxxo");
        getLog().info("dxxc       lxxo                 oxxxxxxxxxxxxxo");
        getLog().info("dxxc       lxxo                 oxxo");
        getLog().info("dxxc       lxxo                 oxxxxxxxxxxxxxo");
        getLog().info("dxxc       lxxo                 oxxo");
        getLog().info("cxxoooooooxxxo                  oxxxxxxxxxxxxxo");
        getLog().info("  xoooooooxxxo                  oxxxxxxxxxxxxxo");
        getLog().info("");
        getLog().info("lkkl       ;kkk oxxxxxxxxxxxxxo xooooooooooo,  ");
        getLog().info("lkkkx:.  ,dkkkx oxxxxxxxxxxxxxo lxxxxxxxxxxxxb;");
        getLog().info(" okkkkokkkkd,   oxxo            lxxd       :xxx");
        getLog().info("   .dkkkkkk.    oxxxxxxxxxxxxxo lxxd       :xxx");
        getLog().info("  ,dkkkkkkkk,   oxxo            cxxd       :xxx");
        getLog().info(" ,kkkkx okkkk,  oxxxxxxxxxxxxxo   oxxxxxxxxxxxx");
        getLog().info("xkko       ckko oxxxxxxxxxxxxxo    :xxxxxxxxxxx");
    }

    /**
     * Isolated {@link ThreadGroup} to capture uncaught exceptions.
     */
    class IsolatedThreadGroup extends ThreadGroup {

        private final Object monitor = new Object();

        private Throwable exception;

        public IsolatedThreadGroup(String name) {
            super(name);
        }

        @Override
        public void uncaughtException(Thread thread, Throwable ex) {
            if (!(ex instanceof ThreadDeath)) {
                synchronized (this.monitor) {
                    this.exception = this.exception != null ? this.exception : ex;
                }
                getLog().warn("An error occured in main Thread", ex);
            }
        }

        public void rethrowUncaughtException() throws MojoExecutionException {
            synchronized (this.monitor) {
                if (this.exception != null) {
                    throw new MojoExecutionException("An exception occurred while running", this.exception);
                }
            }
        }

    }
}
