package org.nuxeo.runtime.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.internal.AssumptionViolatedException;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

public class ConditionalIgnoreRule implements MethodRule {

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.METHOD })
    public @interface Ignore {
        Class<? extends Condition> condition() default IgnoreAlways.class;
    }

    public interface Condition {
        boolean shouldIgnore(Method method, Object target);
    }

    public static final class IgnoreAlways implements Condition {
        @Override
        public boolean shouldIgnore(Method method, Object target) {
            return true;
        }
    }

    public static final class NXP10926H2Upgrade implements Condition {

        @Override
        public boolean shouldIgnore(Method method, Object target) {
            return true;
        }

    }

    public static final class IgnoreIsolated implements Condition {
        boolean isIsolated = "org.nuxeo.runtime.testsuite.IsolatedClassloader".equals(getClass().getClassLoader().getClass().getName());

        @Override
        public boolean shouldIgnore(Method method, Object target) {
            return isIsolated;
        }
    }

    public static final class IgnoreLongRunning implements Condition {

        @Override
        public boolean shouldIgnore(Method method, Object target) {
            return true; // TODO add an annotation suitable for
        }

    }

    @Override
    public Statement apply(Statement base, FrameworkMethod method,
            Object fixtureTarget) {
        Class<?> fixtureType = fixtureTarget.getClass();
        Method fixtureMethod = method.getMethod();
        if (fixtureType.isAnnotationPresent(Ignore.class)) {
            checkIgnore(fixtureType.getAnnotation(Ignore.class),
                    fixtureMethod, fixtureTarget);
        }
        if (fixtureMethod.isAnnotationPresent(Ignore.class)) {
            checkIgnore(fixtureMethod.getAnnotation(Ignore.class),
                    fixtureMethod, fixtureTarget);
        }
        return base;
    }

    protected void checkIgnore(Ignore type, Method method,
            Object target) {
        Class<? extends Condition> conditionType = type.condition();
        Condition condition = newCondition(conditionType);
        injectCondition(method, target, conditionType, condition);
        if(condition.shouldIgnore(method, target)) {
            throw new AssumptionViolatedException(condition.getClass().getSimpleName());
        }
    }

    protected void injectCondition(Method method, Object target,
            Class<? extends Condition> conditionType, Condition condition)
            throws SecurityException, Error {
        for (Field eachField : conditionType.getDeclaredFields()) {
            if (!eachField.isAnnotationPresent(Inject.class)) {
                continue;
            }
            Object eachValue = null;
            if (!eachField.isAnnotationPresent(Named.class)) {
                String name = eachField.getAnnotation(Named.class).value();
                switch (name) {
                case "target":
                    eachValue = target;
                case "method":
                    eachValue = method;
                }
            } else {
                Class<?> eachType = eachField.getType();
                if (eachType.equals(Object.class)) {
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
                throw new Error("Cannot inject condition value in " + conditionType, cause);
            }
        }
    }

    protected Condition newCondition(Class<? extends Condition> conditionType)
            throws Error {
        Condition condition;
        try {
            condition = conditionType.newInstance();
        } catch (InstantiationException | IllegalAccessException cause) {
            throw new Error("Cannot instantiate condition of type "
                    + conditionType, cause);
        }
        return condition;
    }

}
