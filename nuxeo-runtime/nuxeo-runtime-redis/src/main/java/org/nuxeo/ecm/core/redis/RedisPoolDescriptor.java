/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.core.redis;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

import redis.clients.jedis.Protocol;

@XObject("pool")
public abstract class RedisPoolDescriptor {

    @XNode("disabled")
    protected boolean disabled = false;

    public String password;

    @XNode("password")
    public void setPassword(String value) {
        password = StringUtils.defaultIfBlank(value, null);
    }

    @XNode("database")
    public int database = Protocol.DEFAULT_DATABASE;

    @XNode("timeout")
    public int timeout = Protocol.DEFAULT_TIMEOUT;

    @XNode("maxTotal")
    public int maxTotal = 16;

    @XNode("maxIdle")
    public int maxIdle = 8;

    @XNode("prefix")
    public String prefix;

    protected abstract RedisExecutor newExecutor();
}
