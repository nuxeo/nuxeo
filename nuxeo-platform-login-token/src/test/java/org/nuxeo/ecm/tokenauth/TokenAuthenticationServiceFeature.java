/*
 * (C) Copyright 2006-20012 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.tokenauth;

import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.ecm.tokenauth.service.TokenAuthenticationService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.SimpleFeature;

/**
 * Feature to run tests needing the {@link TokenAuthenticationService}.
 *
 * @author Antoine Taillefer (ataillefer@nuxeo.com)
 * @since 5.7
 */
@Features(PlatformFeature.class)
@Deploy({
        "org.nuxeo.ecm.platform.login.token:OSGI-INF/token-authentication-directory-types.xml",
        "org.nuxeo.ecm.platform.login.token:OSGI-INF/token-authentication-framework.xml",
        "org.nuxeo.ecm.platform.login.token.test:OSGI-INF/test-token-authentication-directory-contrib.xml" })
public class TokenAuthenticationServiceFeature extends SimpleFeature {

}
