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
 *     Laurent Doguin
 */
package org.nuxeo.ecm.core.versioning;

import java.io.Serializable;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Descriptor to contribute the initial version state of a document.
 *
 * @author Laurent Doguin
 * @since 5.4.2
 */
@XObject("initialState")
public class InitialStateDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    @XNode("@minor")
    protected int minor = 0;

    @XNode("@major")
    protected int major = 0;

    public int getMinor() {
        return minor;
    }

    public int getMajor() {
        return major;
    }

}
