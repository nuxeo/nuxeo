/*
 * (C) Copyright 2014-2017 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 *     Kevin Leturc
 *     Funsho David
 */
package org.nuxeo.runtime.mongodb;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.function.Consumer;
import java.util.stream.StreamSupport;

import javax.net.ssl.SSLContext;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.commons.beanutils.Converter;
import org.apache.commons.beanutils.FluentPropertyBeanIntrospector;
import org.apache.commons.beanutils.PropertyUtilsBean;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.ReadConcern;
import com.mongodb.ReadConcernLevel;
import com.mongodb.ReadPreference;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;

/**
 * Helper for connection to the MongoDB server
 *
 * @since 9.1
 */
public class MongoDBConnectionHelper {

    private static final Log log = LogFactory.getLog(MongoDBConnectionHelper.class);

    private static final String DB_DEFAULT = "nuxeo";

    private static final int MONGODB_OPTION_CONNECTION_TIMEOUT_MS = 30000;

    private static final int MONGODB_OPTION_SOCKET_TIMEOUT_MS = 60000;

    /** @since 11.1 */
    public static class ReadPreferenceConverter implements Converter {

        public static final ReadPreferenceConverter INSTANCE = new ReadPreferenceConverter();

        @SuppressWarnings("unchecked")
        @Override
        public <T> T convert(Class<T> type, Object value) {
            return (T) ReadPreference.valueOf((String) value);
        }
    }

    /** @since 11.1 */
    public static class ReadConcernConverter implements Converter {

        public static final ReadConcernConverter INSTANCE = new ReadConcernConverter();

        @SuppressWarnings("unchecked")
        @Override
        public <T> T convert(Class<T> type, Object value) {
            ReadConcern readConcern;
            if ("default".equalsIgnoreCase((String) value)) {
                readConcern = ReadConcern.DEFAULT;
            } else {
                ReadConcernLevel level = ReadConcernLevel.fromString((String) value);
                readConcern = new ReadConcern(level);
            }
            return (T) readConcern;
        }
    }

    /** @since 11.1 */
    public static class WriteConcernConverter implements Converter {

        public static final WriteConcernConverter INSTANCE = new WriteConcernConverter();

        @SuppressWarnings("unchecked")
        @Override
        public <T> T convert(Class<T> type, Object value) {
            return (T) WriteConcern.valueOf((String) value);
        }
    }

    private MongoDBConnectionHelper() {
        // Empty
    }

    /**
     * Initialize a connection to the MongoDB server
     *
     * @param server the server url
     * @return the MongoDB client
     */
    public static MongoClient newMongoClient(String server) {
        MongoDBConnectionConfig config = new MongoDBConnectionConfig();
        config.server = server;
        return newMongoClient(config);
    }

    /**
     * Initializes a connection to the MongoDB server.
     *
     * @param config the MongoDB connection config
     * @return the MongoDB client
     * @since 10.3
     */
    public static MongoClient newMongoClient(MongoDBConnectionConfig config) {
        return newMongoClient(config, null);
    }

    /**
     * Initializes a connection to the MongoDB server.
     *
     * @param config the MongoDB connection config
     * @param optionsConsumer a consumer of the client options builder
     * @return the MongoDB client
     * @since 11.1
     */
    public static MongoClient newMongoClient(MongoDBConnectionConfig config,
                                             Consumer<MongoClientOptions.Builder> optionsConsumer) {
        String server = config.server;
        if (StringUtils.isBlank(server)) {
            throw new RuntimeException("Missing <server> in MongoDB descriptor");
        }
        MongoClientOptions.Builder optionsBuilder = MongoClientOptions.builder().applicationName("Nuxeo");
        SSLContext sslContext = getSSLContext(config);
        if (sslContext == null) {
            if (config.ssl != null) {
                optionsBuilder.sslEnabled(config.ssl.booleanValue());
            }
        } else {
            optionsBuilder.sslEnabled(true);
            optionsBuilder.sslContext(sslContext);
        }

        // don't wait forever by default when connecting
        optionsBuilder.connectTimeout(MONGODB_OPTION_CONNECTION_TIMEOUT_MS);
        optionsBuilder.socketTimeout(MONGODB_OPTION_SOCKET_TIMEOUT_MS);

        // set properties from Nuxeo config descriptor
        ConvertUtilsBean convertUtils = new ConvertUtilsBean();
        convertUtils.register(ReadPreferenceConverter.INSTANCE, ReadPreference.class);
        convertUtils.register(ReadConcernConverter.INSTANCE, ReadConcern.class);
        convertUtils.register(WriteConcernConverter.INSTANCE, WriteConcern.class);
        PropertyUtilsBean propertyUtils = new PropertyUtilsBean();
        propertyUtils.addBeanIntrospector(new FluentPropertyBeanIntrospector(""));
        BeanUtilsBean beanUtils = new BeanUtilsBean(convertUtils, propertyUtils);
        try {
            beanUtils.populate(optionsBuilder, config.properties);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }

        // hook for caller to set additional properties
        if (optionsConsumer != null) {
            optionsConsumer.accept(optionsBuilder);
        }

        MongoClient client;
        if (server.startsWith("mongodb://") || server.startsWith("mongodb+srv://")) {
            // allow mongodb*:// URI syntax for the server, to pass everything in one string
            client = new MongoClient(new MongoClientURI(server, optionsBuilder));
        } else {
            client = new MongoClient(new ServerAddress(server), optionsBuilder.build());
        }
        if (log.isDebugEnabled()) {
            log.debug("MongoClient initialized with options: " + client.getMongoClientOptions().toString());
        }
        return client;
    }

    protected static SSLContext getSSLContext(MongoDBConnectionConfig config) {
        try {
            KeyStore trustStore = loadKeyStore(config.trustStorePath, config.trustStorePassword, config.trustStoreType);
            KeyStore keyStore = loadKeyStore(config.keyStorePath, config.keyStorePassword, config.keyStoreType);
            if (trustStore == null && keyStore == null) {
                return null;
            }
            SSLContextBuilder sslContextBuilder = SSLContexts.custom();
            if (trustStore != null) {
                sslContextBuilder.loadTrustMaterial(trustStore, null);
            }
            if (keyStore != null) {
                sslContextBuilder.loadKeyMaterial(keyStore,
                        StringUtils.isBlank(config.keyStorePassword) ? null : config.keyStorePassword.toCharArray());
            }
            return sslContextBuilder.build();
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("Cannot setup SSL context: " + config, e);
        }
    }

    protected static KeyStore loadKeyStore(String path, String password, String type)
            throws GeneralSecurityException, IOException {
        if (StringUtils.isBlank(path)) {
            return null;
        }
        String keyStoreType = StringUtils.defaultIfBlank(type, KeyStore.getDefaultType());
        KeyStore keyStore = KeyStore.getInstance(keyStoreType);
        char[] passwordChars = StringUtils.isBlank(password) ? null : password.toCharArray();
        try (InputStream is = Files.newInputStream(Paths.get(path))) {
            keyStore.load(is, passwordChars);
        }
        return keyStore;
    }

    /**
     * @return a database representing the specified database
     */
    public static MongoDatabase getDatabase(MongoClient mongoClient, String dbname) {
        if (StringUtils.isBlank(dbname)) {
            dbname = DB_DEFAULT;
        }
        return mongoClient.getDatabase(dbname);
    }

    /**
     * Check if the collection exists and if it is not empty
     *
     * @param mongoDatabase the Mongo database
     * @param collection the collection name
     * @return true if the collection exists and not empty, false otherwise
     */
    @SuppressWarnings("deprecation")
    public static boolean hasCollection(MongoDatabase mongoDatabase, String collection) {
        MongoIterable<String> collections = mongoDatabase.listCollectionNames();
        boolean found = StreamSupport.stream(collections.spliterator(), false).anyMatch(collection::equals);
        // we do not need precision here, using the old count allows to preserve method signature
        return found && mongoDatabase.getCollection(collection).count() > 0; // NOSONAR
    }
}
