/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: IndexableDocumentInfoImpl.java 13094 2007-03-01 13:36:13Z janguenot $
 */

package org.nuxeo.ecm.core.search.api.client.indexing.resources.document.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.TypeProvider;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.search.api.client.IndexingException;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.document.DocumentIndexableResource;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.IndexableResourceConf;
import org.nuxeo.runtime.api.Framework;

/**
 * Document indexable resource implementation.
 *
 * @see org.nuxeo.ecm.core.search.api.client.indexing.resources.document.DocumentIndexableResource
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class DocumentIndexableResourceImpl extends
        AbstractNXCoreIndexableResource implements DocumentIndexableResource {

    private static final long serialVersionUID = 782382133065340451L;

    private static final Log log = LogFactory.getLog(DocumentIndexableResourceImpl.class);

    public static final String BLOB_DATA_KEY = "data";

    public static final String BLOB_ENCODING_KEY = "encoding";

    public static final String BLOB_MIMETYPE_KEY = "mime-type";

    public static final String BLOB_DIGEST_KEY = "digest";

    public static final String BLOB_NAME_KEY = "name";

    public static final String BLOB_LENGTH_KEY = "length";

    protected String docUUID;

    protected DocumentRef docRef;

    protected DocumentRef docParentRef;

    protected Path docPath;

    protected String docURL;

    protected String docType;

    protected String docVersionLabel;

    protected String docName;

    protected String docCurrentLifeCycle;

    protected ACP docAcp;

    protected Boolean isDocVersion;

    protected Boolean isDocProxy;

    protected List<String> docFacets;

    protected Set<String> docSchemas;

    protected DocumentModel targetDoc;

    protected Long flags = 0L; // XXX depends on DocumentModelImpl

    public DocumentIndexableResourceImpl() {
    }

    public DocumentIndexableResourceImpl(DocumentModel dm,
            IndexableResourceConf conf, String sid) {
        super(conf != null ? conf.getName() : null, conf, sid,
                dm.getRepositoryName());
        docUUID = dm.getId();
        docRef = dm.getRef();
        docParentRef = dm.getParentRef();
        docPath = dm.getPath();
        docURL = "foo/bar"; // FIXME use an URL resolver here.
        docType = dm.getType();
        docVersionLabel = dm.getVersionLabel();
        isDocVersion = dm.isVersion();
        isDocProxy = dm.isProxy();
        docName = dm.getName();

        // dm has been reloaded from new core session if called from IndexingThread
        targetDoc = dm;

        // Life cycle

        if (dm.isLifeCycleLoaded()) {
            try {
                docCurrentLifeCycle = dm.getCurrentLifeCycleState();
            } catch (ClientException e) {
                log.warn("Cannot get additionial document model properties.");
            }
        }
        try {
            docAcp = dm.getACP();
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }

        // FACETS

        if (dm.getFacets() != null) {
            docFacets = new ArrayList<String>(dm.getFacets());
        } else {
            docFacets = Collections.emptyList();
        }

        // SCHEMAS (this could be more lazy: needed by getValueFor only
        String[] schemas = dm.getSchemas();
        if (schemas != null) {
            docSchemas = new HashSet<String>();
            docSchemas.addAll(Arrays.asList(schemas));
        } else {
            docSchemas = Collections.emptySet();
        }

        if (dm instanceof DocumentModelImpl) {
            flags = dm.getFlags();
        }
    }

    public DocumentRef getDocRef() {
        return docRef;
    }

    @SuppressWarnings("unchecked")
    protected Serializable extractComplexProperty(Serializable complex,
            String subField) {
        if (complex == null) {
            return null;
        }
        if (complex instanceof List) {
            List<Serializable> l = (List<Serializable>) complex;
            List<Serializable> res = new ArrayList<Serializable>(l.size());
            for (Serializable s : l) {
                res.add(extractComplexProperty(s, subField));
            }
            return (Serializable) res;
        }
        if (complex instanceof Map) {
            return ((Map<String, Serializable>) complex).get(subField);
        }
        if (complex instanceof Blob) {
            Blob blob = (Blob) complex;
            if (BLOB_ENCODING_KEY.equals(subField)) {
                return blob.getEncoding();
            }
            if (BLOB_MIMETYPE_KEY.equals(subField)) {
                return blob.getMimeType();
            }
            if (BLOB_DIGEST_KEY.equals(subField)) {
                return blob.getDigest();
            }
            if (BLOB_LENGTH_KEY.equals(subField)) {
                return blob.getLength();
            }
            if (BLOB_NAME_KEY.equals(subField)) {
                return blob.getFilename();
            }
            if (BLOB_DATA_KEY.equals(subField)) {
                return (Serializable) blob;
            }
            log.warn("Unknown sub-property for Blob: " + subField);
            return (Serializable) blob;
        }

        return complex;
    }

    public Serializable getValueFor(String indexableDataName)
            throws IndexingException {

        String[] split = indexableDataName.split(":", 3);
        if (split.length < 2) {
            log.warn("Invalid field name: " + indexableDataName);
            return null;
        }
        String schemaPrefix = split[0];
        if (!docSchemas.contains(schemaPrefix)) {
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "Document %s (type=%s) does not bear schema %s",
                        docUUID, docType, schemaPrefix));
            }
            return null;
        }

        String fieldName = split[1];

        Serializable res = null;
        try {
            login();

            if (docRef != null) {
                try {
                    if (targetDoc != null) {
                        res = (Serializable) targetDoc.getProperty(schemaPrefix, fieldName);
                    } else {
                        DocumentModel doc = getCoreSession().getDocument(docRef);
                        TypeProvider typeProvider = Framework.getLocalService(SchemaManager.class);
                        Schema docSchema = doc.hasSchema(schemaPrefix) ? typeProvider.getSchema(schemaPrefix)
                                : null;
                        if (docSchema != null) {
                            String prefix = docSchema.getNamespace().prefix;
                            if (prefix != null && prefix.length() > 0) {
                                fieldName = prefix + ':' + fieldName;
                            }
                            res = doc.getPropertyValue(fieldName);
                        } else {
                            log.warn("Cannot find schema with name="
                                    + schemaPrefix);
                        }
                    }
                    if (split.length > 2) {
                        res = extractComplexProperty(res, split[2]);
                    }
                } catch (ClientException ce) {
                    throw new IndexingException(ce);
                }
            }
        } finally {
            logout();
        }
        return res;
    }

    public DocumentRef getDocParentRef() {
        return docParentRef;
    }

    public Path getDocPath() {
        return docPath;
    }

    public String getDocType() {
        return docType;
    }

    public String getDocURL() {
        return docURL;
    }

    public ACP getDocMergedACP() throws IndexingException {
        if (docAcp == null) {
            try {
                login();
                docAcp = getCoreSession().getACP(docRef);
            } catch (ClientException e) {
                log.error("Cannot get ACP from core...");
            } finally {
                logout();
            }
        }
        return docAcp;
    }

    public String getDocCurrentLifeCycleState() {
        if (docCurrentLifeCycle == null) {
            try {
                login();
                coreSession = getCoreSession();
                docCurrentLifeCycle = coreSession.getCurrentLifeCycleState(docRef);
            } catch (Exception e) {
                log.error("Cannot get life cycle from core...");
            } finally {
                logout();
            }
        }
        return docCurrentLifeCycle;
    }

    public String getDocUUID() {
        return docUUID;
    }

    public String computeId() {
        return getQid() + '-' + getName();
    }

    public String getDocVersionLabel() {
        return docVersionLabel;
    }

    public Boolean isDocVersion() {
        return isDocVersion;
    }

    public String getQid() {
        return getDocRepositoryName() + ':' + docUUID;
    }

    public String getDocName() {
        return docName;
    }

    public Boolean isDocProxy() {
        return isDocProxy;
    }

    public List<String> getDocFacets() {
        if (docFacets != null) {
            return docFacets;
        }
        return Collections.emptyList();
    }

    @Override
    public String toString() {
        return "DocumentIndexableResourceImpl<" + getName() + " for document "
                + docUUID + '>';
    }

    public Long getFlags() {
        return flags;
    }

}
