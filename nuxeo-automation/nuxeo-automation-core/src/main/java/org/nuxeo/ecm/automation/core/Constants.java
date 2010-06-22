/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface Constants {

    // Known input/output types
    public final static String O_DOCUMENT = "document";

    public final static String O_DOCUMENTS = "documents";

    public final static String O_BLOB = "blob";

    public final static String O_BLOBS = "blobs";

    // Parameter types
    public final static String T_STRING = "string"; // injectable as String

    public final static String T_BOOLEAN = "boolean"; // injectable as

    // Boolean

    public final static String T_DATE = "date"; // W3C string injectable as

    // Date

    public final static String T_INTEGER = "integer"; // integer number

    // injectable as Long

    public final static String T_FLOAT = "float"; // float number injectable

    // as Double

    public final static String T_RESOURCE = "resource"; // resource url

    // injectable as URL

    public final static String T_DOCUMENT = "document"; // Document injectable

    // as DocumentRef /
    // DocumentModel

    public final static String T_DOCUMENTS = "documents"; // Document

    // injectable as
    // DocumentRefList
    // /
    // DocumentModelList

    public final static String T_SCRIPT = "script"; // inline mvel injectable

    // script as Script

    public final static String T_PROPERTIES = "properties"; // Java properties

    // content
    // injectable as
    // Properties

    // Category names
    public final static String CAT_FETCH = "Fetch";

    public final static String CAT_SCRIPTING = "Scripting";

    public final static String CAT_EXECUTION = "Context";

    public final static String CAT_EXECUTION_STACK = "Push & Pop";

    public final static String CAT_SUBCHAIN_EXECUTION = "Chain Execution";

    public final static String CAT_DOCUMENT = "Document";

    public final static String CAT_BLOB = "Files";

    public final static String CAT_NOTIFICATION = "Notification";

    public final static String CAT_SERVICES = "Services";

    public final static String CAT_USERS_GROUPS = "Users & Groups";

    public final static String CAT_UI = "User Interface";

    public final static String SEAM_CONTEXT = "Seam";

    public final static String WORKFLOW_CONTEXT = "Workflow";

    // Widget types
    public final static String W_TEXT = "Text"; // the default Widget for

    // String or any other unknown
    // type

    public final static String W_MULTILINE_TEXT = "TextArea";

    public final static String W_PROPERTIES = "Properties"; // to edit a

    // properties table

    public final static String W_CHECK = "Check"; // the default widget for

    // Boolean

    public final static String W_RADIO = "Radio";

    public final static String W_OPTION = "Option"; // a single selection

    // listbox

    public final static String W_LIST = "List";

    public final static String W_COMBO = "Combo";

    public final static String W_DATE = "Date"; // Default widget for Date

    public final static String W_DECIMAL = "Decimal"; // Default widget for

    // Long

    public final static String W_NUMBER = "Number"; // Default widget for

    // Double

    public static final String W_AUDIT_EVENT = "AuditEvent";
}
