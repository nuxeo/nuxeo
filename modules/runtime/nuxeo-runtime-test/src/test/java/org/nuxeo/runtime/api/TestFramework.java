/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Contributors:
 *      Kevin Leturc <kleturc@nuxeo.com>
 */

package org.nuxeo.runtime.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.function.Supplier;

import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.function.ThrowableRunnable;
import org.nuxeo.common.function.ThrowableSupplier;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LogCaptureFeature;
import org.nuxeo.runtime.test.runner.LogCaptureFeature.FilterOn;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * @since 11.3
 */
@RunWith(FeaturesRunner.class)
@Features({ RuntimeFeature.class, LogCaptureFeature.class })
@FilterOn(loggerClass = TestFramework.class, logLevel = "INFO")
public class TestFramework {

    private static final Logger log = LogManager.getLogger(TestFramework.class);

    @Inject
    protected LogCaptureFeature.Result result;

    // NXP-29682
    @Test
    public void testDoPrivilegedRunnableAPI() {
        // keep declaration as it, we want to test API signature
        Runnable runnable = () -> log.info("Useful message");
        Framework.doPrivileged(runnable);
        List<String> messages = result.getCaughtEventMessages();
        assertEquals(1, messages.size());
        assertEquals("Useful message", messages.get(0));
    }

    // NXP-29682
    @Test
    public void testDoPrivilegedSupplierAPI() {
        // keep declaration as it, we want to test API signature
        Supplier<String> supplier = () -> "Useful return";
        String ret = Framework.doPrivileged(supplier);
        assertEquals("Useful return", ret);
    }

    // NXP-29682
    @Test
    public void testDoPrivilegedThrowingRunnableAPI() {
        try {
            // keep declaration as it, we want to test API signature
            ThrowableRunnable<Exception> runnable = () -> throwSomething("Useful message");
            Framework.doPrivilegedThrowing(runnable);
            fail("Should have failed");
        } catch (Exception e) {
            assertEquals("Useful message", e.getMessage());
        }
    }

    // NXP-29682
    @Test
    public void testDoPrivilegedThrowingSupplierAPI() {
        try {
            // keep declaration as it, we want to test API signature
            ThrowableSupplier<String, Exception> supplier = () -> throwSomething("Useful message");
            Framework.doPrivilegedThrowing(supplier);
            fail("Should have failed");
        } catch (Exception e) {
            assertEquals("Useful message", e.getMessage());
        }
    }

    protected String throwSomething(String message) throws Exception {
        throw new Exception(message);
    }
}
