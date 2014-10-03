/*******************************************************************************
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *******************************************************************************/
package org.nuxeo.runtime.test.runner;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.internal.AssumptionViolatedException;
import org.junit.rules.MethodRule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

public class ConditionalIgnoreRule implements MethodRule, TestRule {

    public static class Feature extends SimpleFeature {
        protected static final ConditionalIgnoreRule rule = new ConditionalIgnoreRule();

        @ClassRule
        public static TestRule classRule() {
            return rule;
        }

        @Rule
        public MethodRule methodRule() {
            return rule;
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.METHOD })
    public @interface Ignore {
        Class<? extends Condition> condition();
    }

    public interface Condition {
        boolean shouldIgnore();
    }

    public static final class NXP10926H2Upgrade implements Condition {

        @Override
        public boolean shouldIgnore() {
            return true;
        }

    }

    public static final class IgnoreIsolated implements Condition {
        boolean isIsolated = "org.nuxeo.runtime.testsuite.IsolatedClassloader"
            .equals(getClass().getClassLoader().getClass().getName());

        @Override
        public boolean shouldIgnore() {
            return isIsolated;
        }
    }

    public static final class IgnoreLongRunning implements Condition {

        @Override
        public boolean shouldIgnore() {
            return true; // TODO add an annotation suitable for
        }

    }

    @Override
    public Statement apply(Statement base, FrameworkMethod method,
            Object fixtureTarget) {
        Class<?> fixtureType = fixtureTarget.getClass();
        Method fixtureMethod = method.getMethod();
        if (fixtureType.isAnnotationPresent(Ignore.class)) {
            check(fixtureType.getAnnotation(Ignore.class), fixtureType,
                    fixtureMethod, fixtureTarget);
        }
        if (fixtureMethod.isAnnotationPresent(Ignore.class)) {
            check(fixtureMethod.getAnnotation(Ignore.class), fixtureType,
                    fixtureMethod, fixtureTarget);
        }
        return base;
    }

    @Override
    public Statement apply(Statement base, Description description) {
        Class<?> fixtureType = description.getTestClass();
        if (fixtureType.isAnnotationPresent(Ignore.class)) {
            check(fixtureType.getAnnotation(Ignore.class), fixtureType);
        }
        return base;
    }

    protected void check(Ignore ignore, Class<?> type) {
        check(ignore, type, null, null);
    }

    protected void check(Ignore ignore, Class<?> type, Method method,
            Object target) {
        Class<? extends Condition> conditionType = ignore.condition();
        if (conditionType == null) {
            return;
        }
        Condition condition = newCondition(type, method, target, conditionType);
        if (condition.shouldIgnore()) {
            throw new AssumptionViolatedException(condition.getClass()
                .getSimpleName());
        }
    }

    protected Condition newCondition(Class<?> type, Method method,
            Object target, Class<? extends Condition> conditionType)
            throws Error {
        Condition condition;
        try {
            condition = conditionType.newInstance();
        } catch (InstantiationException | IllegalAccessException cause) {
            throw new Error("Cannot instantiate condition of type "
                    + conditionType, cause);
        }
        injectCondition(type, method, target, condition);
        return condition;
    }

    protected void injectCondition(Class<?> type, Method method, Object target,
            Condition condition) throws SecurityException, Error {
        Error errors = new Error("Cannot inject condition parameters in "
                + condition.getClass());
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
                errors.addSuppressed(new Error("Cannot inject "
                        + eachField.getName()));
            }
            eachField.setAccessible(true);
            try {
                eachField.set(condition, eachValue);
            } catch (IllegalArgumentException | IllegalAccessException cause) {
                errors.addSuppressed(new Error("Cannot inject "
                        + eachField.getName(), cause));
            }
        }
        if (errors.getSuppressed().length > 0) {
            throw errors;
        }
    }



}
