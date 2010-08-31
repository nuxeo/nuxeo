/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     ws@nuxeo.com
 */

package key;

import static org.junit.Assert.*;
import java.security.KeyPair;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.BackendType;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @author <a href="mailto:ws@nuxeo.com">WS</a>
 *
 */
//TODO this test will probably be moved to the core project
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(type = BackendType.H2, user = "Administrator")
@Deploy( { "org.nuxeo.ecm.core.api" })
public class KeyServiceTest {

    protected static KeyService service;

    protected static Session session;

    @Before
    public void setUp() throws Exception {
        service = new KeyServiceImpl();
    }

    @Test
    public void testCreateKeys() throws Exception {
        CertInfo certInfo = new CertInfo();
        certInfo.setKeyAlgorithm("RSA");
        certInfo.setNumBits(1024);
        certInfo.setSecurityProviderName("BC");
        KeyPair keyPair = service.createKeys(certInfo);
        assertNotNull(keyPair.getPrivate());
    }

}
