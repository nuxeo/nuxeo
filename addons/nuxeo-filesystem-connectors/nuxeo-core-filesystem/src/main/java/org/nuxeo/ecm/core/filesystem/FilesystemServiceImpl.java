/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.filesystem;

import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.filesystem.FilesystemService;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;

/**
 * Default implementation of the {@link FilesystemService}.
 * <p>
 * Assumes that URL path components correspond to Nuxeo document names.
 */
public class FilesystemServiceImpl implements FilesystemService {

    @Override
    public String getFilename(DocumentModel doc) throws ClientException {
        String filename;
        try {
            Blob blob = (Blob) doc.getPropertyValue(FILE_PROPERTY);
            filename = blob == null ? null : blob.getFilename();
        } catch (PropertyNotFoundException e) {
            filename = null;
        }
        return filename;
    }

    @Override
    public String getTitle(DocumentModel doc) throws ClientException {
        String title;
        try {
            title = (String) doc.getPropertyValue(TITLE_PROPERTY);
        } catch (PropertyNotFoundException e) {
            title = null;
        }
        return title;
    }

    @Override
    public String getName(DocumentModel doc) throws ClientException {
        return doc.getName();
    }

    @Override
    public DocumentModel resolvePath(CoreSession session, String path) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public Blob resolveBlobPath(CoreSession session, String path) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getChildrenPaths(CoreSession session, String path) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public void getCreationInfo(CoreSession session, String folderPath,
            Blob blob) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public BlobHolder getBlobHolder(CoreSession session, String path) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public void set(DocumentModel doc, DocumentModel oldDoc)
            throws ClientException {
        String filename = getFilename(doc);
        String title = getTitle(doc);
        String name = getName(doc);

        String oldFilename;
        if (oldDoc == null) {
            oldFilename = null;
        } else {
            oldFilename = getFilename(oldDoc);
            if (StringUtils.equals(filename, oldFilename)) {
                filename = null;
            }
            if (StringUtils.equals(title, getTitle(oldDoc))) {
                title = null;
            }
            if (StringUtils.equals(name, getName(oldDoc))) {
                name = null;
            }
        }

        if (filename != null || title != null || name != null) {
            set(doc, filename, title, name, oldFilename);
        }
    }

    @Override
    public void set(DocumentModel doc, String filename, String title,
            String name, String oldFilename) throws ClientException {
        if (filename != null) {
            title = filename;
            name = filename;
        } else {
            String suffix;
            if (oldFilename == null) {
                oldFilename = getFilename(doc);
            }
            suffix = getSuffix(oldFilename);
            if (title != null) {
                title = fixSuffix(title, suffix);
                name = title;
                filename = title;
            } else if (name != null) {
                name = fixSuffix(name, suffix);
                title = name;
                filename = name;
            }
        }
        setFilenameTitleName(doc, filename, title, name);
    }

    protected static final int SUFFIX_MAX_LEN = 6;

    protected static String getSuffix(String string) {
        if (string == null) {
            return "";
        }
        string = StringUtils.trim(string);
        int pos = string.lastIndexOf('.') + 1;
        int len = string.length() - pos;
        if (pos == 0 || len > SUFFIX_MAX_LEN || len < 1) {
            return "";
        }
        String suffix = string.substring(pos);
        // check only chars
        for (char c : suffix.toCharArray()) {
            if (!Character.isLetter(c) && !Character.isDigit(c)) {
                return "";
            }
        }
        return '.' + suffix;
    }

    protected static String fixSuffix(String string, String suffix) {
        if (suffix.length() != 0 && !string.endsWith(suffix)) {
            // try to remove previous suffix
            int li = string.lastIndexOf('.');
            if (li > 0 && li >= string.length() - SUFFIX_MAX_LEN - 1) {
                string = string.substring(0, li);
            }
            string += suffix;
        }
        return string;
    }

    /**
     * Sets values exactly as specified.
     */
    protected void setFilenameTitleName(DocumentModel doc, String filename,
            String title, String name) throws ClientException {
        if (filename != null) {
            try {
                Property prop = doc.getProperty(FILE_PROPERTY);
                Blob blob = (Blob) prop.getValue();
                if (blob != null) {
                    blob.setFilename(filename);
                    prop.setValue(blob);
                }
            } catch (PropertyNotFoundException e) {
                // ignore
            }
        }
        if (title != null) {
            try {
                doc.setPropertyValue(TITLE_PROPERTY, title);
            } catch (PropertyNotFoundException e) {
                // ignore
            }
        }
        if (name != null) {
            doc.setPathInfo(doc.getPath().removeLastSegments(1).toString(),
                    name);
        }
    }

    /**
     * Finds a document in the current folder having the same file.
     * <p>
     * Useful for drag & drop import, or "Import File" button.
     */
    public DocumentRef findSameDocumentInFolder(/* ... */) {
        return null;
    }

    /**
     * Creates a document with an optional attached file.
     * <p>
     * Method {@link CoreSession#createDocument} must be called after this.
     *
     * @return the document, ready to be created in the session
     * @throws ClientException
     */
    public DocumentModel newDocument(CoreSession session,
            DocumentRef parentRef, String type, String title, Blob blob)
            throws ClientException {
        DocumentModel doc = session.createDocumentModel(type);

        String filename = blob == null ? null : blob.getFilename();
        filename = clean(filename);

        title = clean(title);

        String name;
        if (title == null && filename == null) {
            name = type;
        } else if (title == null) {
            name = filename;
        } else {
            name = title;
        }

        // set title
        try {
            doc.setPropertyValue(TITLE_PROPERTY, name);
        } catch (PropertyNotFoundException e) {
            // no dc:title property
        }

        // set blob and its filename
        if (blob != null) {
            try {
                Property prop = doc.getProperty(FILE_PROPERTY);
                if (!name.equals(blob.getFilename())) {
                    // update filename
                    prop.setValue(blob);
                    // refetch as a SQLBlob which is updatable
                    blob = (Blob) prop.getValue();
                    blob.setFilename(name);
                }
                prop.setValue(blob);
            } catch (PropertyNotFoundException e) {
                // no file:content property
            }
        }

        // set name (and parent path)
        String parentPath;
        if (parentRef instanceof PathRef) {
            parentPath = parentRef.toString();
        } else {
            parentPath = session.getDocument(parentRef).getPathAsString();
        }
        doc.setPathInfo(parentPath, name);

        return doc;
    }

    /**
     * Cleans a string, returning {@code null} if it was too simple.
     *
     * @param s
     * @return the cleaned string, or {@code null}
     */
    protected String clean(String s) {
        if (s == null) {
            return null;
        }
        s = s.trim();
        int max = getMaxSize();
        if (s.length() > max) {
            s = s.substring(0, max).trim();
        }
        s = s.replace("/", "-");
        s = s.replace("\\", "-");
        return tooSimple(s) ? null : s;
    }

    protected int getMaxSize() {
        return 24; // TODO parametrize + bigger
    }

    protected static final Pattern TOO_SIMPLE = Pattern.compile("^[- .,;?!:/\\\\'\"]*$");

    protected boolean tooSimple(String s) {
        return TOO_SIMPLE.matcher(s).matches();
    }

}
