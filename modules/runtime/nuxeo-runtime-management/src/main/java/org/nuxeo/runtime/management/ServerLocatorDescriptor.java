/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     matic
 */
package org.nuxeo.runtime.management;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.common.xmap.registry.XRegistryId;

/**
 * @author matic
 */
@XObject("locator")
@XRegistry(compatWarnOnMerge = true)
public class ServerLocatorDescriptor {

    @XNode(value = "@domain", defaultAssignment = "")
    @XRegistryId
    protected String domainName;

    @XNode(value = "@default", defaultAssignment = "true")
    protected boolean isDefault;

    @XNode(value = "@exist", defaultAssignment = "true")
    protected boolean isExisting;

    @XNode(value = "@rmiPort", defaultAssignment = "1099")
    protected int rmiPort;

    @XNode(value = "@remote", defaultAssignment = "true")
    protected boolean remote;

}
