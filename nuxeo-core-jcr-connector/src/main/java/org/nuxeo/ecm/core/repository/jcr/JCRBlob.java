/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.repository.jcr;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.api.impl.blob.StreamBlob;

/**
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
// TODO this class is not serializable -> cannot be used by a DTO
public class JCRBlob extends StreamBlob {

    public static final Log log = LogFactory.getLog(JCRBlob.class);

    public static final String ENCODING = "jcr:encoding";

    public static final String MIMETYPE = "jcr:mimeType";

    public static final String DATA = "jcr:data";

    public static final String NT_NAME = NodeConstants.ECM_NT_CONTENT.rawname;

    public static final String FILENAME = NodeConstants.ECM_CONTENT_FILENAME.rawname;

    public static final String LENGTH = NodeConstants.ECM_CONTENT_LENGTH.rawname;

    public static final String DIGEST = NodeConstants.ECM_CONTENT_DIGEST.rawname;

    final Node node;

    public JCRBlob(Node node) {
        this.node = node;
    }

    public void setFilename(String filename) {
        if (filename != null) {
            try {
                node.setProperty(FILENAME, filename);
            } catch (Exception e) {
                if (compat_14_adaptNode(node)) {// if node type was invalid and adapting was successful - try again
                    try {
                        node.setProperty(FILENAME, filename);
                    } catch (Exception ee) {
                        ee.printStackTrace();
                    }
                }
            }
        }
    }

    public void setDigest(String digest) {
        if (digest != null) {
            try {
                node.setProperty(DIGEST, digest);
            } catch (Exception e) {
                if (compat_14_adaptNode(node)) {// if node type was invalid and adapting was successful - try again
                    try {
                        node.setProperty(DIGEST, digest);
                    } catch (Exception ee) {
                        ee.printStackTrace();
                    }
                }
            }
        }
    }

    public void setLength(long length) {
        try {
            node.setProperty(LENGTH, length);
        } catch (Exception e) {
            if (compat_14_adaptNode(node)) {
                // if node type was invalid and adapting was succesfull - try again
                try {
                    node.setProperty(LENGTH, length);
                } catch (Exception ee) {
                    ee.printStackTrace();
                }
            }
        }
    }

    public void setEncoding(String encoding) {
        if (encoding != null) {
            try {
                node.setProperty(ENCODING, encoding);
            } catch (Exception e) {
                // TODO: re-throw wrapped in IOException as done in getStream
                // instead (embedded cause with Throwable.initCause)
                e.printStackTrace();
            }
        }
    }

    public void setMimeType(String mimeType) {
        if (mimeType != null) {
            try {
                node.setProperty(MIMETYPE, mimeType);
            } catch (Exception e) {
                // TODO: re-throw wrapped in IOException as done in getStream
                // instead (embedded cause with Throwable.initCause)
                e.printStackTrace();
            }
        }
    }

    public String getFilename() {
        try {
            return node.getProperty(FILENAME).getString();
        } catch (Exception e) {
            // do not throw exception this may happens when suing old repos (old blob schemas)
            //e.printStackTrace();
        }
        return null;
    }

    public String getDigest() {
        try {
            return node.getProperty(DIGEST).getString();
        } catch (Exception e) {
            // do not throw exception this may happens when suing old repos (old blob schemas)
            //e.printStackTrace();
        }
        return null;
    }

    @Override
    public long getLength() {
        try {
            return node.getProperty(LENGTH).getLong();
        } catch (Exception e) {
            // do not throw exception this may happens when suing old repos (old blob schemas)
            //e.printStackTrace();
        }
        return -1;
    }

    public String getEncoding() {
        try {
            return node.getProperty(ENCODING).getString();
        } catch (PathNotFoundException e) {
            return null;
        } catch (RepositoryException e) {
            // TODO: re-throw wrapped in IOException as done in getStream
            // instead (embedded cause with Throwable.initCause)
            //e.printStackTrace();
        }
        return null;
    }

    public String getMimeType() {
        try {
            return node.getProperty(MIMETYPE).getString();
        } catch (PathNotFoundException e) {
            return null;
        } catch (RepositoryException e) {
            // TODO: re-throw wrapped in IOException as done in getStream
            // instead (embedded cause with Throwable.initCause)
            //e.printStackTrace();
        }
        return null;
    }

    /**
     * Get the underlying stream. Use getStream() instead
     * @return
     * @throws IOException
     */
    public InputStream _getStream() throws IOException {
        try {
            return node.getProperty(DATA).getStream();
        } catch (PathNotFoundException e) {
            return EMPTY_INPUT_STREAM;
        } catch (RepositoryException e) {
            throw (IOException) (new IOException("could fetch stream").initCause(e));
        }
    }

    /**
     * Do not return the Jackrabbit stream directly. Wrap it inside a
     * JCRBlobInputStream to be able to reset the stream and read it more than
     * once.
     * <p>
     * Fix for NXP-2072. See also NXP-2072.
     *
     * @see JCRBlobInputStream
     */
    public InputStream getStream() {
        return new JCRBlobInputStream(this);
    }

    public static Node getOrCreateContentNode(Node node, String name)
            throws RepositoryException {
        try {
            return node.getNode(name);
        } catch (PathNotFoundException e) {
            return node.addNode(name, NT_NAME);
        }
    }

    public static void setContent(Node node, String name, Blob value)
            throws DocumentException {
        // TODO: move this into JCRContentSource ?
        try {
            if (value == null) { // remove content
                try {
                    node.getNode(name).remove();
                } catch (PathNotFoundException e) {
                    // silently ignore
                }
                return;
            }
            Node csNode = getOrCreateContentNode(node, name);
            setContent(csNode, value);
        } catch (Exception e) {
            throw new DocumentException("setContent failed", e);
        }
    }

    // compatibility code -> this should work with older core versions (<1.4) that are using nt:resource to store blobs
    private static boolean compat_14_adaptNode(Node node) {
        try {
            NodeType nt = node.getPrimaryNodeType();
            if (nt.getName().equals("nt:resource")) {
                NodeType[] mixins = node.getMixinNodeTypes();
                for (NodeType mixin : mixins) {
                    if (NodeConstants.ECM_MIX_CONTENT.equals(mixin.getName())) {
                        return false;
                    }
                }
                // adapt the node by adding the blob mixin type
                node.addMixin(NodeConstants.ECM_MIX_CONTENT.rawname);
                log.info("Deprecated blob property node type was succesfully adapted");
                return true;
            }
        } catch (Exception e) {
            log.error("Failed to adapt incompatible blob property node type", e);
        }
        return false;
    }

    public static void setContent(Node node, Blob value) throws DocumentException {
        // TODO: move this into JCRContentSource ?
        InputStream in = null;
        try {
            if (value == null) { // remove content
                node.remove();
                return;
            }
            // set first the filename, length and digest first - to catch node
            // type incompatibility errors from the begining
            String filename = value.getFilename();
            if (filename != null) {
                node.setProperty(FILENAME, filename);
            }
            String digest = value.getDigest();
            if (digest != null) {
                node.setProperty(DIGEST, digest);
            }
            long length = value.getLength();
            if (length > -1) {
                node.setProperty(LENGTH, length);
            }
            in = value.getStream();
            if (in != null) {
                node.setProperty(DATA, in);
            }
            String encoding = value.getEncoding();
            if (encoding != null) {
                node.setProperty(ENCODING, encoding);
            }
            String ctype = value.getMimeType();
            if (ctype == null) {
                ctype = "application/octet-stream";
            }
            node.setProperty(MIMETYPE, ctype);
            node.setProperty("jcr:lastModified", Calendar.getInstance());
        } catch (Exception e) {
            if (compat_14_adaptNode(node)) {
                // if node type was invalid and adapting was successful - try again
                setContent(node, value);
                return; // do not thrown exception
            }
            throw new DocumentException("setContent failed", e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // XXX: should not be catched
                    e.printStackTrace();
                }
            }
        }
    }

    public boolean isPersistent() {
        return false;
    }

    public Blob persist() throws IOException {
        return new FileBlob(getStream(), getMimeType(), getEncoding());
    }

}
