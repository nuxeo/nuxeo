/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Stephane Lacoin at Nuxeo (aka matic)
 */
package org.nuxeo.ecm.core.management.jtajca;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.Executors;

import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.RunWith;
import org.junit.runners.model.Statement;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Insure we can detect a tx leak at tear down
 *
 * @since 8.4
 */
public class CanDetectTransactionLeakTest {
    protected static class IgnoreInner implements TestRule {
        static boolean isRunningInners;

        @Override
        public Statement apply(Statement base, Description description) {
            Assume.assumeTrue(isRunningInners);
            return base;
        }
    }

    @RunWith(FeaturesRunner.class)
    @Features({ JtajcaManagementFeature.class, CoreFeature.class })
    public static class LeakingTxTest {
        @ClassRule
        public static final IgnoreInner ignoreInner = new IgnoreInner();

        @Test
        public void leakATransaction() {
            Executors.newSingleThreadExecutor().execute(() -> {
                TransactionHelper.requireNewTransaction();
            });
        }
    }

    @Test
    public void canDetectLeaks() {
        IgnoreInner.isRunningInners = true;
        try {
            Result result = JUnitCore.runClasses(LeakingTxTest.class);
            assertThat(result.getRunCount()).isEqualTo(1);
            assertThat(result.getFailureCount()).isEqualTo(1);
        } finally {
            IgnoreInner.isRunningInners = false;
        }
    }

}
