/*******************************************************************************
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.nuxeo.ecm.core.redis;

import org.junit.runners.model.FrameworkMethod;
import org.nuxeo.ecm.core.redis.RedisFeature.Config;
import org.nuxeo.ecm.core.redis.RedisFeature.Mode;
import org.nuxeo.ecm.core.redis.embedded.RedisEmbeddedGuessConnectionError;
import org.nuxeo.ecm.core.redis.embedded.RedisEmbeddedPool;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.SimpleFeature;

@Features(RedisFeature.class)
@Config(mode = Mode.embedded, guessError=RedisEmbeddedGuessConnectionError.OnRandomCall.class)
public class RedisFailoverFeature extends SimpleFeature {

    RedisEmbeddedPool pool;

    @Override
    public void start(FeaturesRunner runner) throws Exception {
        pool = (RedisEmbeddedPool) Framework.getLocalService(
                RedisExecutor.class).getPool();
    }

    @Override
    public void beforeMethodRun(FeaturesRunner runner, FrameworkMethod method,
            Object test) throws Exception {
        Config config = runner.getConfig(method, Config.class);
        if (pool instanceof RedisEmbeddedPool) {
            pool.setError(config.guessError().newInstance());
        }
    }

    @Override
    public void afterTeardown(FeaturesRunner runner) throws Exception {
        pool.setError(new RedisEmbeddedGuessConnectionError.NoError());
    }
}
