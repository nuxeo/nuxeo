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

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "configuration")
public class ConfigurationInfo {

    public ConfigurationInfo() {
    }

    @XmlElement(name = "dbtemplate")
    public String dbtemplate = "default";

    @XmlElementWrapper(name = "basetemplates")
    @XmlElement(name = "template")
    public List<String> basetemplates = new ArrayList<>();

    @XmlElementWrapper(name = "pkgtemplates")
    @XmlElement(name = "template")
    public List<String> pkgtemplates = new ArrayList<>();

    @XmlElementWrapper(name = "usertemplates")
    @XmlElement(name = "template")
    public List<String> usertemplates = new ArrayList<>();

    @XmlElementWrapper(name = "keyvals")
    @XmlElement(name = "keyval")
    public List<KeyValueInfo> keyvals = new ArrayList<>();

}
