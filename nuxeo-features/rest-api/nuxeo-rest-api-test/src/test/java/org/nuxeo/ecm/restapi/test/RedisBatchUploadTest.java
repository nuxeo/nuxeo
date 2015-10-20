package org.nuxeo.ecm.restapi.test;

/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */

import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;
import org.nuxeo.ecm.core.redis.transientstore.TransientStoreRedisFeature;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.ecm.restapi.server.jaxrs.BatchUploadObject;
import org.nuxeo.runtime.test.runner.ContributableFeaturesRunner;
import org.nuxeo.runtime.test.runner.Features;

/**
 * Tests the {@link BatchUploadObject} endpoints against a Redis implementation of the {@link TransientStore}.
 * 
 * @since 7.10
 */
@RunWith(ContributableFeaturesRunner.class)
@Features(TransientStoreRedisFeature.class)
@SuiteClasses(BatchUploadFixture.class)
public class RedisBatchUploadTest extends BaseTest {

}
