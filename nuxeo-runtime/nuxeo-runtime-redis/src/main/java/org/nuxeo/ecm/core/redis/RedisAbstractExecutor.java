/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.redis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.exceptions.JedisException;

/**
 * Abstract implementation of a {@link RedisExecutor}.
 * <p>
 * This base implementation collects the loaded scripts to be able to re-load them if the server has been restarted and
 * has lost them.
 *
 * @since 8.10
 */
public abstract class RedisAbstractExecutor implements RedisExecutor {

    private static final String ERROR_PREFIX_NOSCRIPT = "NOSCRIPT";

    protected Map<String, String> scripts = new HashMap<>();

    @Override
    public String scriptLoad(String script) throws JedisException {
        String sha1 = execute(jedis -> jedis.scriptLoad(script));
        scripts.put(sha1, script);
        return sha1;
    }

    @Override
    public Object evalsha(String sha1, List<String> keys, List<String> args) throws JedisException {
        try {
            return execute(jedis -> jedis.evalsha(sha1, keys, args));
        } catch (JedisDataException e) {
            if (!e.getMessage().startsWith(ERROR_PREFIX_NOSCRIPT)) {
                throw e;
            }
            // re-load the script
            String script = scripts.get(new String(sha1));
            if (script == null) {
                throw e;
            }
            execute(jedis -> jedis.scriptLoad(script));
            // retry once
            return execute(jedis -> jedis.evalsha(sha1, keys, args));
        }
    }

    @Override
    public Object evalsha(byte[] sha1, List<byte[]> keys, List<byte[]> args) throws JedisException {
        try {
            return execute(jedis -> jedis.evalsha(sha1, keys, args));
        } catch (JedisDataException e) {
            if (!e.getMessage().startsWith(ERROR_PREFIX_NOSCRIPT)) {
                throw e;
            }
            // re-load the script
            String script = scripts.get(new String(sha1));
            if (script == null) {
                throw e;
            }
            execute(jedis -> jedis.scriptLoad(script));
            // retry once
            return execute(jedis -> jedis.evalsha(sha1, keys, args));
        }
    }

}
