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

import org.nuxeo.ecm.core.lifecycle.LifeCycleConstants;
import org.nuxeo.ecm.core.schema.TypeConstants;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface NodeConstants {

    // ------------- namespaces --------------
    String NS_ECM_SYSTEM_URI = "http://www.nuxeo.org/ecm/system";
    String NS_ECM_SYSTEM_PREFIX = "ecm";
    String NS_ECM_SECURITY_URI = TypeConstants.SECURITY_SCHEMA_URI;
    String NS_ECM_SECURITY_PREFIX = TypeConstants.SECURITY_SCHEMA_PREFIX;
    String NS_ECM_LIFECYCLE_URI = LifeCycleConstants.LIFECYCLE_SCHEMA_URI;
    String NS_ECM_LIFECYLE_PREFIX = LifeCycleConstants.LIFECYCLE_SCHEMA_PREFIX;
    String NS_ECM_TYPES_URI = "http://nuxeo.org/ecm/jcr/types";
    String NS_ECM_TYPES_PREFIX = "ecmnt";
    String NS_ECM_MIXIN_URI = "http://nuxeo.org/ecm/jcr/mixin";
    String NS_ECM_MIXIN_PREFIX = "ecmmix";
    String NS_ECM_DOCS_URI = "http://nuxeo.org/ecm/jcr/docs";
    String NS_ECM_DOCS_PREFIX = "ecmdt";
    String NS_ECM_SCHEMAS_URI = "http://nuxeo.org/ecm/jcr/schemas";
    String NS_ECM_SCHEMAS_PREFIX = "ecmst";
    String NS_ECM_FIELDS_URI = "http://nuxeo.org/ecm/jcr/fields";
    String NS_ECM_FIELDS_PREFIX = "ecmft";

    String NS_ECM_VERSIONING_URI = "http://nuxeo.org/ecm/jcr/versioning";
    String NS_ECM_VERSIONING_PREFIX = "ecmver";

    // virtual property used only when indexing
    JCRName ECM_MIXIN_TYPE = new JCRName("mixinType", NS_ECM_SYSTEM_URI, NS_ECM_SYSTEM_PREFIX);
    JCRName ECM_PARENT_ID = new JCRName("parentId", NS_ECM_SYSTEM_URI, NS_ECM_SYSTEM_PREFIX);
    JCRName ECM_PATH = new JCRName("path", NS_ECM_SYSTEM_URI, NS_ECM_SYSTEM_PREFIX);
    JCRName ECM_ISPROXY = new JCRName("isProxy", NS_ECM_SYSTEM_URI, NS_ECM_SYSTEM_PREFIX);
    JCRName ECM_ISCHECKEDINVERSION = new JCRName("isCheckedInVersion", NS_ECM_SYSTEM_URI, NS_ECM_SYSTEM_PREFIX);

    // ------------- QNames ---------------------
    JCRName ECM_NT_BASE = new JCRName("base", NS_ECM_TYPES_URI, NS_ECM_TYPES_PREFIX);
    JCRName ECM_NT_CONTAINER = new JCRName("container", NS_ECM_TYPES_URI, NS_ECM_TYPES_PREFIX);
    JCRName ECM_NT_OCONTAINER = new JCRName("orderedContainer", NS_ECM_TYPES_URI, NS_ECM_TYPES_PREFIX);
    JCRName ECM_NT_DOCSEQ = new JCRName("docseq", NS_ECM_TYPES_URI, NS_ECM_TYPES_PREFIX);
    JCRName ECM_NT_DOCUMENT = new JCRName("document", NS_ECM_TYPES_URI, NS_ECM_TYPES_PREFIX);
    JCRName ECM_NT_FOLDER = new JCRName("folder", NS_ECM_TYPES_URI, NS_ECM_TYPES_PREFIX);
    JCRName ECM_NT_OFOLDER = new JCRName("orderedFolder", NS_ECM_TYPES_URI, NS_ECM_TYPES_PREFIX);
    JCRName ECM_NT_DOCUMENT_PROXY = new JCRName("documentProxy", NS_ECM_TYPES_URI, NS_ECM_TYPES_PREFIX);

    JCRName ECM_NT_PROPERTY = new JCRName("property", NS_ECM_TYPES_URI, NS_ECM_TYPES_PREFIX);
    JCRName ECM_NT_PROPERTY_BAG = new JCRName("bag", NS_ECM_TYPES_URI, NS_ECM_TYPES_PREFIX);
    JCRName ECM_NT_PROPERTY_LIST = new JCRName("list", NS_ECM_TYPES_URI, NS_ECM_TYPES_PREFIX);

    JCRName ECM_MIX_CONTENT = new JCRName("content", NS_ECM_MIXIN_URI, NS_ECM_MIXIN_PREFIX);
    JCRName ECM_NT_CONTENT = new JCRName("content", NS_ECM_FIELDS_URI, NS_ECM_FIELDS_PREFIX);

    JCRName ECM_NT_ACP = new JCRName("acp", NS_ECM_FIELDS_URI, NS_ECM_FIELDS_PREFIX);
    JCRName ECM_NT_ACL = new JCRName("acl", NS_ECM_FIELDS_URI, NS_ECM_FIELDS_PREFIX);
    JCRName ECM_NT_ACE = new JCRName("ace", NS_ECM_FIELDS_URI, NS_ECM_FIELDS_PREFIX);

    JCRName ECM_NT_LIFECYCLE_STATE = new JCRName(LifeCycleConstants.LIFECYCLE_STATE_PROP,
            NS_ECM_FIELDS_URI, NS_ECM_FIELDS_PREFIX);
    JCRName ECM_NT_LIFECYCLE_POLICY = new JCRName(LifeCycleConstants.LIFECYCLE_POLICY_PROP,
            NS_ECM_FIELDS_URI, NS_ECM_FIELDS_PREFIX);

    JCRName ECM_MIX_SCHEMA = new JCRName("schema", NS_ECM_MIXIN_URI, NS_ECM_MIXIN_PREFIX);
    JCRName ECM_MIX_FOLDER = new JCRName("folder", NS_ECM_MIXIN_URI, NS_ECM_MIXIN_PREFIX);
    JCRName ECM_MIX_ORDERED = new JCRName("ordered", NS_ECM_MIXIN_URI, NS_ECM_MIXIN_PREFIX);
    JCRName ECM_MIX_UNSTRUCTURED = new JCRName("unstructured", NS_ECM_MIXIN_URI, NS_ECM_MIXIN_PREFIX);

    // the system schema
    JCRName ECM_NT_SECURITY_SCHEMA = new JCRName(TypeConstants.SECURITY_SCHEMA_NAME,
            NS_ECM_SCHEMAS_URI, NS_ECM_SCHEMAS_PREFIX);
    JCRName ECM_NT_LIFECYCLE_SCHEMA = new JCRName(LifeCycleConstants.LIFECYCLE_SCHEMA_NAME,
            NS_ECM_SCHEMAS_URI, NS_ECM_SCHEMAS_PREFIX);
    JCRName ECM_NT_SYSTEM_SCHEMA = new JCRName("system",
            NS_ECM_SCHEMAS_URI, NS_ECM_SCHEMAS_PREFIX);
    JCRName ECM_NT_PROXY_SCHEMA = new JCRName("proxy",
            NS_ECM_SCHEMAS_URI, NS_ECM_SCHEMAS_PREFIX);

    JCRName ECM_ROOT = new JCRName("root", NS_ECM_SYSTEM_URI, NS_ECM_SYSTEM_PREFIX);
    String  ECM_ROOT_TYPE = "Root";
    JCRName ECM_CHILDREN = new JCRName("children", NS_ECM_SYSTEM_URI, NS_ECM_SYSTEM_PREFIX);
    JCRName ECM_ACP = new JCRName("acp", NS_ECM_SYSTEM_URI, NS_ECM_SYSTEM_PREFIX);
    JCRName ECM_OWNERS = new JCRName("owners", NS_ECM_SYSTEM_URI, NS_ECM_SYSTEM_PREFIX);
    JCRName ECM_PERMISSION = new JCRName("permissions", NS_ECM_SYSTEM_URI, NS_ECM_SYSTEM_PREFIX);
    JCRName ECM_PRINCIPAL = new JCRName("principals", NS_ECM_SYSTEM_URI, NS_ECM_SYSTEM_PREFIX);
    JCRName ECM_TYPE = new JCRName("type", NS_ECM_SYSTEM_URI, NS_ECM_SYSTEM_PREFIX);
    JCRName ECM_NAME = new JCRName("name", NS_ECM_SYSTEM_URI, NS_ECM_SYSTEM_PREFIX);
    JCRName ECM_LIFECYCLE_STATE = new JCRName(LifeCycleConstants.LIFECYCLE_STATE_PROP,
            NS_ECM_SYSTEM_URI, NS_ECM_SYSTEM_PREFIX);
    JCRName ECM_LIFECYCLE_POLICY = new JCRName(LifeCycleConstants.LIFECYCLE_POLICY_PROP,
            NS_ECM_SYSTEM_URI, NS_ECM_SYSTEM_PREFIX);

    JCRName ECM_LOCK = new JCRName("lock", NS_ECM_SYSTEM_URI, NS_ECM_SYSTEM_PREFIX);
    JCRName ECM_INDEXED = new JCRName("indexed", NS_ECM_SYSTEM_URI, NS_ECM_SYSTEM_PREFIX);
    JCRName ECM_DIRTY = new JCRName("dirty", NS_ECM_SYSTEM_URI, NS_ECM_SYSTEM_PREFIX);
    JCRName ECM_SYSTEM_ANY = new JCRName("any", NS_ECM_SYSTEM_URI, NS_ECM_SYSTEM_PREFIX);

    JCRName ECM_REF_FROZEN_NODE = new JCRName("refFrozenNode", NS_ECM_SYSTEM_URI, NS_ECM_SYSTEM_PREFIX);
    JCRName ECM_REF_UUID = new JCRName("refUUID", NS_ECM_SYSTEM_URI, NS_ECM_SYSTEM_PREFIX);

    JCRName ECM_LIST_KEYS = new JCRName("listKeys", NS_ECM_SYSTEM_URI, NS_ECM_SYSTEM_PREFIX);

    // versioning storage
    JCRName ECM_VERSION_STORAGE = new JCRName("versionStorage", NS_ECM_VERSIONING_URI, NS_ECM_VERSIONING_PREFIX);

    // versionableMixin
    JCRName ECM_VERSIONABLE_MIXIN = new JCRName("versionable", NS_ECM_MIXIN_URI, NS_ECM_MIXIN_PREFIX);

    JCRName ECM_NT_VERSION_HISTORY = ECM_NT_CONTAINER;
    JCRName ECM_VERSION_HISTORY = new JCRName("versionHistory", NS_ECM_SYSTEM_URI, NS_ECM_SYSTEM_PREFIX);

    JCRName ECM_VERSION_ID = new JCRName("versionId", NS_ECM_SYSTEM_URI, NS_ECM_SYSTEM_PREFIX);

    JCRName ECM_NT_VER_BASE_VERSION = ECM_NT_DOCUMENT;
    JCRName ECM_VER_BASE_VERSION = new JCRName("baseVersion", NS_ECM_SYSTEM_URI, NS_ECM_SYSTEM_PREFIX);

    JCRName ECM_VER_ISCHECKEDOUT = new JCRName("isCheckedOut", NS_ECM_SYSTEM_URI, NS_ECM_SYSTEM_PREFIX);

    JCRName ECM_NT_VERSION = ECM_NT_CONTAINER; //new JCRName("versions", NS_ECM_SYSTEM_URI, NS_ECM_SYSTEM_PREFIX);
    JCRName ECM_VERSION = new JCRName("version", NS_ECM_SYSTEM_URI, NS_ECM_SYSTEM_PREFIX);

    JCRName ECM_VERSION_FROZEN_UUID = new JCRName("frozenUuid", NS_ECM_SYSTEM_URI, NS_ECM_SYSTEM_PREFIX);
    JCRName ECM_VERSION_CREATEDATE = new JCRName("created", NS_ECM_SYSTEM_URI, NS_ECM_SYSTEM_PREFIX);
    JCRName ECM_VERSION_LABEL = new JCRName("label", NS_ECM_SYSTEM_URI, NS_ECM_SYSTEM_PREFIX);
    JCRName ECM_VERSION_DESCRIPTION = new JCRName("description", NS_ECM_SYSTEM_URI, NS_ECM_SYSTEM_PREFIX);
    JCRName ECM_VERSION_PREDECESSOR = new JCRName("predecessor", NS_ECM_SYSTEM_URI, NS_ECM_SYSTEM_PREFIX);
    JCRName ECM_VERSION_SUCCESSOR = new JCRName("successor", NS_ECM_SYSTEM_URI, NS_ECM_SYSTEM_PREFIX);

    JCRName ECM_FROZEN_NODE_UUID = new JCRName("frozenNode", NS_ECM_SYSTEM_URI, NS_ECM_SYSTEM_PREFIX);

    JCRName ECM_CONTENT_LENGTH = new JCRName("length", "", "");
    JCRName ECM_CONTENT_FILENAME = new JCRName("filename", "", "");
    JCRName ECM_CONTENT_DIGEST = new JCRName("digest", "", "");

}
