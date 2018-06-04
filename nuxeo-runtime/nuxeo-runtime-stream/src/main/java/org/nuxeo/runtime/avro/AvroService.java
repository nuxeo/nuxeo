/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     pierre
 */
package org.nuxeo.runtime.avro;

import org.apache.avro.Schema;
import org.apache.avro.message.SchemaStore;

/**
 * This service allows to create a {@link AvroSchemaFactoryContext}.
 *
 * @since 10.2
 */
public interface AvroService extends SchemaStore {

    /**
     * Registers the schema into the SchemaStore.
     *
     * @param schema to be registered
     */
    void addSchema(Schema schema);

    /**
     * Creates the Avro schema from an object.<br>
     * <br>
     * An AvroSchemaFactory handling the object class has to be implemented and registered to the AvroComponent..
     *
     * @param input any object
     * @return the Avro schema
     */
    <D> Schema createSchema(D input);

    /**
     * Decodes a valid Avro name to its actual value.<br>
     *
     * @param input the name to decode
     * @return the decoded name
     */
    String decodeName(String input);

    /**
     * Encodes a name for it to be eligible to Avro limitations (alphanumeric and _).<br>
     * <br>
     * By default Nuxeo can encode - and :<br>
     * Other replacements can be registered to the AvroComponent.<br>
     *
     * @param input the name to encode
     * @return the encoded name
     */
    String encodeName(String input);

    /**
     * Map an Avro data to an instance of the given class.<br>
     * <br>
     * An AvroMapper handling the given class has to be implemented and registered to the AvroComponent..
     *
     * @param schema the Avro schema
     * @param clazz the class to map the Avro object to
     * @param object the Avro data
     * @return an instance of the given class
     */
    <D, M> D fromAvro(Schema schema, Class<D> clazz, M object);

    /**
     * Map an object to an Avro data.<br>
     * <br>
     * An AvroMapper handling the given class has to be implemented and registered.
     *
     * @param schema the Avro schema
     * @param input the object to map to an Avro data
     * @return the Avro data
     */
    <D, M> M toAvro(Schema schema, D input);

}
