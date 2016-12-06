/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class Constants {

    /**
     * A chain id prefix used in mysterious situations (old rest api it seems).
     * <p>
     * Hardcoded here to better track usage now that chains are registered as operations on the service?
     *
     * @since 5.9.4
     */
    public static final String CHAIN_ID_PREFIX = "Chain.";

    // Known input/output types

    public static final String O_DOCUMENT = "document";

    public static final String O_DOCUMENTS = "documents";

    public static final String O_BLOB = "blob";

    public static final String O_BLOBS = "blobs";

    // Parameter types

    // injectable as String
    public static final String T_STRING = "string";

    // injectable as Boolean
    public static final String T_BOOLEAN = "boolean";

    // W3C string injectable as Date
    public static final String T_DATE = "date";

    // injectable as Integer
    public static final String T_INTEGER = "integer";

    // injectable as Long
    public static final String T_LONG = "long";

    // injectable as Double
    public static final String T_FLOAT = "float";

    // injectable as URL
    public static final String T_RESOURCE = "resource";

    // injectable as DocumentRef or DocumentModel
    public static final String T_DOCUMENT = "document";

    // injectable as DocumentRefList / DocumentModelList
    public static final String T_DOCUMENTS = "documents";

    public static final String T_BLOB = "blob";

    public static final String T_BLOBS = "bloblist";

    // inline MVEL injectable script as Script
    public static final String T_SCRIPT = "script";

    // Java properties content injectable as Properties
    public static final String T_PROPERTIES = "properties";

    // Category names

    public static final String CAT_FETCH = "Fetch";

    public static final String CAT_SCRIPTING = "Scripting";

    public static final String CAT_EXECUTION = "Execution Context";

    public static final String CAT_EXECUTION_STACK = "Push & Pop";

    public static final String CAT_SUBCHAIN_EXECUTION = "Execution Flow";

    public static final String CAT_DOCUMENT = "Document";

    public static final String CAT_BLOB = "Files";

    public static final String CAT_NOTIFICATION = "Notification";

    public static final String CAT_SERVICES = "Services";

    public static final String CAT_CONVERSION = "Conversion";

    public static final String CAT_USERS_GROUPS = "Users & Groups";

    public static final String CAT_UI = "User Interface";

    public static final String CAT_LOCAL_CONFIGURATION = "Local Configuration";

    public static final String CAT_WORKFLOW = "Workflow Context";

    public static final String SEAM_CONTEXT = "Seam";

    public static final String WORKFLOW_CONTEXT = "Workflow";

    public static final String CAT_BUSINESS = "Business";

    /**
     * @since 5.9.4
     */
    public static final String CAT_CHAIN = "Chain";

    // Widget types

    // the default Widget for String or any other unknown type
    public static final String W_TEXT = "Text";

    public static final String W_MULTILINE_TEXT = "TextArea";

    public static final String W_MAIL_TEMPLATE = "MailTemplate";

    public static final String W_TEMPLATE_RESOURCE = "TemplateResource";

    // to edit a properties table
    public static final String W_PROPERTIES = "Properties";

    // the default widget for Boolean
    public static final String W_CHECK = "Check";

    public static final String W_RADIO = "Radio";

    // a single selection listbox
    public static final String W_OPTION = "Option";

    public static final String W_LIST = "List";

    public static final String W_COMBO = "Combo";

    // Default widget for Date
    public static final String W_DATE = "Date";

    // Default widget for Long
    public static final String W_DECIMAL = "Decimal";

    // Default widget for Double
    public static final String W_NUMBER = "Number";

    public static final String W_AUDIT_EVENT = "AuditEvent";

    // key for setting workflow variables on the operation context
    public static final String VAR_WORKFLOW = "WorkflowVariables";

    // key for setting workflow node variables on the operation context
    public static final String VAR_WORKFLOW_NODE = "NodeVariables";

    // key for setting chain runtime variables on the operation context
    public static final String VAR_RUNTIME_CHAIN = "ChainParameters";


    private Constants() {
    }

}
