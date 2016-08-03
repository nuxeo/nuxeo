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
package org.nuxeo.ecm.core.event.kafka.listeners;

import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;
import org.nuxeo.ecm.core.event.kafka.KafkaEventBusFeature;
import org.nuxeo.ecm.core.event.test.PostCommitEventListenerTest;
import org.nuxeo.ecm.core.event.test.virusscan.TestDummyVirusScanner;
import org.nuxeo.ecm.core.uidgen.TestDocUIDGeneratorListener;
import org.nuxeo.ecm.core.version.test.TestVersioningRemovalPolicy;
import org.nuxeo.runtime.test.runner.ContributableFeaturesRunner;
import org.nuxeo.runtime.test.runner.Features;

@RunWith(ContributableFeaturesRunner.class)
@Features(KafkaEventBusFeature.class)
@SuiteClasses({ PostCommitEventListenerTest.class, TestDummyVirusScanner.class, TestDocUIDGeneratorListener.class,
        TestVersioningRemovalPolicy.class })
public class TestListenersWithKafkaPipe {

}
