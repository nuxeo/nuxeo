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
 *     dmetzler
 */
package org.nuxeo.ecm.restapi.jaxrs.io.directory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.nuxeo.ecm.automation.core.util.DocumentHelper;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.BaseSession;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryEntry;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 5.7.3
 */
@Provider
@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON + "+nxentity" })
public class DirectoryEntryReader implements MessageBodyReader<DirectoryEntry> {

    protected static final Log log = LogFactory.getLog(DirectoryEntryReader.class);

    @Context
    private JsonFactory factory;

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return DirectoryEntry.class.isAssignableFrom(type);
    }

    @Override
    public DirectoryEntry readFrom(Class<DirectoryEntry> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException, WebApplicationException {
        String content = IOUtils.toString(entityStream);
        if (content.isEmpty()) {
            if (content.isEmpty()) {
                throw new WebException("No content in request body", Response.Status.BAD_REQUEST.getStatusCode());
            }

        }

        try {
            return readRequest(content, httpHeaders);
        } catch (IOException | ClientException e) {
            throw WebException.wrap(e);
        }
    }

    /**
     * @param content
     * @param httpHeaders
     * @return
     * @throws IOException
     * @throws JsonParseException
     * @throws ClientException
     */
    private DirectoryEntry readRequest(String content, MultivaluedMap<String, String> httpHeaders) throws IOException,
            ClientException {

        JsonParser jp = factory.createJsonParser(content);
        return readJson(jp, httpHeaders);
    }

    public static DirectoryEntry readJson(JsonParser jp, MultivaluedMap<String, String> httpHeaders)
            throws IOException, ClientException {

        JsonToken tok = jp.nextToken();

        // skip {
        if (jp.getCurrentToken() == JsonToken.START_OBJECT) {
            tok = jp.nextToken();
        }
        String directoryName = null;
        JsonNode propertiesNode = null;
        while (tok != JsonToken.END_OBJECT) {
            String key = jp.getCurrentName();
            jp.nextToken();
            if ("directoryName".equals(key)) {
                directoryName = jp.readValueAs(String.class);
            } else if ("properties".equals(key)) {
                propertiesNode = jp.readValueAsTree();
            } else if ("entity-type".equals(key)) {
                String entityType = jp.readValueAs(String.class);
                if (!DirectoryEntryWriter.ENTITY_TYPE.equals(entityType)) {
                    throw new WebApplicationException(Response.Status.BAD_REQUEST);
                }
            } else {
                log.debug("Unknown key: " + key);
                jp.skipChildren();
            }

            tok = jp.nextToken();

        }

        DirectoryService ds = Framework.getLocalService(DirectoryService.class);
        Directory directory = ds.getDirectory(directoryName);

        if (directory == null) {
            throw new WebResourceNotFoundException("Directory " + directoryName + " does not exists");
        }

        return getDirectoryEntryFromNode(propertiesNode, directory);

    }

    private static DirectoryEntry getDirectoryEntryFromNode(JsonNode propertiesNode, Directory directory)
            throws DirectoryException, ClientException, IOException {

        String schema = directory.getSchema();
        String id = propertiesNode.get(directory.getIdField()).getTextValue();

        Session session = null;
        try {
            session = directory.getSession();
            DocumentModel entry = session.getEntry(id);

            if (entry == null) {
                entry = BaseSession.createEntryModel(null, schema, id, new HashMap<String, Object>());
            }

            Properties props = new Properties();
            Iterator<Entry<String, JsonNode>> fields = propertiesNode.getFields();
            while (fields.hasNext()) {
                Entry<String, JsonNode> fieldEntry = fields.next();
                props.put(schema + ":" + fieldEntry.getKey(), fieldEntry.getValue().getTextValue());
            }

            DocumentHelper.setProperties(null, entry, props);

            return new DirectoryEntry(directory.getName(), entry);

        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

}
