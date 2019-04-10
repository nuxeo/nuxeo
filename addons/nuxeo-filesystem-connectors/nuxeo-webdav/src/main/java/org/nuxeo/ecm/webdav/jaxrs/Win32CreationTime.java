/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Vitalii Siryi (Gagnavarslan)
 */
package org.nuxeo.ecm.webdav.jaxrs;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

/**
 * Microsoft Extension property. http://msdn.microsoft.com/en-us/library/cc250142(PROT.10).aspx
 *
 * @author Organization: Gagnavarslan ehf
 */
@XmlRootElement(name = "Win32CreationTime")
public final class Win32CreationTime {

    @XmlValue
    private String value;

    public Win32CreationTime() {
    }

    public Win32CreationTime(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}
