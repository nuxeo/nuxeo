/*
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
 *
 * Contributors:
 *     tiry
 */
package org.nuxeo.ecm.core.event.kafka.test;

import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.SimpleFeature;


/**
 * Test feature to deploy Nuxeo Event Bus with Kafka plug.
 * Because of startup ordering issue, we need to define a specific feature so that core-event and kafka are deploted in the right order !
 *
 * @since TODO
 */
@Features({ KafkaFeature.class, RuntimeFeature.class })
@Deploy("org.nuxeo.ecm.core.event")
public class KafkaEventBusFeature extends SimpleFeature {

}
