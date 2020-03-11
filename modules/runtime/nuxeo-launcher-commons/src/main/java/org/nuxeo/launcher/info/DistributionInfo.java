/*
 * (C) Copyright 2012-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     mguillaume
 */

package org.nuxeo.launcher.info;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
        try (InputStream in = new FileInputStream(distFile)) {
            distProps.load(in);
        }
        name = distProps.getProperty(Environment.DISTRIBUTION_NAME, "unknown");
        server = distProps.getProperty(Environment.DISTRIBUTION_SERVER, "unknown");
        version = distProps.getProperty(Environment.DISTRIBUTION_VERSION, "unknown");
        date = distProps.getProperty(Environment.DISTRIBUTION_DATE, "unknown");
        packaging = distProps.getProperty(Environment.DISTRIBUTION_PACKAGE, "unknown");
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
