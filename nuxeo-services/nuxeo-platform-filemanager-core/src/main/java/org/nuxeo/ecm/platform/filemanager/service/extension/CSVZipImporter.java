/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.platform.filemanager.service.extension;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CloseableFile;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.TypeConstants;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.SimpleTypeImpl;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;
import org.nuxeo.ecm.core.schema.types.primitives.IntegerType;
import org.nuxeo.ecm.core.schema.types.primitives.LongType;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;
import org.nuxeo.ecm.platform.types.TypeManager;

public class CSVZipImporter extends AbstractFileImporter {

    private static final long serialVersionUID = 1L;

    private static final String MARKER = "meta-data.csv";

    private static final Log log = LogFactory.getLog(CSVZipImporter.class);

    public static ZipFile getArchiveFileIfValid(File file) throws IOException {
        ZipFile zip;

        try {
            zip = new ZipFile(file);
        } catch (ZipException e) {
            log.debug("file is not a zipfile ! ", e);
            return null;
        } catch (IOException e) {
            log.debug("can not open zipfile ! ", e);
            return null;
        }

        ZipEntry marker = zip.getEntry(MARKER);

        if (marker == null) {
            zip.close();
            return null;
        } else {
            return zip;
        }
    }

    @Override
    public boolean isOneToMany() {
        return true;
    }

    @Override
    public DocumentModel create(CoreSession documentManager, Blob content, String path, boolean overwrite,
            String filename, TypeManager typeService) throws IOException {
        ZipFile zip = null;
        try (CloseableFile source = content.getCloseableFile()) {
            zip = getArchiveFileIfValid(source.getFile());
            if (zip == null) {
                return null;
            }

            DocumentModel container = documentManager.getDocument(new PathRef(path));

            ZipEntry index = zip.getEntry(MARKER);
            try (Reader reader = new InputStreamReader(zip.getInputStream(index));
                    CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withHeader());) {

                Map<String, Integer> header = csvParser.getHeaderMap();
                for (CSVRecord csvRecord : csvParser) {
                    String type = null;
                    String id = null;
                    Map<String, String> stringValues = new HashMap<>();
                    for (String headerValue : header.keySet()) {
                        String lineValue = csvRecord.get(headerValue);
                        if ("type".equalsIgnoreCase(headerValue)) {
                            type = lineValue;
                        } else if ("id".equalsIgnoreCase(headerValue)) {
                            id = lineValue;
                        } else {
                            stringValues.put(headerValue, lineValue);
                        }
                    }

                    boolean updateDoc = false;
                    // get doc for update
                    DocumentModel targetDoc = null;
                    if (id != null) {
                        // update ?
                        String targetPath = new Path(path).append(id).toString();
                        if (documentManager.exists(new PathRef(targetPath))) {
                            targetDoc = documentManager.getDocument(new PathRef(targetPath));
                            updateDoc = true;
                        }
                    }

                    // create doc if needed
                    if (targetDoc == null) {
                        if (type == null) {
                            log.error("Can not create doc without a type, skipping line");
                            continue;
                        }

                        if (id == null) {
                            id = IdUtils.generateStringId();
                        }
                        targetDoc = documentManager.createDocumentModel(path, id, type);
                    }

                    // update doc properties
                    DocumentType targetDocType = targetDoc.getDocumentType();
                    for (Map.Entry<String, String> entry : stringValues.entrySet()) {
                        String fname = entry.getKey();
                        String stringValue = entry.getValue();
                        Field field = null;
                        boolean usePrefix = false;
                        String schemaName = null;
                        String fieldName = null;

                        if (fname.contains(":")) {
                            if (targetDocType.hasField(fname)) {
                                field = targetDocType.getField(fname);
                                usePrefix = true;
                            }
                        } else if (fname.contains(".")) {
                            String[] parts = fname.split("\\.");
                            schemaName = parts[0];
                            fieldName = parts[1];
                            if (targetDocType.hasSchema(schemaName)) {
                                field = targetDocType.getField(fieldName);
                                usePrefix = false;
                            }
                        } else {
                            if (targetDocType.hasField(fname)) {
                                field = targetDocType.getField(fname);
                                usePrefix = false;
                                schemaName = field.getDeclaringType().getSchemaName();
                            }
                        }

                        if (field != null) {
                            Serializable fieldValue = getFieldValue(field, stringValue, zip);

                            if (fieldValue != null) {
                                if (usePrefix) {
                                    targetDoc.setPropertyValue(fname, fieldValue);
                                } else {
                                    targetDoc.setProperty(schemaName, fieldName, fieldValue);
                                }
                            }
                        }
                    }
                    if (updateDoc) {
                        documentManager.saveDocument(targetDoc);
                    } else {
                        documentManager.createDocument(targetDoc);
                    }
                }
            }
            return container;
        } finally {
            IOUtils.closeQuietly(zip);
        }
    }

    protected Serializable getFieldValue(Field field, String stringValue, ZipFile zip) {
        Serializable fieldValue = null;
        Type type = field.getType();
        if (type.isSimpleType()) {
            if (type instanceof SimpleTypeImpl) {
                // consider super type instead
                type = type.getSuperType();
            }
            if (type instanceof StringType) {
                fieldValue = stringValue;
            } else if (type instanceof IntegerType) {
                fieldValue = Integer.parseInt(stringValue);
            } else if (type instanceof LongType) {
                fieldValue = Long.parseLong(stringValue);
            } else if (type instanceof DateType) {
                try {
                    Date date;
                    if (stringValue.length() == 10) {
                        date = new SimpleDateFormat("dd/MM/yyyy").parse(stringValue);
                    } else if (stringValue.length() == 8) {
                        date = new SimpleDateFormat("dd/MM/yy").parse(stringValue);
                    } else {
                        log.warn("Unknown date format :" + stringValue);
                        return null;
                    }
                    fieldValue = date;
                } catch (ParseException e) {
                    log.error("Error during date parsing", e);
                }
            } else {
                log.warn(String.format("Unsupported field type '%s'", type));
                return null;
            }
        } else if (type.isComplexType() && TypeConstants.CONTENT.equals(field.getName().getLocalName())) {
            ZipEntry blobIndex = zip.getEntry(stringValue);
            if (blobIndex != null) {
                Blob blob;
                try {
                    blob = Blobs.createBlob(zip.getInputStream(blobIndex));
                } catch (IOException e) {
                    throw new NuxeoException(e);
                }
                blob.setFilename(stringValue);
                fieldValue = (Serializable) blob;
            }
        }

        return fieldValue;
    }

}
