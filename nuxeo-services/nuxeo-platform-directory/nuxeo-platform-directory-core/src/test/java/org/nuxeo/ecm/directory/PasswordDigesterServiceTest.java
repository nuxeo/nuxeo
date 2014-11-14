/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     dmetzler
 */
package org.nuxeo.ecm.directory;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.directory.digest.PasswordDigester;
import org.nuxeo.ecm.directory.digest.PasswordDigesterService;
import org.nuxeo.ecm.directory.digest.UnknownAlgorithmException;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

import com.google.inject.Inject;

/**
 *
 *
 * @since 7.1
 */
@RunWith(FeaturesRunner.class)
@Deploy({ "org.nuxeo.ecm.directory", "org.nuxeo.ecm.directory.api" })
@Features(RuntimeFeature.class)
@LocalDeploy("org.nuxeo.ecm.directory:remove-digester-contrib.xml")
public class PasswordDigesterServiceTest {

    @Inject
    PasswordDigesterService ds;

    @Test
    public void passwordDigesterServiceIsRegsitered() throws Exception {
        assertNotNull(ds);
    }

    @Test
    public void itGetTheSSHADigester() throws Exception {
        PasswordDigester digester = ds.getPasswordDigester("SSHA");
        assertNotNull(digester);
        assertTrue(digester.hashPassword("abcd").startsWith("{SSHA}"));
    }

    @Test(expected=UnknownAlgorithmException.class)
    public void itThrowsAnExceptionForAnUnknownAlgorithm() throws Exception {
        ds.getPasswordDigester("unknownAlgorithm");
    }


    @Test(expected=UnknownAlgorithmException.class)
    public void itIsPossibleToDisableAnAlgorithm() throws Exception {
        ds.getPasswordDigester("PBKDF2Hmac256");
    }

    @Test
    public void digesterMayVerifyAPassword() throws Exception {
        PasswordDigester digester = ds.getPasswordDigester("SSHA");
        assertTrue(digester.verifyPassword("abcd",
                "{SSHA}WPvqVeSt0Mr2llICYmAX9+pjtPH271eznDHvrw=="));
    }

}
