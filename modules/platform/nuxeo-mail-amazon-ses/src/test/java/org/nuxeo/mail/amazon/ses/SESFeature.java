/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.mail.amazon.ses;

import static com.amazonaws.SDKGlobalConfiguration.ACCESS_KEY_ENV_VAR;
import static com.amazonaws.SDKGlobalConfiguration.ALTERNATE_SECRET_KEY_ENV_VAR;
import static com.amazonaws.SDKGlobalConfiguration.AWS_REGION_ENV_VAR;
import static org.apache.commons.lang3.StringUtils.isNoneBlank;
import static org.junit.Assume.assumeTrue;
import static org.nuxeo.mail.MailConstants.CONFIGURATION_MAIL_FROM;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

/**
 * @since 2023.4
 */
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.runtime.aws")
@Deploy("org.nuxeo.mail")
@Deploy("org.nuxeo.mail.amazon.ses")
public class SESFeature implements RunnerFeature {

    protected static final String AWS_SES_MAIL_SENDER_ENV_VAR = "AWS_SES_MAIL_SENDER";

    @Override
    public void start(FeaturesRunner runner) throws Exception {
        String verifiedSender = System.getenv(AWS_SES_MAIL_SENDER_ENV_VAR);
        assumeTrue("AWS credentials, region and a verified mail are missing in test configuration",
                isNoneBlank(verifiedSender, System.getenv(ACCESS_KEY_ENV_VAR),
                        System.getenv(ALTERNATE_SECRET_KEY_ENV_VAR), System.getenv(AWS_REGION_ENV_VAR)));

        Framework.getProperties().setProperty(CONFIGURATION_MAIL_FROM, verifiedSender);
        RuntimeHarness harness = runner.getFeature(RuntimeFeature.class).getHarness();
        harness.deployContrib("org.nuxeo.mail.amazon.ses.test", "OSGI-INF/test-ses-override-default-sender.xml");
    }

}
