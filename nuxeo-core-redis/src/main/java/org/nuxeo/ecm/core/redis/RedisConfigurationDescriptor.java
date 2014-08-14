/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.redis;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Descriptor for a Redis configuration.
 *
 * @since 5.8
 */
@XObject("redis")
public class RedisConfigurationDescriptor {

    @XNode("@disabled")
    public boolean disabled;

    @XNode("prefix")
    public String prefix;

    @XNode("host")
    public String host;

    @XNode("port")
    public int port;

    @XNode("password")
    public String password;

    @XNode("database")
    public int database;

    @XNode("timeout")
    public int timeout;

}
