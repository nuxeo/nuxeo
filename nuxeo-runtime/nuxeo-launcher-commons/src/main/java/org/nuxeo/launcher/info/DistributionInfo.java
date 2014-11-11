/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     mguillaume
 */

package org.nuxeo.launcher.info;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.nuxeo.common.Environment;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "distribution")
public class DistributionInfo {

    public DistributionInfo() {
    }

    public DistributionInfo(File distFile) throws IOException {
        Properties distProps = new Properties();
        distProps.load(new FileInputStream(distFile));
        name = distProps.getProperty(Environment.DISTRIBUTION_NAME, "unknown");
        server = distProps.getProperty("org.nuxeo.distribution.server",
                "unknown");
        version = distProps.getProperty(Environment.DISTRIBUTION_VERSION,
                "unknown");
        date = distProps.getProperty("org.nuxeo.distribution.date", "unknown");
        packaging = distProps.getProperty("org.nuxeo.distribution.package",
                "unknown");
    }

    @XmlElement()
    public String name = "unknown";

    @XmlElement()
    public String server = "unknown";

    @XmlElement()
    public String version = "unknown";

    @XmlElement()
    public String date = "unknown";

    @XmlElement()
    public String packaging = "unknown";

}
