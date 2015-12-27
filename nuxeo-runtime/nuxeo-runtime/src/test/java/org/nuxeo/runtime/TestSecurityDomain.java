/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.runtime;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.login.AppConfigurationEntry;

import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.runtime.api.login.SecurityDomain;

public class TestSecurityDomain {

    @Test
    public void testSerialization() throws IOException, ClassNotFoundException {
        AppConfigurationEntry.LoginModuleControlFlag flag1 = AppConfigurationEntry.LoginModuleControlFlag.REQUISITE;
        Map<String, Object> opts1 = new HashMap<String, Object>();
        opts1.put("key1", "value1");
        opts1.put("key2", "value2");

        String name1 = "myLoginModule";
        AppConfigurationEntry entry1 = new AppConfigurationEntry(name1, flag1, opts1);

        String name2 = "myLoginModule";
        AppConfigurationEntry.LoginModuleControlFlag flag2 = AppConfigurationEntry.LoginModuleControlFlag.REQUISITE;
        Map<String, Object> opts2 = new HashMap<String, Object>();
        AppConfigurationEntry entry2 = new AppConfigurationEntry(name2, flag2, opts2);

        String securityDomainName = "nuxeo-test";
        SecurityDomain sd = new SecurityDomain(securityDomainName, new AppConfigurationEntry[] { entry1, entry2 });

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(sd);

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        sd = (SecurityDomain) ois.readObject();

        assertEquals(securityDomainName, sd.getName());
        AppConfigurationEntry[] entries = sd.getAppConfigurationEntries();
        assertEquals(2, entries.length);
        assertEquals(entry1.getLoginModuleName(), entries[0].getLoginModuleName());
        assertEquals(entry1.getControlFlag(), entries[0].getControlFlag());
        assertEquals(entry1.getOptions(), entries[0].getOptions());
        assertEquals(entry2.getLoginModuleName(), entries[1].getLoginModuleName());
        assertEquals(entry2.getControlFlag(), entries[1].getControlFlag());
        assertEquals(entry2.getOptions(), entries[1].getOptions());
    }

}
