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
package org.nuxeo.ecm.core.redis.transientstore;

import org.nuxeo.ecm.core.redis.RedisFeature;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.SimpleFeature;
import org.nuxeo.transientstore.test.TransientStoreFeature;

@Features({ TransientStoreFeature.class, RedisFeature.class })
@RepositoryConfig(init = DefaultRepositoryInit.class)
@LocalDeploy({ "org.nuxeo.ecm.core.redis.tests:transientstore-contrib.xml" })
public class TransientStoreRedisFeature extends SimpleFeature {

}
