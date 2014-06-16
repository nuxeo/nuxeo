package org.nuxeo.runtime.testsuite;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.LogFactory;
import org.junit.extensions.cpsuite.ClasspathSuite;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;
import org.junit.runners.model.RunnerScheduler;
import org.nuxeo.common.utils.URLStreamHandlerFactoryInstaller;
import org.nuxeo.runtime.test.runner.FeaturesRunner;


public class IsolatedClasspathSuite extends ClasspathSuite {

   private ExecutorService executor = Executors.newFixedThreadPool(6,new ThreadFactory() {
       Integer count = 0;

       @Override
       public Thread newThread(Runnable r) {
           count += 1;
           return new Thread(r,
                   getTestClass().getJavaClass().getSimpleName() + "-"
                           + count.toString());
       }
   });

   private class ConcurrentScheduler implements RunnerScheduler {

        @Override
        public void schedule(final Runnable childStatement) {
            childStatement.run();
        }

        @Override
        public void finished() {
            executor.shutdown();
            try {
                executor.awaitTermination(4, TimeUnit.MINUTES);
            } catch (InterruptedException cause) {
                LogFactory.getLog(FeaturesRunner.class).error(
                        "Interrupted shudown", cause);
                Thread.currentThread().interrupt();
            }
        }
    }

    public IsolatedClasspathSuite(Class<?> suiteClass, RunnerBuilder builder)
            throws InitializationError {
        super(suiteClass, new IsolatedRunnerBuilder(builder));
        setScheduler(new ConcurrentScheduler());
    }

    public static class IsolatedRunnerBuilder extends RunnerBuilder {

        public IsolatedRunnerBuilder(RunnerBuilder builder) {
            base = builder;
        }

        protected final RunnerBuilder base;

        @Override
        public Runner runnerForClass(Class<?> testClass) throws Throwable {
            IsolatedClassloader loader = new IsolatedClassloader("org.nuxeo", "com.nuxeo", "com.codahale.metrics", "org.mockito", "org.h2", "org.apache.xbean.naming");
            loader.exclude(URLStreamHandlerFactoryInstaller.class);
            loader.exclude(URLStreamHandlerFactoryInstaller.FactoryStack.class);
            Runner baseRunner = base.runnerForClass(testClass);
            Class<?> isolatedTestClass = loader.loadClass(testClass.getName());
            Class<? extends Runner> runnerClass = (Class<? extends Runner>) loader.loadClass(baseRunner.getClass().getName());
            Runner runner = runnerClass.getConstructor(Class.class).newInstance(
                    isolatedTestClass);
            return runner;
        }
    }


    @Override
    protected void runChild(final Runner runner, final RunNotifier notifier) {
        executor.submit(new Runnable() {

            @Override
            public void run() {
                BlockJUnit4ClassRunner junitRunner = (BlockJUnit4ClassRunner)runner;
                final Class<?> testClass = junitRunner.getTestClass().getJavaClass();
                Thread.currentThread().setContextClassLoader(testClass.getClassLoader());
                runner.run(notifier);
            }
        });
    }

}
