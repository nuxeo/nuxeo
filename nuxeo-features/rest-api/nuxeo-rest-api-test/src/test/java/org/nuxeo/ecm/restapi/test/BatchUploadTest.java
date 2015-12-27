/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     dmetzler
 *     ataillefer
 */
package org.nuxeo.ecm.restapi.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.ecm.restapi.server.jaxrs.BatchUploadObject;
import org.nuxeo.runtime.test.runner.ContributableFeaturesRunner;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.transientstore.test.InMemoryTransientStoreFeature;

/**
 * Tests the {@link BatchUploadObject} endpoints against an in-memory implementation of the {@link TransientStore}.
 *
 * @since 5.8
 */
@RunWith(ContributableFeaturesRunner.class)
@Features(InMemoryTransientStoreFeature.class)
@SuiteClasses(BatchUploadFixture.class)
public class BatchUploadTest extends BaseTest {

}
