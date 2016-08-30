/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Stephane Lacoin
 */
package org.nuxeo.runtime.test.runner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.internal.AssumptionViolatedException;
import org.junit.runners.model.TestClass;

import com.google.inject.Binder;
import com.google.inject.Module;

class FeaturesLoader {

    protected enum Direction {
        FORWARD, BACKWARD
    }

    protected interface Callable {
        void call(Holder holder) throws Exception;
    }

    private final FeaturesRunner runner;

    /**
     * @param featuresRunner
     */
    FeaturesLoader(FeaturesRunner featuresRunner) {
        runner = featuresRunner;
    }

    protected class Holder {
        protected final Class<? extends RunnerFeature> type;

        protected final TestClass testClass;

        protected RunnerFeature feature;

        Holder(Class<? extends RunnerFeature> aType) throws InstantiationException, IllegalAccessException {
            type = aType;
            testClass = new TestClass(aType);
            feature = aType.newInstance();
        }

        @Override
        public String toString() {
            return "Holder [type=" + type + "]";
        }

    }

    protected final Map<Class<? extends RunnerFeature>, Holder> index = new HashMap<>();

    protected final List<Holder> holders = new LinkedList<>();

    Iterable<Holder> holders() {
        return holders;
    }

    Iterable<RunnerFeature> features() {
        return new Iterable<RunnerFeature>() {

            @Override
            public Iterator<RunnerFeature> iterator() {
                return new Iterator<RunnerFeature>() {

                    Iterator<Holder> iterator = holders.iterator();

                    @Override
                    public boolean hasNext() {
                        return iterator.hasNext();
                    }

                    @Override
                    public RunnerFeature next() {
                        return iterator.next().feature;
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }

                };
            }

        };
    }

    protected void apply(Direction direction, Callable callable) {
        apply(direction == Direction.FORWARD ? holders : reversed(holders), callable);
    }

    protected <T> List<T> reversed(List<T> list) {
        List<T> reversed = new ArrayList<>(list);
        Collections.reverse(reversed);
        return reversed;
    }

    protected void apply(Iterable<Holder> holders, Callable callable) {
        AssertionError errors = new AssertionError("invoke on features error " + holders);
        for (Holder each : holders) {
            try {
                callable.call(each);
            } catch (AssumptionViolatedException cause) {
                throw cause;
            } catch (Exception cause) {
                errors.addSuppressed(cause);
            }
        }
        if (errors.getSuppressed().length > 0) {
            throw errors;
        }
    }

    protected boolean contains(Class<? extends RunnerFeature> aType) {
        return index.containsKey(aType);
    }

    public void loadFeatures(Class<?> classToRun) throws Exception {
        FeaturesRunner.scanner.scan(classToRun);
        // load required features from annotation
        List<Features> annos = FeaturesRunner.scanner.getAnnotations(classToRun, Features.class);
        if (annos != null) {
            for (Features anno : annos) {
                for (Class<? extends RunnerFeature> cl : anno.value()) {
                    loadFeature(new HashSet<Class<?>>(), cl);
                }
            }
        }
    }

    protected void loadFeature(HashSet<Class<?>> cycles, Class<? extends RunnerFeature> clazz) throws Exception {
        if (index.containsKey(clazz)) {
            return;
        }
        if (cycles.contains(clazz)) {
            throw new IllegalStateException("Cycle detected in features dependencies of " + clazz);
        }
        cycles.add(clazz);
        FeaturesRunner.scanner.scan(clazz);
        // load required features from annotation
        List<Features> annos = FeaturesRunner.scanner.getAnnotations(clazz, Features.class);
        if (annos != null) {
            for (Features anno : annos) {
                for (Class<? extends RunnerFeature> cl : anno.value()) {
                    loadFeature(cycles, cl);
                }
            }
        }
        final Holder actual = new Holder(clazz);
        holders.add(actual);
        index.put(clazz, actual);
    }

    public <T extends RunnerFeature> T getFeature(Class<T> aType) {
        if (!index.containsKey(aType)) {
            return null;
        }
        return aType.cast(index.get(aType).feature);
    }

    protected Module onModule() {
        return new Module() {

            @SuppressWarnings("unchecked")
            @Override
            public void configure(Binder aBinder) {
                for (Holder each : holders) {
                    each.feature.configure(runner, aBinder);
                    aBinder.bind((Class) each.feature.getClass()).toInstance(each.feature);
                    aBinder.requestInjection(each.feature);
                }
            }

        };
    }

}