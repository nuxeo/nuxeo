/*
 * (C) Copyright 2012-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     jcarsique
 */
package org.nuxeo.runtime.test.runner;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.ParentRunner;
import org.junit.runners.Suite.SuiteClasses;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;
import org.junit.runners.model.TestClass;

/**
 * JUnit4 ParentRunner that knows how to run a test class on multiple backend types.
 * <p>
 * To use it :
 *
 * <pre>
 * &#064;RunWith(ParameterizedSuite.class)
 * &#064;SuiteClasses(SimpleSession.class)
 * &#064;ParameterizedFeature(? extends RunnerFeature.class)
 * public class NuxeoSuiteTest {
 *     &#064;Parameters
 *   public static Collection&lt;Object[]&gt; yourParametersMethod() {...}
 * }
 * </pre>
 *
 * &#064;ParameterizedFeature is optional. If used, the corresponding class must implement a method annotated with
 * &#064;ParameterizedMethod
 */
public class ParameterizedSuite extends ParentRunner<FeaturesRunner> {

    /**
     * The <code>ParameterizedFeature</code> annotation specifies the class to be parameterized. That class must
     * implement {@link RunnerFeature}.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Inherited
    public @interface ParameterizedFeature {
        /**
         * @return the class to be parameterized
         */
        Class<?> value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface ParameterizedMethod {
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> getParametersList(TestClass klass) throws Throwable {
        return (List<Object[]>) getParametersMethod(klass).invokeExplosively(null);
    }

    @SuppressWarnings("unchecked")
    private Class<? extends RunnerFeature> getParameterizedClass(Class<?> klass) {
        ParameterizedFeature annotation = klass.getAnnotation(ParameterizedFeature.class);
        return (Class<? extends RunnerFeature>) (annotation != null ? annotation.value() : null);
    }

    private FrameworkMethod getParametersMethod(TestClass testClass) throws Exception {
        List<FrameworkMethod> methods = testClass.getAnnotatedMethods(Parameters.class);
        for (FrameworkMethod each : methods) {
            int modifiers = each.getMethod().getModifiers();
            if (Modifier.isStatic(modifiers) && Modifier.isPublic(modifiers))
                return each;
        }
        throw new Exception("Missing public static Parameters method on class " + testClass.getName());
    }

    private final List<FeaturesRunner> runners = new ArrayList<FeaturesRunner>();

    private final Map<FeaturesRunner, Object[]> runnersParams = new HashMap<FeaturesRunner, Object[]>();

    private List<Object[]> parametersList;

    private Class<? extends RunnerFeature> parameterizedClass;

    public ParameterizedSuite(Class<?> testClass, RunnerBuilder builder) throws InitializationError {
        this(builder, testClass, getSuiteClasses(testClass));
    }

    public ParameterizedSuite(RunnerBuilder builder, Class<?> testClass, Class<?>[] classes) throws InitializationError {
        super(testClass);
        try {
            this.parametersList = getParametersList(getTestClass());
            this.parameterizedClass = getParameterizedClass(testClass);
        } catch (Throwable e) {
            throw new InitializationError(e);
        }
        for (Object[] params : parametersList) {
            List<Runner> runners2 = builder.runners(testClass, classes);
            for (Runner runner : runners2) {
                if (!(runner instanceof FeaturesRunner)) {
                    continue;
                }
                FeaturesRunner featureRunner = (FeaturesRunner) runner;
                this.runners.add(featureRunner);
                try {
                    if (parameterizedClass != null) {
                        runnersParams.put(featureRunner, params);
                        RunnerFeature feature = featureRunner.getFeature(parameterizedClass);
                        for (Method method : feature.getClass().getMethods()) {
                            if (method.getAnnotation(ParameterizedMethod.class) != null) {
                                method.invoke(feature, new Object[] { params });
                            }
                        }
                    }
                } catch (Throwable e) {
                    throw new InitializationError(e);
                }
            }
        }
    }

    protected static Class<?>[] getSuiteClasses(Class<?> klass) throws InitializationError {
        SuiteClasses annotation = klass.getAnnotation(SuiteClasses.class);
        if (annotation == null) {
            throw new InitializationError(String.format("class '%s' must have a SuiteClasses annotation",
                    klass.getName()));
        }
        return annotation.value();
    }

    @Override
    protected Description describeChild(FeaturesRunner child) {
        Description description = child.getDescription();
        return Description.createTestDescription(description.getTestClass(), description.getDisplayName() + " "
                + Arrays.toString(runnersParams.get(child)), description.getAnnotations().toArray(new Annotation[0]));
    }

    @Override
    protected List<FeaturesRunner> getChildren() {
        return runners;
    }

    @Override
    protected void runChild(FeaturesRunner child, RunNotifier notifier) {
        // for (Object[] params : parametersList) {
        System.out.println(String.format("\r\n============= RUNNING %s =================", describeChild(child)));
        // try {
        // if (parameterizedClass != null) {
        // RunnerFeature feature = child.getFeature(parameterizedClass);
        // for (Method method : feature.getClass().getMethods()) {
        // if (method.getAnnotation(ParameterizedMethod.class) != null) {
        // method.invoke(feature, new Object[] { params });
        // }
        // }
        // }
        child.run(notifier);
        // } catch (Throwable e) {
        // notifier.fireTestFailure(new Failure(child.getDescription(), e));
        // }
        // }
    }
}
