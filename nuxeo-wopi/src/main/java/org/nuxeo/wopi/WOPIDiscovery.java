/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.wopi;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.nuxeo.ecm.core.api.NuxeoException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * Class used to parse a WOPI discovery XML file.
 *
 * @since 10.3
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class WOPIDiscovery {

    @JacksonXmlProperty(localName = "net-zone")
    private NetZone netZone;

    public NetZone getNetZone() {
        return netZone;
    }

    public void setNetZone(NetZone netZone) {
        this.netZone = netZone;
    }

    protected static final XmlMapper XML_MAPPER = new XmlMapper();

    public static WOPIDiscovery read(File discoveryFile) {
        try {
            return XML_MAPPER.readValue(discoveryFile, WOPIDiscovery.class);
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NetZone {

        @JacksonXmlProperty(localName = "app")
        @JacksonXmlElementWrapper(useWrapping = false)
        private List<App> apps;

        public List<App> getApps() {
            return apps;
        }

        public void setApps(List<App> apps) {
            this.apps = apps;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class App {

        private String name;

        @JacksonXmlProperty(localName = "action")
        @JacksonXmlElementWrapper(useWrapping = false)
        private List<Action> actions;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<Action> getActions() {
            return actions;
        }

        public void setActions(List<Action> actions) {
            this.actions = actions;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Action {
        private String name;

        private String ext;

        @JacksonXmlProperty(localName = "urlsrc")
        private String url;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getExt() {
            return ext;
        }

        public void setExt(String ext) {
            this.ext = ext;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

    }
}
