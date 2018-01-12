/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.lib.stream.tools;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.nuxeo.lib.stream.log.kafka.KafkaUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Parse an xml file describing Kafka configurations, the format is the one used by KafkaConfigDescriptor. We can not
 * use the Nuxeo descriptor directly because we are in a library without Nuxeo dependency.
 *
 * @since 9.10
 */
public class KafkaConfigParser {
    protected static final String DEFAULT_ZK_SERVERS = "DEFAULT_TEST";

    protected static final String DEFAULT_BOOTSTRAP_SERVERS = "DEFAULT_TEST";

    protected String zkServers;

    private Properties producerProperties;

    private Properties consumerProperties;

    private String prefix;

    public KafkaConfigParser(Path path, String configName) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(path.toFile());
            NodeList nodes = document.getElementsByTagName("kafkaConfig");
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                String name = node.getAttributes().getNamedItem("name").getNodeValue();
                if (configName.equals(name)) {
                    parseConfig(node);
                    return;
                }
            }
        } catch (SAXException | IOException | ParserConfigurationException e) {
            throw new IllegalArgumentException("Invalid Kafka config file: " + path, e);
        }
        throw new IllegalArgumentException(String.format("Config: %s not found in file: %s", configName, path));
    }

    protected void parseConfig(Node node) {
        prefix = node.getAttributes().getNamedItem("topicPrefix").getNodeValue();
        setZkServers(node.getAttributes().getNamedItem("zkServers").getNodeValue());
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if ("producer".equals(child.getNodeName())) {
                producerProperties = decodeProperties(child);
            } else if ("consumer".equals(child.getNodeName())) {
                consumerProperties = decodeProperties(child);
            }
        }
    }

    protected Properties decodeProperties(Node node) {
        NodeList children = node.getChildNodes();
        Properties ret = new Properties();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if ("property".equals(child.getNodeName())) {
                String name = child.getAttributes().getNamedItem("name").getNodeValue();
                String value = child.getTextContent();
                if (ProducerConfig.BOOTSTRAP_SERVERS_CONFIG.equals(name) && DEFAULT_BOOTSTRAP_SERVERS.equals(value)) {
                    ret.put(name, KafkaUtils.getBootstrapServers());
                } else {
                    ret.put(name, value);
                }
            }
        }
        return ret;
    }

    public String getZkServers() {
        return zkServers;
    }

    public Properties getProducerProperties() {
        return producerProperties;
    }

    public Properties getConsumerProperties() {
        return consumerProperties;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setZkServers(String zkServers) {
        if (DEFAULT_ZK_SERVERS.equals(zkServers)) {
            this.zkServers = KafkaUtils.getZkServers();
        } else {
            this.zkServers = zkServers;
        }
    }
}
