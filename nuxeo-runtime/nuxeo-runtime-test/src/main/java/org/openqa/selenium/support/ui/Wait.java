/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 */

package org.openqa.selenium.support.ui;

import com.google.common.base.Function;

/**
 * A generic interface for waiting until a condition is true or not null. The condition may take a single argument of
 * type .
 *
 * @param <F> the argument to pass to any function called
 */
public interface Wait<F> {

    /**
     * Implementations should wait until the condition evaluates to a value that is neither null nor false. Because of
     * this contract, the return type must not be Void.
     * <p>
     * If the condition does not become true within a certain time (as defined by the implementing class), this method
     * will throw a non-specified {@link Throwable}. This is so that an implementor may throw whatever is idiomatic for
     * a given test infrastructure (e.g. JUnit4 would throw {@link AssertionError}.
     *
     * @param <T> the return type of the method, which must not be Void
     * @param isTrue the parameter to pass to the {@link ExpectedCondition}
     */
    <T> T until(Function<F, T> isTrue);
}
