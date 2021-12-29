/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Thomas Fowley <thomas.fowley@hyland.com>
 */
package org.nuxeo.ecm.core.redis;

import org.nuxeo.common.xmap.XMap;
import org.nuxeo.launcher.config.ConfigurationException;
import org.nuxeo.launcher.config.ConfigurationGenerator;
import org.nuxeo.launcher.config.backingservices.BackingChecker;
import redis.clients.jedis.Jedis;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @since 2021.10
 */
public class RedisChecker implements BackingChecker {

    private static final String TEMPLATE_NAME = "redis";

    private static final String CONFIG_NAME = "redis-config.xml";

    @Override
    public boolean accepts(ConfigurationGenerator cg) {
        return cg.getTemplateList().contains(TEMPLATE_NAME);
    }

    @Override
    public void check(ConfigurationGenerator cg) throws ConfigurationException {
        RedisPoolDescriptor config = getConfig(cg);
        RedisExecutor executor = config.newExecutor();
        try {
            String pong = executor.execute(Jedis::ping);
            if (!"PONG".equals(pong)) {
                throw new RuntimeException("Unable to ping Redis, received " + pong); // NOSONAR
            }
        } catch (RuntimeException e) {
            throw new ConfigurationException("Unable to reach Redis on prefix: " + config.prefix, e);
        } finally {
            executor.getPool().close();
        }
    }

    protected RedisPoolDescriptor getConfig(ConfigurationGenerator cg) throws ConfigurationException {
        File configFile = new File(cg.getConfigDir(), CONFIG_NAME);
        if (!configFile.exists()) {
            throw new ConfigurationException("Cannot find Redis configuration: " + CONFIG_NAME);
        }
        XMap xmap = new XMap();
        xmap.register(RedisPoolDescriptor.class);
        xmap.register(RedisServerDescriptor.class);
        xmap.register(RedisSentinelDescriptor.class);
        try (InputStream inStream = new FileInputStream(configFile)) {
            Object[] nodes = xmap.loadAll(inStream);
            for (Object node : nodes) {
                if (node != null) {
                    return (RedisPoolDescriptor) node;
                }
            }
            throw new ConfigurationException("No RedisPoolDescriptor found in " + configFile.getAbsolutePath());
        } catch (IOException e) {
            throw new ConfigurationException(
                    "Failed to load RedisPoolDescriptor from " + configFile.getAbsolutePath(), e);
        }
    }
}
