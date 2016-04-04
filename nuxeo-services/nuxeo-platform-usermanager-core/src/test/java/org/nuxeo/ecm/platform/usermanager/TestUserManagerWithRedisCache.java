/*******************************************************************************
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *******************************************************************************/
package org.nuxeo.ecm.platform.usermanager;

import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;
import org.nuxeo.ecm.core.cache.CacheFeature;
import org.nuxeo.ecm.core.redis.RedisFeature;
import org.nuxeo.runtime.test.runner.ContributableFeaturesRunner;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.SimpleFeature;

@RunWith(ContributableFeaturesRunner.class)
@Features(TestUserManagerWithRedisCache.Feature.class)
@SuiteClasses(TestUserManager.class)
public class TestUserManagerWithRedisCache {

    @Features({ RedisFeature.class, CacheFeature.class })
    @LocalDeploy("org.nuxeo.ecm.platform.usermanager:test-usermanager-redis-cache.xml")
    public static class Feature extends SimpleFeature{

    }

}
