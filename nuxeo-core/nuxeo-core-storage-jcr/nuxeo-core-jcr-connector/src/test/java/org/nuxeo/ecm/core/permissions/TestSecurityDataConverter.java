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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.core.permissions;


import junit.framework.TestCase;

/**
 *
 * @author Razvan Caraghin
 *
 */
public class TestSecurityDataConverter extends TestCase {

//    SecurityData securityData = null;
//    SecurityDataConverter converter = null;
//    SecurityDTO securityDto = null;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

//        securityDto = new SecurityDTOImpl();
//
//        List<ClientAceDto> aces = new ArrayList<ClientAceDto>();
//        ClientAceDto ace = null;
//
//
//        ace = new ClientAceDto("user#1", "right#1", ACEType.GRANT);
//        aces.add(ace);
//        ace = new ClientAceDto("user#2", "right#2", ACEType.GRANT);
//        aces.add(ace);
//        ace = new ClientAceDto("user#1", "right#3", ACEType.GRANT);
//        aces.add(ace);
//        ace = new ClientAceDto("user#2", "right#4", ACEType.GRANT);
//        aces.add(ace);
//
//        ace = new ClientAceDto("user#1", "right#5", ACEType.DENY);
//        aces.add(ace);
//        ace = new ClientAceDto("user#2", "right#9", ACEType.DENY);
//        aces.add(ace);
//        ace = new ClientAceDto("user#1", "right#8", ACEType.DENY);
//        aces.add(ace);
//        ace = new ClientAceDto("user#2", "right#4", ACEType.DENY);
//        aces.add(ace);
//        ace = new ClientAceDto("user#1", "right#4", ACEType.DENY);
//        aces.add(ace);
//        securityDto.setCurrentDocumentClientAces(aces);
//
//        aces = new ArrayList<ClientAceDto>();
//        ace = new ClientAceDto("user#1", "right#1", ACEType.GRANT);
//        aces.add(ace);
//        ace = new ClientAceDto("user#2", "right#2", ACEType.GRANT);
//        aces.add(ace);
//        ace = new ClientAceDto("user#1", "right#3", ACEType.GRANT);
//        aces.add(ace);
//        ace = new ClientAceDto("user#2", "right#4", ACEType.GRANT);
//        aces.add(ace);
//
//        ace = new ClientAceDto("user#1", "right#5", ACEType.DENY);
//        aces.add(ace);
//        ace = new ClientAceDto("user#2", "right#9", ACEType.DENY);
//        aces.add(ace);
//        ace = new ClientAceDto("user#1", "right#8", ACEType.DENY);
//        aces.add(ace);
//        ace = new ClientAceDto("user#2", "right#4", ACEType.DENY);
//        aces.add(ace);
//        ace = new ClientAceDto("user#1", "right#4", ACEType.DENY);
//        aces.add(ace);
//
//        securityDto.setParentDocumentsClientAces(aces);
//        securityData = new SecurityData();
//
//        converter = new SecurityDataConverter();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

//        securityData = null;
//        converter = null;
//        securityDto = null;
    }

    /**
     * test the converters.
     */
    // XXX: do something
    public void testConversion() {
//        converter.convertSecurityDataByUsers(securityDto, securityData);
//
//        assertTrue(securityData.getCurrentDocDeny().size() == 2);
//        assertNotNull(securityData.getCurrentDocDeny().get("user#1"));
//        assertTrue(securityData.getCurrentDocDeny().get("user#1").size() == 3);
//        assertTrue(securityData.getCurrentDocDeny().get("user#1").contains("right#5"));
//        assertTrue(securityData.getCurrentDocDeny().get("user#1").contains("right#8"));
//        assertTrue(securityData.getCurrentDocDeny().get("user#1").contains("right#4"));
//        assertNotNull(securityData.getCurrentDocDeny().get("user#2"));
//        assertTrue(securityData.getCurrentDocDeny().get("user#2").size() == 2);
//        assertTrue(securityData.getCurrentDocDeny().get("user#2").contains("right#9"));
//        assertTrue(securityData.getCurrentDocDeny().get("user#2").contains("right#4"));
//
//
//        assertTrue(securityData.getCurrentDocGrant().size() == 2);
//        assertNotNull(securityData.getCurrentDocGrant().get("user#1"));
//        assertTrue(securityData.getCurrentDocGrant().get("user#1").size() == 2);
//        assertTrue(securityData.getCurrentDocGrant().get("user#1").contains("right#1"));
//        assertTrue(securityData.getCurrentDocGrant().get("user#1").contains("right#3"));
//        assertNotNull(securityData.getCurrentDocGrant().get("user#2"));
//        assertTrue(securityData.getCurrentDocGrant().get("user#2").size() == 2);
//        assertTrue(securityData.getCurrentDocGrant().get("user#2").contains("right#2"));
//        assertTrue(securityData.getCurrentDocGrant().get("user#2").contains("right#4"));
//
//
//        assertTrue(securityData.getParentDocsDeny().size() == 2);
//        assertNotNull(securityData.getParentDocsDeny().get("user#1"));
//        assertTrue(securityData.getParentDocsDeny().get("user#1").size() == 3);
//        assertTrue(securityData.getParentDocsDeny().get("user#1").contains("right#5"));
//        assertTrue(securityData.getParentDocsDeny().get("user#1").contains("right#8"));
//        assertTrue(securityData.getParentDocsDeny().get("user#1").contains("right#4"));
//        assertNotNull(securityData.getParentDocsDeny().get("user#2"));
//        assertTrue(securityData.getParentDocsDeny().get("user#2").size() == 2);
//        assertTrue(securityData.getParentDocsDeny().get("user#2").contains("right#9"));
//        assertTrue(securityData.getParentDocsDeny().get("user#2").contains("right#4"));
//
//
//        assertTrue(securityData.getParentDocsGrant().size() == 2);
//        assertNotNull(securityData.getParentDocsGrant().get("user#1"));
//        assertTrue(securityData.getParentDocsGrant().get("user#1").size() == 2);
//        assertTrue(securityData.getParentDocsGrant().get("user#1").contains("right#1"));
//        assertTrue(securityData.getParentDocsGrant().get("user#1").contains("right#3"));
//        assertNotNull(securityData.getParentDocsGrant().get("user#2"));
//        assertTrue(securityData.getParentDocsGrant().get("user#2").size() == 2);
//        assertTrue(securityData.getParentDocsGrant().get("user#2").contains("right#2"));
//        assertTrue(securityData.getParentDocsGrant().get("user#2").contains("right#4"));
    }
}
