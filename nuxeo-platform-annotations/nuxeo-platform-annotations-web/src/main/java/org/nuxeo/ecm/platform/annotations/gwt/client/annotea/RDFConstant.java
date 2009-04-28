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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.gwt.client.annotea;

/**
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
 *
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
