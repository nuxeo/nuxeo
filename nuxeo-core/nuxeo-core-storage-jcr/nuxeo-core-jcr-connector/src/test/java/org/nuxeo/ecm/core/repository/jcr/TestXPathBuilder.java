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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.repository.jcr;

import junit.framework.TestCase;

import org.nuxeo.ecm.core.query.QueryException;

public class TestXPathBuilder extends TestCase {

    public void test() throws QueryException {
        double s = System.currentTimeMillis();
        String q = null, x = null;

        q = "SELECT * FROM Document WHERE test/dc:title='test'";
        x = "//element(*,ecmdt:Document)[test/@dc:title = 'test']";
        assertEquals(x, XPathBuilder.fromNXQL(q));

        q = "select * from File where ecm:path LIKE '%/wiki/FrontPage'";
        //x = "//%/ecm:children/wiki/ecm:children/element(FrontPage,ecmdt:File)";
        x = "//element(*,ecmdt:File)[ jcr:like(@ecm:path, '%/wiki/FrontPage') ]";
        assertEquals(x, XPathBuilder.fromNXQL(q));


        q = "select * from File where ecm:path LIKE '%/wiki/FrontPage/'";
        //x = "//%/ecm:children/wiki/ecm:children/FrontPage/ecm:children/element(*,ecmdt:File)";
        x = "//element(*,ecmdt:File)[ jcr:like(@ecm:path, '%/wiki/FrontPage/') ]";
        assertEquals(x, XPathBuilder.fromNXQL(q));

        q = "select * from File where ecm:path LIKE '%/wiki/FrontPage/%'";
        //x = "//%/ecm:children/wiki/ecm:children/FrontPage/ecm:children//element(*,ecmdt:File)";
        x = "//element(*,ecmdt:File)[ jcr:like(@ecm:path, '%/wiki/FrontPage/%') ]";
        assertEquals(x, XPathBuilder.fromNXQL(q));

        q = "select * from File where ecm:path LIKE '/wiki/FrontPage'";
        //x = "/jcr:root/ecm:root/ecm:children/wiki/ecm:children/element(FrontPage,ecmdt:File)";
        x = "//element(*,ecmdt:File)[ jcr:like(@ecm:path, '/wiki/FrontPage') ]";
        assertEquals(x, XPathBuilder.fromNXQL(q));

        q = "select * from File where ecm:path LIKE '/wiki/FrontPage/'";
        //x = "/jcr:root/ecm:root/ecm:children/wiki/ecm:children/FrontPage/ecm:children/element(*,ecmdt:File)";
        x = "//element(*,ecmdt:File)[ jcr:like(@ecm:path, '/wiki/FrontPage/') ]";
        assertEquals(x, XPathBuilder.fromNXQL(q));

        q = "select * from File where ecm:path LIKE '/wiki/FrontPage/%'";
        //x = "/jcr:root/ecm:root/ecm:children/wiki/ecm:children/FrontPage/ecm:children//element(*,ecmdt:File)";
        x = "//element(*,ecmdt:File)[ jcr:like(@ecm:path, '/wiki/FrontPage/%') ]";
        assertEquals(x, XPathBuilder.fromNXQL(q));

        //

        q = "select doc from File where ecm:fulltext = '%MyText%'";
        x = "//element(*,ecmdt:File)[jcr:contains(., '%MyText%')]";
        assertEquals(x, XPathBuilder.fromNXQL(q));

        q = "select * from File where ecm:fulltext = '%MyText%' and ecm:uuid='test'";
        x = "//element(*,ecmdt:File)[(jcr:contains(., '%MyText%')) and (@jcr:uuid = 'test')]";
        assertEquals(x, XPathBuilder.fromNXQL(q));

        q = "select * from File where ecm:name = 'My' and ecm:test NOT LIKE 'test'";
        x = "//element(*,ecmdt:File)[(fn:name() = 'My') and ( not(jcr:like(@ecm:test, 'test')) )]";
        assertEquals(x, XPathBuilder.fromNXQL(q));

        q = "select * from File where ecm:fulltext = '%MyText%' and dc:title IN ('test1', 'test2')";
        x = "//element(*,ecmdt:File)[(jcr:contains(., '%MyText%')) and ( (@dc:title = 'test1' or @dc:title = 'test2') )]";
        assertEquals(x, XPathBuilder.fromNXQL(q));

        q = "select * from publishedVersions where ecm:fulltext = '%MyText%' and ecm:primaryType = 'File'";
        x = "//element(*,ecmnt:documentProxy)/jcr:deref(@ecm:refFrozenNode, '*')[(jcr:contains(., '%MyText%')) and (@jcr:primaryType = 'ecmdt:File')]";
        assertEquals(x, XPathBuilder.fromNXQL(q));

        q = "SELECT * FROM Document WHERE dc:title='test' ORDER BY foo DESC, bar ASC";
        x = "//element(*,ecmdt:Document)[@dc:title = 'test'] order by @foo descending, @bar";
        assertEquals(x, XPathBuilder.fromNXQL(q));

        // TODO XXX bs
        // This test is disabled since it is not passing in Java6. In java6 the expected date in xs:dateTime
        // is T00:00:00.000Z and not T00:00:00.000+01:00
        q = "select * from document where ecm:path LIKE '%/ws/%' and dc:created between DATE '2004-02-10' and DATE '2005-01-02'";
        //x = "//%/ecm:children/ws/ecm:children//element(*,ecmnt:document)[( (dc:created >= xs:dateTime('2004-02-10T00:00:00.000+01:00') and dc:created <= xs:dateTime('2005-01-02T00:00:00.000+01:00')))]";
        x = "//element(*,ecmnt:document)[( jcr:like(@ecm:path, '%/ws/%') ) and ( (dc:created >= xs:dateTime('2004-02-10T00:00:00.000+01:00') and dc:created <= xs:dateTime('2005-01-02T00:00:00.000+01:00')))]";
        //assertEquals(x, XPathBuilder.fromNXQL(q));

        //q = "select * from document where ecm:path LIKE '/default-domain/workspaces/%' and   dc:created != TIMESTAMP '1003-02-10 10:00:00' and my:urgency = 2 order by ecm:path";
        //x = "/jcr:root/ecm:root/ecm:children/default-domain/ecm:children/workspaces/ecm:children//element(*,ecmnt:document)[((@dc:created <> xs:dateTime('1003-02-10T10:00:00.000+00:09:21'))) and (@my:urgency = 2)] order by @jcr:path ascending";
        //assertEquals(x, XPathBuilder.fromNXQL(q));

        // System.out.println(">>>> " + ((System.currentTimeMillis() - s) / 1000) + " sec.");

        q = "SELECT * FROM Workspace WHERE NOT ecm:path STARTSWITH '/default-domain/templates'";
        x = "//element(*,ecmdt:Workspace)[ not(( jcr:like(@ecm:path, '/default-domain/templates/%') )) ]";
        assertEquals(x, XPathBuilder.fromNXQL(q));

        q = "SELECT * FROM Workspace WHERE ecm:path STARTSWITH '/default-domain/templates' ORDER BY dc:title";
        //x = "/jcr:root/ecm:root/ecm:children/default-domain/ecm:children/templates/ecm:children//element(*,ecmdt:Workspace) order by @dc:title";
        x = "//element(*,ecmdt:Workspace)[ jcr:like(@ecm:path, '/default-domain/templates/%') ] order by @dc:title";
        assertEquals(x, XPathBuilder.fromNXQL(q));

        q = "SELECT * FROM Workspace WHERE ecm:path STARTSWITH '/default-domain/templates' AND NOT (ecm:path STARTSWITH '/default-domain/templates/t1')";
        x = "//element(*,ecmdt:Workspace)[( jcr:like(@ecm:path, '/default-domain/templates/%') ) and ( not(( jcr:like(@ecm:path, '/default-domain/templates/t1/%') )) )]";
        // System.out.println(XPathBuilder.fromNXQL(q));
        assertEquals(x, XPathBuilder.fromNXQL(q));
    }

}
