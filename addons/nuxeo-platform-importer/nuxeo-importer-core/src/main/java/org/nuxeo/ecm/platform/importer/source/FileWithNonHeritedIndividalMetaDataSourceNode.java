/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.ecm.platform.importer.source;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolderWithProperties;

public class FileWithNonHeritedIndividalMetaDataSourceNode extends FileSourceNode {

    private static final Log log = LogFactory.getLog(FileWithNonHeritedIndividalMetaDataSourceNode.class);

    public static final String PROPERTY_FILE_SUFIX = ".properties";

    protected static Pattern numPattern = Pattern.compile("([0-9\\,\\.\\+\\-]+)");

    public static final String LIST_SEPARATOR = "|";

    public static final String REGEXP_LIST_SEPARATOR = "\\|";

    public static final String ARRAY_SEPARATOR = "||";

    public static final String REGEXP_ARRAY_SEPARATOR = "\\|\\|";

    public FileWithNonHeritedIndividalMetaDataSourceNode(File file) {
        super(file);
    }

    public FileWithNonHeritedIndividalMetaDataSourceNode(String path) {
        super(path);
    }

    @Override
    public List<SourceNode> getChildren() {
        List<SourceNode> children = new ArrayList<SourceNode>();
        File[] listFiles = file.listFiles();
        for (File child : listFiles) {
            if (isPropertyFile(child)) {
                // skip
            } else {
                children.add(new FileWithNonHeritedIndividalMetaDataSourceNode(child));
            }
        }
        return children;
    }

    protected boolean isPropertyFile(File file) {
        return (file.getName().contains(PROPERTY_FILE_SUFIX));
    }

    @Override
    public BlobHolder getBlobHolder() throws IOException {
        BlobHolder bh = null;
        String metadataFilename = file.getParent() + File.separator + getFileNameNoExt(file) + PROPERTY_FILE_SUFIX;
        File metadataFile = new File(metadataFilename);
        if (metadataFile.exists()) {
            bh = new SimpleBlobHolderWithProperties(Blobs.createBlob(file), loadPropertyFile(metadataFile));
        } else {
            bh = new SimpleBlobHolder(Blobs.createBlob(file));
        }
        return bh;
    }

    protected Map<String, Serializable> loadPropertyFile(File propertyFile) {
        Properties mdProperties = new Properties();
        Map<String, Serializable> map = new HashMap<String, Serializable>();

        try {
            mdProperties.load(new FileInputStream(propertyFile));
            Enumeration<?> names = mdProperties.propertyNames();
            while (names.hasMoreElements()) {
                String name = (String) names.nextElement();
                map.put(name, parseFromString(mdProperties.getProperty(name)));
            }
        } catch (IOException e) {
            log.error("Unable to read property file " + propertyFile, e);
        }
        return map;
    }

    protected Serializable parseFromString(String value) {

        Serializable prop = value;
        if (value.contains(ARRAY_SEPARATOR)) {
            prop = value.split(REGEXP_ARRAY_SEPARATOR);
        } else if (value.contains(LIST_SEPARATOR)) {
            List<Serializable> lstprop = new ArrayList<Serializable>();
            String[] parts = value.split(REGEXP_LIST_SEPARATOR);
            for (String part : parts) {
                lstprop.add(parseFromString(part));
            }
            prop = (Serializable) lstprop;
        }
        return prop;
    }
}
