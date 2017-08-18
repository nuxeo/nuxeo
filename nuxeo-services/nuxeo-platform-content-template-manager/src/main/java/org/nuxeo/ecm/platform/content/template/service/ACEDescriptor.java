/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.platform.content.template.service;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * ACE Descriptor. Immutable.
 */
@XObject(value = "ace")
public class ACEDescriptor {

    @XNode("@granted")
    private boolean granted = true;

    @XNode("@principal")
    private String principal;

    @XNode("@permission")
    private String permission;

    public ACEDescriptor() {
        // default constructor
    }

    public ACEDescriptor(ACEDescriptor toCopy) {
        this.granted = toCopy.granted;
        this.principal = toCopy.principal;
        this.permission = toCopy.permission;
    }

    public boolean getGranted() {
        return granted;
    }

    public String getPermission() {
        return permission;
    }

    public String getPrincipal() {
        return principal;
    }

}
