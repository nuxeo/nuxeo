/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 */
package org.nuxeo.ecm.core.test;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertNotNull;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.lib.stream.log.kafka.KafkaUtils;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.osgi.framework.Bundle;

/**
 * Description of the specific capabilities of a WorkManager for tests, and helper methods.
 *
 * @since 10.3
 */
public class WorkManagerConfiguration {

    private static final Log log = LogFactory.getLog(WorkManagerConfiguration.class);

    public static final String WORK_MANAGER_PROPERTY = "nuxeo.test.workmanager.implementation";

    public static final String WORK_MANAGER_DEFAULT = "default";

    public static final String WORK_MANAGER_CHRONICLE_STREAM = "chronicle-stream";

    public static final String WORK_MANAGER_KAFKA_STREAM = "kafka-stream";

    private static final String STREAM_WORK_MANAGER_STORESTATE_ENABLED_PROPERTY = "nuxeo.test.stream.workmanager.storestate.enabled";

    private static final String STREAM_WORK_MANAGER_STORESTATE_ENABLED_DEFAULT = "false";

    private String workManagerType;

    private boolean streamWorkManagerStoreStateEnabled;

    final CoreFeature feature;

    public WorkManagerConfiguration(CoreFeature feature) {
        this.feature = feature;
    }

    protected void init() {
        workManagerType = calcWorkManagerType();
        streamWorkManagerStoreStateEnabled = Boolean.parseBoolean(defaultProperty(
                STREAM_WORK_MANAGER_STORESTATE_ENABLED_PROPERTY, STREAM_WORK_MANAGER_STORESTATE_ENABLED_DEFAULT));
        if (WORK_MANAGER_KAFKA_STREAM.equals(workManagerType)) {
            initKafka();
        }
    }

    protected String calcWorkManagerType() {
        final String value = System.getProperty(WORK_MANAGER_PROPERTY);
        if (StringUtils.isNotEmpty(value)) {
            if (asList(WORK_MANAGER_DEFAULT, WORK_MANAGER_CHRONICLE_STREAM, WORK_MANAGER_KAFKA_STREAM).contains(
                    value)) {
                throw new NuxeoException(String.format("Optional system property '%s' has illegal value '%s'",
                        WORK_MANAGER_PROPERTY, value));
            }
            return value;
        }
        ;
        String workManagerType;
        if (isKafkaEnabled()) {
            workManagerType = WORK_MANAGER_KAFKA_STREAM;
        } else {
            workManagerType = WORK_MANAGER_DEFAULT;
        }
        log.debug(String.format(
                "Calculated workManagerType value to be '%s' since System Property '%s' was not specified",
                workManagerType, WORK_MANAGER_PROPERTY));
        return workManagerType;
    }

    protected boolean isKafkaEnabled() {
        return "true".equals(System.getProperty("kafka")) && KafkaUtils.kafkaDetected();
    }

    protected static String defaultProperty(String name, String def) {
        String value = System.getProperty(name);
        if (value == null || value.equals("") || value.equals("${" + name + "}")) {
            value = def;
        }
        Framework.getProperties().setProperty(name, value);
        return value;
    }

    public void initKafka() {
        KafkaUtils kafkaUtils = new KafkaUtils();
        kafkaUtils.listTopics().forEach(kafkaUtils::markTopicForDeletion);
    }

    public List<URL> getDeploymentContribURLs(FeaturesRunner runner) {
        List<URL> contribURLs;
        String[] contribPaths;
        String bundleName = "org.nuxeo.ecm.core.test";
        String msg = "Deploying a %s WorkManager";
        switch (workManagerType) {
        case WORK_MANAGER_DEFAULT:
            contribPaths = new String[] { "OSGI-INF/test-default-work-queuing-contrib.xml" };
            contribURLs = getDeploymentContribURLs(runner, bundleName, contribPaths);
            break;
        case WORK_MANAGER_CHRONICLE_STREAM:
            log.info(String.format(msg, "Chronicle Stream"));
            contribPaths = new String[] { "OSGI-INF/test-chronicle-stream-config-contrib.xml",
                    "OSGI-INF/test-stream-workmanager-contrib.xml" };
            contribURLs = getDeploymentContribURLs(runner, bundleName, contribPaths);
            break;
        case WORK_MANAGER_KAFKA_STREAM:
            log.info(String.format(msg, "Kafka Stream"));
            contribPaths = new String[] { "OSGI-INF/test-kafka-stream-config-contrib.xml",
                    "OSGI-INF/test-stream-workmanager-contrib.xml" };
            contribURLs = getDeploymentContribURLs(runner, bundleName, contribPaths);
            break;
        default:
            throw new NuxeoException(
                    String.format("WorkManagerConfiguration.workManagerType '%s' is invalid, check system property %s",
                            workManagerType, WORK_MANAGER_PROPERTY));
        }
        return contribURLs;
    }

    private List<URL> getDeploymentContribURLs(FeaturesRunner runner, String bundleName, String[] contribPaths) {
        List<URL> contribURLs = new ArrayList<>(contribPaths.length);
        for (String cPath : contribPaths) {
            URL deploymentContribURL = getDeploymentContribURL(runner, bundleName, cPath);
            contribURLs.add(deploymentContribURL);
        }
        return contribURLs;
    }

    private URL getDeploymentContribURL(FeaturesRunner runner, String bundleName, String contribPath) {
        Bundle bundle = runner.getFeature(RuntimeFeature.class).getHarness().getOSGiAdapter().getRegistry().getBundle(
                bundleName);
        URL contribURL = bundle.getEntry(contribPath);
        assertNotNull("deployment contrib " + contribPath + " not found", contribURL);
        return contribURL;
    }

    public String getWorkManagerType() {
        return workManagerType;
    }

}
