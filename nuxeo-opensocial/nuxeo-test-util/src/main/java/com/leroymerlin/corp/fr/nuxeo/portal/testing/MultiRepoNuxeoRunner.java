package com.leroymerlin.corp.fr.nuxeo.portal.testing;

import java.util.ArrayList;
import java.util.List;

import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.Suite.SuiteClasses;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;

import com.leroymerlin.corp.fr.nuxeo.portal.testing.annotation.Repos;

@Repos
public class MultiRepoNuxeoRunner extends ParentRunner<NuxeoRunner> {

    private List<NuxeoRunner> fRunners = new ArrayList<NuxeoRunner>();
    private RepoType[] repos;

    public MultiRepoNuxeoRunner(Class<?> testClass, RunnerBuilder builder)
            throws InitializationError {
        this(builder, testClass, getAnnotatedClasses(testClass), getRepoTypes(testClass));
    }

    private static RepoType[] getRepoTypes(Class<?> testClass) {
        Repos annotation = testClass.getAnnotation(Repos.class);
        if(annotation == null) {
            return MultiRepoNuxeoRunner.class.getAnnotation(Repos.class).value();
        } else {
            return annotation.value();
        }
    }

    public MultiRepoNuxeoRunner(RunnerBuilder builder, Class<?> testClass,
            Class<?>[] classes, RepoType[] repoTypes) throws InitializationError {
        this(null, builder.runners(null, classes), repoTypes);
    }

    protected MultiRepoNuxeoRunner(Class<?> klass, List<Runner> runners, RepoType[] repoTypes) throws InitializationError {
        super(klass);
        for(Runner nuxeoRunner : runners) {
            fRunners.add((NuxeoRunner) nuxeoRunner);
        }
        repos = repoTypes;
    }

    private static Class<?>[] getAnnotatedClasses(Class<?> klass) throws InitializationError {
        SuiteClasses annotation= klass.getAnnotation(SuiteClasses.class);
        if (annotation == null)
            throw new InitializationError(String.format("class '%s' must have a SuiteClasses annotation", klass.getName()));
        for(Class<?> testClass : annotation.value()) {
            if(!testClass.getAnnotation(RunWith.class).value().isAssignableFrom(NuxeoRunner.class)) {
                throw new InitializationError(String.format("class '%s' must have a SuiteClasses annotation", klass.getName()));
            }
        }
        return annotation.value();
    }

    @Override
    protected Description describeChild(NuxeoRunner child) {
        return child.getDescription();
    }

    @Override
    protected List<NuxeoRunner> getChildren() {
        return fRunners;
    }

    @Override
    protected void runChild(NuxeoRunner child, RunNotifier notifier) {
        for(RepoType type : repos) {

            child.setRepoType(type);
            child.resetInjector();
            child.run(notifier);
        }
    }

}
