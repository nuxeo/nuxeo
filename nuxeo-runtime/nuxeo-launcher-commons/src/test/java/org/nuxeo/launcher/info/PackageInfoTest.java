/*
 * (C) Copyright 2012-2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Julien Carsique
 *
 */

package org.nuxeo.launcher.info;

import static org.junit.Assert.assertEquals;

import java.io.StringWriter;
import java.io.Writer;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.junit.Before;
import org.junit.Test;

import org.nuxeo.connect.connector.fake.FakeDownloadablePackage;
import org.nuxeo.connect.update.Package;
import org.nuxeo.connect.update.PackageState;
import org.nuxeo.connect.update.PackageVisibility;
import org.nuxeo.connect.update.Version;

//import org.json.XML;

/**
 * @since 5.7
 */
public class PackageInfoTest {

    static final Log log = LogFactory.getLog(PackageInfoTest.class);

    private PackageInfo packageInfo1;

    @Before
    public void setup() {
        Package pkg = new FakeDownloadablePackage("test", new Version("1.0.0"));
        packageInfo1 = new PackageInfo(pkg);
    }

    @Test
    public void testMarshalling() throws JAXBException, JSONException {
        JAXBContext jc = JAXBContext.newInstance(PackageInfo.class);
        Writer xml = new StringWriter();
        Marshaller marshaller = jc.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.marshal(packageInfo1, xml);
        log.debug(xml.toString());
        JSONObject entity = XML.toJSONObject(xml.toString()).getJSONObject(
                "package");
        assertEquals(PackageVisibility.UNKNOWN,
                PackageVisibility.valueOf(entity.getString("visibility")));
        assertEquals(false, entity.getBoolean("supported"));
        assertEquals("test", entity.getString("name"));
        assertEquals(PackageState.UNKNOWN,
                PackageState.getByLabel(entity.getString("state")));
        assertEquals(false, entity.getBoolean("supportsHotReload"));
        assertEquals(new Version("1.0.0"),
                new Version(entity.getString("version")));
    }
}
