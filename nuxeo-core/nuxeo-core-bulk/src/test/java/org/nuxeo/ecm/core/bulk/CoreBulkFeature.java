/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *       Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.core.bulk;

import java.time.Duration;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.nuxeo.ecm.core.io.CoreIOFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.login.LoginAs;
import org.nuxeo.runtime.stream.RuntimeStreamFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * Intermediate feature for nuxeo-core-bulk module.
 *
 * @since 10.2
 */
@Deploy("org.nuxeo.runtime.kv")
@Deploy("org.nuxeo.ecm.core.bulk")
@Deploy("org.nuxeo.ecm.core.bulk.test")
@Features({ RuntimeFeature.class, TransactionalFeature.class, RuntimeStreamFeature.class, CoreIOFeature.class })
public class CoreBulkFeature implements RunnerFeature {

    public static class DummyLogin implements LoginAs {
        @Override
        public LoginContext loginAs(String username) throws LoginException {
            return Framework.login();
        }
    }

    @Override
    public void initialize(FeaturesRunner runner) {
        runner.getFeature(TransactionalFeature.class)
              .addWaiter(deadline -> Framework.getService(BulkService.class).await(Duration.ofMinutes(1)));
    }

}
