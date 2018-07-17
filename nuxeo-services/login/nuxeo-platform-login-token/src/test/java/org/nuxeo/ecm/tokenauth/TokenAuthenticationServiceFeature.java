/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.tokenauth;

import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.ecm.tokenauth.service.TokenAuthenticationService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.RunnerFeature;

/**
 * Feature to run tests needing the {@link TokenAuthenticationService}.
 *
 * @author Antoine Taillefer (ataillefer@nuxeo.com)
 * @since 5.7
 */
@Features(PlatformFeature.class)
@Deploy("org.nuxeo.ecm.platform.web.common:OSGI-INF/authentication-contrib.xml")
@Deploy("org.nuxeo.ecm.platform.oauth")
@Deploy("org.nuxeo.ecm.automation.server:OSGI-INF/auth-contrib.xml")
@Deploy("org.nuxeo.ecm.platform.login.token:OSGI-INF/token-authentication-contrib.xml")
@Deploy("org.nuxeo.ecm.platform.login.token:OSGI-INF/token-authentication-directory-types.xml")
@Deploy("org.nuxeo.ecm.platform.login.token:OSGI-INF/token-authentication-framework.xml")
@Deploy("org.nuxeo.ecm.platform.login.token.test:OSGI-INF/test-token-authentication-directory-contrib.xml")
@Deploy("org.nuxeo.ecm.platform.login.token.test:OSGI-INF/test-token-authentication-system-login-contrib.xml")
public class TokenAuthenticationServiceFeature implements RunnerFeature {

}
