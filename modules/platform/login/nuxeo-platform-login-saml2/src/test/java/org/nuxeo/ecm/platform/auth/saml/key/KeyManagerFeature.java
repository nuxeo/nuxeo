/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.ecm.platform.auth.saml.key;

import java.security.cert.Certificate;

import org.junit.ClassRule;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.TemporaryKeyStore;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

/**
 * @since 2023.0
 */
@Deploy("org.nuxeo.ecm.platform.login.saml2.test")
public class KeyManagerFeature implements RunnerFeature {

    public static final String KEY_STORE_TYPE = "JKS";

    public static final String KEY_STORE_PASSWORD = "password";

    public static final String KEY_STORE_ENTRY_SAML_KEY = "saml";

    public static final String KEY_STORE_ENTRY_SAML_PASSWORD = "password";

    @ClassRule
    public static final TemporaryKeyStore TEMPORARY_KEY_STORE = new TemporaryKeyStore.Builder(KEY_STORE_TYPE,
            KEY_STORE_PASSWORD).generateKeyPair(KEY_STORE_ENTRY_SAML_KEY, KEY_STORE_ENTRY_SAML_PASSWORD).build();

    @Override
    public void start(FeaturesRunner runner) throws Exception {
        // now deploy KeyManager contribution
        RuntimeHarness harness = runner.getFeature(RuntimeFeature.class).getHarness();
        Framework.getProperties()
                 .setProperty("nuxeo.test.keyManager.store.path", TEMPORARY_KEY_STORE.getPath().toString());
        harness.deployContrib("org.nuxeo.ecm.platform.login.saml2.test", "OSGI-INF/key-manager-contrib.xml");
    }

    public Certificate getCertificate() {
        return TEMPORARY_KEY_STORE.getKeyPair(KEY_STORE_ENTRY_SAML_KEY).certificate();
    }
}
