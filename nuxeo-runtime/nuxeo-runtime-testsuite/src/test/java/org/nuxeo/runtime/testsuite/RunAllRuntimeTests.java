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
package org.nuxeo.runtime.testsuite;
import org.junit.After;
import org.junit.extensions.cpsuite.ClasspathSuite.ClassnameFilters;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.tracelog.internal.TraceEnabler;


@RunWith(IsolatedClasspathSuite.class)
@ClassnameFilters("org\\.nuxeo\\..*\\.Test.*")
public class RunAllRuntimeTests {

    TraceEnabler traceEnabler = new TraceEnabler();

    public void enableTrace() {
        traceEnabler.enable(true);
    }

    @After
    public void disableTrace() {
        traceEnabler.disable();
    }
}
