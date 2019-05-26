/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.permissions;

import org.junit.Test;

/**
 * @author Razvan Caraghin
 */
public class TestSecurityDataConverter {

    // SecurityData securityData = null;
    // SecurityDataConverter converter = null;
    // SecurityDTO securityDto = null;
    //
    // @Before
    // protected void setUp() throws Exception {
    // super.setUp();
    //
    // securityDto = new SecurityDTOImpl();
    //
    // List<ClientAceDto> aces = new ArrayList<ClientAceDto>();
    // ClientAceDto ace = null;
    //
    //
    // ace = new ClientAceDto("user#1", "right#1", ACEType.GRANT);
    // aces.add(ace);
    // ace = new ClientAceDto("user#2", "right#2", ACEType.GRANT);
    // aces.add(ace);
    // ace = new ClientAceDto("user#1", "right#3", ACEType.GRANT);
    // aces.add(ace);
    // ace = new ClientAceDto("user#2", "right#4", ACEType.GRANT);
    // aces.add(ace);
    //
    // ace = new ClientAceDto("user#1", "right#5", ACEType.DENY);
    // aces.add(ace);
    // ace = new ClientAceDto("user#2", "right#9", ACEType.DENY);
    // aces.add(ace);
    // ace = new ClientAceDto("user#1", "right#8", ACEType.DENY);
    // aces.add(ace);
    // ace = new ClientAceDto("user#2", "right#4", ACEType.DENY);
    // aces.add(ace);
    // ace = new ClientAceDto("user#1", "right#4", ACEType.DENY);
    // aces.add(ace);
    // securityDto.setCurrentDocumentClientAces(aces);
    //
    // aces = new ArrayList<ClientAceDto>();
    // ace = new ClientAceDto("user#1", "right#1", ACEType.GRANT);
    // aces.add(ace);
    // ace = new ClientAceDto("user#2", "right#2", ACEType.GRANT);
    // aces.add(ace);
    // ace = new ClientAceDto("user#1", "right#3", ACEType.GRANT);
    // aces.add(ace);
    // ace = new ClientAceDto("user#2", "right#4", ACEType.GRANT);
    // aces.add(ace);
    //
    // ace = new ClientAceDto("user#1", "right#5", ACEType.DENY);
    // aces.add(ace);
    // ace = new ClientAceDto("user#2", "right#9", ACEType.DENY);
    // aces.add(ace);
    // ace = new ClientAceDto("user#1", "right#8", ACEType.DENY);
    // aces.add(ace);
    // ace = new ClientAceDto("user#2", "right#4", ACEType.DENY);
    // aces.add(ace);
    // ace = new ClientAceDto("user#1", "right#4", ACEType.DENY);
    // aces.add(ace);
    //
    // securityDto.setParentDocumentsClientAces(aces);
    //
    // converter = new SecurityDataConverter();
    // }
    //
    // @After
    // protected void tearDown() throws Exception {
    // super.tearDown();
    //
    // securityData = null;
    // converter = null;
    // securityDto = null;
    // }
    //
    // /**
    // * test the converters
    // *
    // */
    @Test
    public void testConversion() {
        // securityData = converter.convertSecurityDataByUsers(securityDto);
        //
        // assertTrue(securityData.getCurrentDocDeny().size() == 2);
        // assertNotNull(securityData.getCurrentDocDeny().get("user#1"));
        // assertTrue(securityData.getCurrentDocDeny().get("user#1").size() == 3);
        // assertTrue(securityData.getCurrentDocDeny().get("user#1").contains("right#5"));
        // assertTrue(securityData.getCurrentDocDeny().get("user#1").contains("right#8"));
        // assertTrue(securityData.getCurrentDocDeny().get("user#1").contains("right#4"));
        // assertNotNull(securityData.getCurrentDocDeny().get("user#2"));
        // assertTrue(securityData.getCurrentDocDeny().get("user#2").size() == 2);
        // assertTrue(securityData.getCurrentDocDeny().get("user#2").contains("right#9"));
        // assertTrue(securityData.getCurrentDocDeny().get("user#2").contains("right#4"));
        //
        //
        // assertTrue(securityData.getCurrentDocGrant().size() == 2);
        // assertNotNull(securityData.getCurrentDocGrant().get("user#1"));
        // assertTrue(securityData.getCurrentDocGrant().get("user#1").size() == 2);
        // assertTrue(securityData.getCurrentDocGrant().get("user#1").contains("right#1"));
        // assertTrue(securityData.getCurrentDocGrant().get("user#1").contains("right#3"));
        // assertNotNull(securityData.getCurrentDocGrant().get("user#2"));
        // assertTrue(securityData.getCurrentDocGrant().get("user#2").size() == 2);
        // assertTrue(securityData.getCurrentDocGrant().get("user#2").contains("right#2"));
        // assertTrue(securityData.getCurrentDocGrant().get("user#2").contains("right#4"));
        //
        //
        // assertTrue(securityData.getParentDocsDeny().size() == 2);
        // assertNotNull(securityData.getParentDocsDeny().get("user#1"));
        // assertTrue(securityData.getParentDocsDeny().get("user#1").size() == 3);
        // assertTrue(securityData.getParentDocsDeny().get("user#1").contains("right#5"));
        // assertTrue(securityData.getParentDocsDeny().get("user#1").contains("right#8"));
        // assertTrue(securityData.getParentDocsDeny().get("user#1").contains("right#4"));
        // assertNotNull(securityData.getParentDocsDeny().get("user#2"));
        // assertTrue(securityData.getParentDocsDeny().get("user#2").size() == 2);
        // assertTrue(securityData.getParentDocsDeny().get("user#2").contains("right#9"));
        // assertTrue(securityData.getParentDocsDeny().get("user#2").contains("right#4"));
        //
        //
        // assertTrue(securityData.getParentDocsGrant().size() == 2);
        // assertNotNull(securityData.getParentDocsGrant().get("user#1"));
        // assertTrue(securityData.getParentDocsGrant().get("user#1").size() == 2);
        // assertTrue(securityData.getParentDocsGrant().get("user#1").contains("right#1"));
        // assertTrue(securityData.getParentDocsGrant().get("user#1").contains("right#3"));
        // assertNotNull(securityData.getParentDocsGrant().get("user#2"));
        // assertTrue(securityData.getParentDocsGrant().get("user#2").size() == 2);
        // assertTrue(securityData.getParentDocsGrant().get("user#2").contains("right#2"));
        // assertTrue(securityData.getParentDocsGrant().get("user#2").contains("right#4"));
    }
}
