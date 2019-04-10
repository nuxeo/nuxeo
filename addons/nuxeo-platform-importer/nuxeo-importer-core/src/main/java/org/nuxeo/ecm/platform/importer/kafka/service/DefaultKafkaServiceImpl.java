/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * Contributors:
 *     anechaev
 */
package org.nuxeo.ecm.platform.importer.kafka.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.protocol.ApiKeys;
import org.apache.kafka.common.requests.CreateTopicsRequest;
import org.apache.kafka.common.requests.CreateTopicsResponse;
import org.apache.kafka.common.requests.RequestHeader;
import org.apache.kafka.common.requests.ResponseHeader;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

public class DefaultKafkaServiceImpl implements DefaultKafkaService {

    private static final Log log = LogFactory.getLog(DefaultKafkaServiceImpl.class);

    private static final short apiKey = ApiKeys.CREATE_TOPICS.id;
    public static final short version = 0;
    private static final short correlationId = -1;

    private Properties mProducerProperties;
    private Properties mConsumerProperties;
    private List<String> mTopics = new ArrayList<>();

    @Override
    public Properties getProducerProperties() {
        return mProducerProperties;
    }

    @Override
    public void setProducerProperties(Properties producerProperties) {
        mProducerProperties = producerProperties;
    }

    @Override
    public Properties getConsumerProperties() {
        return mConsumerProperties;
    }

    @Override
    public void setConsumerProperties(Properties consumerProperties) {
        mConsumerProperties = consumerProperties;
    }

    @Override
    public List<String> allTopics() {
        return mTopics;
    }

    @Override
    public void addTopic(String topic) {
        if (!mTopics.contains(topic)) {
            mTopics.add(topic);
        }
    }

    @Override
    public boolean removeTopic(String topic) {
        return mTopics.remove(topic);
    }

    @Override
    public List<String> propagateTopics(int partitions, short replication, int timeout) throws IOException {
        CreateTopicsRequest.TopicDetails topicDetails = new CreateTopicsRequest.TopicDetails(partitions, replication);
        Map<String, CreateTopicsRequest.TopicDetails> topicConfig = mTopics.stream()
                .collect(Collectors.toMap(k -> k, v -> topicDetails));

        CreateTopicsRequest request = new CreateTopicsRequest(topicConfig, timeout);

        String client = (String) getConsumerProperties().get(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG);
        List<String> errors = new ArrayList<>();
        try {
            CreateTopicsResponse response = createTopic(request, client);
//            return mTopics;
            return response.errors().entrySet().stream()
//                    .filter(error -> error.getValue() == Errors.NONE)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error(e);
        }

        return errors;
    }

    private static CreateTopicsResponse createTopic(CreateTopicsRequest request, String client) throws IllegalArgumentException, IOException {
        String[] comp = client.split(":");
        if (comp.length != 2) {
            throw new IllegalArgumentException("Wrong client directive");
        }
        String address = comp[0];
        int port = Integer.parseInt(comp[1]);

        RequestHeader header = new RequestHeader(apiKey, version, client, correlationId);
        ByteBuffer buffer = ByteBuffer.allocate(header.sizeOf() + request.sizeOf());
        header.writeTo(buffer);
        request.writeTo(buffer);

        byte byteBuf[] = buffer.array();

        byte[] resp = requestAndReceive(byteBuf, address, port);
        ByteBuffer respBuffer = ByteBuffer.wrap(resp);
        ResponseHeader.parse(respBuffer);

        return CreateTopicsResponse.parse(respBuffer);
    }

    private static byte[] requestAndReceive(byte[] buffer, String address, int port) throws IOException {
        try(Socket socket = new Socket(address, port);
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            DataInputStream dis = new DataInputStream(socket.getInputStream())
        ) {
            dos.writeInt(buffer.length);
            dos.write(buffer);
            dos.flush();

            byte resp[] = new byte[dis.readInt()];
            dis.readFully(resp);

            return resp;
        } catch (IOException e) {
            log.error(e);
        }

        return new byte[0];
    }

}