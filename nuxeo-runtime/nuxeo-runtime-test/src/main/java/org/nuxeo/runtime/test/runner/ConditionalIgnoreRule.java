/*
 * (C) Copyright 2014-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Stephane Lacoin, Julien Carsique
 *
 */
package org.nuxeo.runtime.test.runner;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.SystemUtils;
import org.junit.Rule;
import org.junit.rules.MethodRule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

public class ConditionalIgnoreRule implements TestRule, MethodRule {
    @Inject
    private RunNotifier runNotifier;

    @Inject
    private FeaturesRunner runner;

    public static class Feature implements RunnerFeature {
        protected static final ConditionalIgnoreRule rule = new ConditionalIgnoreRule();

        @Rule
        public MethodRule methodRule() {
            return rule;
        }

        @Rule
        public static TestRule testRule() {
            return rule;
        }

    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.METHOD })
    public @interface Ignore {
        Class<? extends Condition> condition();

        /**
         * Optional reason why the test is ignored, reported additionally to the condition class simple name.
         */
        String cause() default "";
    }

    public interface Condition {
        boolean shouldIgnore();
    }

    public static final class NXP10926H2Upgrade implements Condition {
        @Override
        public boolean shouldIgnore() {
            return false;
        }
    }

    public static final class IgnoreIsolated implements Condition {
        boolean isIsolated = "org.nuxeo.runtime.testsuite.IsolatedClassloader".equals(
                getClass().getClassLoader().getClass().getName());

        @Override
        public boolean shouldIgnore() {
            return isIsolated;
        }
    }

    public static final class IgnoreLongRunning implements Condition {
        @Override
        public boolean shouldIgnore() {
            return true;
        }
    }

    public static final class IgnoreWindows implements Condition {
        @Override
        public boolean shouldIgnore() {
            return SystemUtils.IS_OS_WINDOWS;
        }
    }

    @Override
    public Statement apply(Statement base, Description description) {
        Ignore ignore = runner.getConfig(Ignore.class);
        Class<? extends Condition> conditionType = ignore.condition();
        if (conditionType == null) {
            return base;
        }
        return new Statement() {

            @Override
            public void evaluate() throws Throwable {
                // as this is a TestRule / Rule (see Feature#testRule) runtime was already started
                // because we are here after BeforeClassStatement (runtime start) and before
                // the MethodRule / Rule execution which cause processing of method annotations, but here we just want
                // to check condition on the class which doesn't depend on method annotations
                if (newCondition(null, null, null, conditionType).shouldIgnore()) {
                    runNotifier.fireTestIgnored(description);
                } else {
                    base.evaluate();
                }
            }
        };
    }

    @Override
    public Statement apply(Statement base, FrameworkMethod frameworkMethod, Object target) {
        Ignore ignore = runner.getConfig(frameworkMethod, Ignore.class);
        Class<? extends Condition> conditionType = ignore.condition();
        if (conditionType == null) {
            return base;
        }
        Class<?> type = target.getClass();
        Method method = frameworkMethod.getMethod();
        Description description = Description.createTestDescription(type, method.getName(), method.getAnnotations());
        return new Statement() {

            @Override
            public void evaluate() throws Throwable {
                if (newCondition(type, method, target, conditionType).shouldIgnore()) {
                    runNotifier.fireTestIgnored(description);
                } else {
                    base.evaluate();
                }
            }
        };
    }

    protected Condition newCondition(Class<?> type, Method method, Object target,
            Class<? extends Condition> conditionType) throws Error {
        Condition condition;
        try {
            condition = conditionType.newInstance();
        } catch (InstantiationException | IllegalAccessException cause) {
            throw new Error("Cannot instantiate condition of type " + conditionType, cause);
        }
        injectCondition(type, method, target, condition);
        return condition;
    }

    protected void injectCondition(Class<?> type, Method method, Object target, Condition condition)
            throws SecurityException, Error {
        Error errors = new Error("Cannot inject condition parameters in " + condition.getClass());
        for (Field eachField : condition.getClass().getDeclaredFields()) {
            if (!eachField.isAnnotationPresent(Inject.class)) {
                continue;
            }
            Object eachValue = null;
            if (eachField.isAnnotationPresent(Named.class)) {
                String name = eachField.getAnnotation(Named.class).value();
                if ("type".equals(name)) {
                    eachValue = type;
                } else if ("target".equals(name)) {
                    eachValue = target;
                } else if ("method".equals(name)) {
                    eachValue = method;
                }
            } else {
                Class<?> eachType = eachField.getType();
                if (eachType.equals(Class.class)) {
                    eachValue = type;
                } else if (eachType.equals(Object.class)) {
                    eachValue = target;
                } else if (eachType.equals(Method.class)) {
                    eachValue = method;
                }
            }
            if (eachValue == null) {
                continue;
            }
            eachField.setAccessible(true);
            try {
                eachField.set(condition, eachValue);
            } catch (IllegalArgumentException | IllegalAccessException cause) {
                errors.addSuppressed(new Error("Cannot inject " + eachField.getName(), cause));
            }
        }
        if (errors.getSuppressed().length > 0) {
            throw errors;
        }
        runner.getInjector().injectMembers(condition);
    }

}
