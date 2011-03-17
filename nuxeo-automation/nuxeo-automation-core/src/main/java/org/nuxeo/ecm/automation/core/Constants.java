/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class Constants {

    // Known input/output types
    public static final String O_DOCUMENT = "document";

    public static final String O_DOCUMENTS = "documents";

    public static final String O_BLOB = "blob";

    public static final String O_BLOBS = "blobs";

    // Parameter types
    public static final String T_STRING = "string"; // injectable as String

    public static final String T_BOOLEAN = "boolean"; // injectable as

    // Boolean

    public static final String T_DATE = "date"; // W3C string injectable as

    // Date

    public static final String T_INTEGER = "integer"; // integer number

    // injectable as Long

    public static final String T_FLOAT = "float"; // float number injectable

    // as Double

    public static final String T_RESOURCE = "resource"; // resource url

    // injectable as URL

    public static final String T_DOCUMENT = "document"; // Document injectable

    // as DocumentRef /
    // DocumentModel

    public static final String T_DOCUMENTS = "documents"; // Document

    public static final String T_BLOB = "blob";

    public static final String T_BLOBS = "bloblist";

    // injectable as
    // DocumentRefList
    // /
    // DocumentModelList

    public static final String T_SCRIPT = "script"; // inline MVEL injectable

    // script as Script

    public static final String T_PROPERTIES = "properties"; // Java properties

    // content
    // injectable as
    // Properties

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

    public static final String SEAM_CONTEXT = "Seam";

    public static final String WORKFLOW_CONTEXT = "Workflow";

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

    private Constants() {
    }

}
