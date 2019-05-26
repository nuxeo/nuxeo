/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.platform.ui.web.restAPI.service;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.restlet.Restlet;

/**
 * Descriptor for a Restlet
 *
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 */
@XObject(value = "restletPlugin")
public class RestletPluginDescriptor {

    @XNode("@name")
    private String name;

    @XNode("@enabled")
    private Boolean enabled = Boolean.TRUE;

    @XNodeList(value = "urlPatterns/urlPattern", type = ArrayList.class, componentType = String.class)
    private List<String> urlPatterns = new ArrayList<>();

    @XNode("@useSeam")
    private boolean useSeam;

    @XNode("@useConversation")
    private boolean useConversation;

    @XNode("@class")
    private Class<Restlet> className;

    @XNode("@isSingleton")
    private boolean isSingleton = Boolean.FALSE;

    public Class<Restlet> getClassName() {
        return className;
    }

    public void setClassName(Class<Restlet> className) {
        this.className = className;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getUrlPatterns() {
        return urlPatterns;
    }

    public void setUrlPatterns(List<String> urlPatterns) {
        this.urlPatterns = urlPatterns;
    }

    public boolean getUseSeam() {
        return useSeam;
    }

    public void setUseSeam(boolean useSeam) {
        this.useSeam = useSeam;
    }

    public boolean getUseConversation() {
        return useConversation;
    }

    public void setUseConversation(boolean useConversation) {
        this.useConversation = useConversation;
    }

    public boolean isSingleton() {
        return isSingleton;
    }

    public void setIsSingleton(boolean isSingleton) {
        this.isSingleton = isSingleton;
    }

}
