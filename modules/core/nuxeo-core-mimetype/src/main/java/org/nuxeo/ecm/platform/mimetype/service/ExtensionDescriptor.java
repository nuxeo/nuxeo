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
 * $Id: MimetypeDescriptor.java 20310 2007-06-11 15:54:14Z lgodard $
 */
package org.nuxeo.ecm.platform.mimetype.service;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Filename extension definition.
 * <p>
 * Allow the mimetype service to guess which mimetype to use for each extension. Ambiguous extensions (such as xml) tell
 * the service that a binary sniffing operation is advised to guess the right mimetype.
 *
 * @author <a href="mailto:og@nuxeo.com">Olivier Grisel</a>
 */
@XObject("fileExtension")
public class ExtensionDescriptor {

    @XNode("@name")
    protected String name;

    @XNode("@mimetype")
    protected String mimetype;

    protected boolean ambiguous = false;

    public ExtensionDescriptor() {
    }

    public ExtensionDescriptor(String name) {
        this.name = name;
    }

    public String getMimetype() {
        return mimetype;
    }

    public void setMimetype(String mimetype) {
        this.mimetype = mimetype;
    }

    public boolean isAmbiguous() {
        return ambiguous;
    }

    @XNode("@ambiguous")
    public void setAmbiguous(boolean ambiguous) {
        this.ambiguous = ambiguous;
    }

    public String getName() {
        return name;
    }

}
