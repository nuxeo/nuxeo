/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.gwt.client.annotea;

/**
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
 */
public interface RDFConstant {

    String R = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";

    String A = "http://www.w3.org/2000/10/annotation-ns#";

    String D = "http://purl.org/dc/elements/1.1/";

    String H = "http://www.w3.org/1999/xx/http#";

    String R_ABOUT = "{" + R + "}about";

    String R_RESOURCE = "{" + R + "}resource";

    String R_TYPE = "{" + R + "}type";

    String D_TITLE = "{" + D + "}title";

    String D_CREATOR = "{" + D + "}creator";

    String D_DATE = "{" + D + "}date";

    String A_CONTEXT = "{" + A + "}context";

    String A_BODY = "{" + A + "}body";

    String H_BODY = "{" + H + "}Body";

}
