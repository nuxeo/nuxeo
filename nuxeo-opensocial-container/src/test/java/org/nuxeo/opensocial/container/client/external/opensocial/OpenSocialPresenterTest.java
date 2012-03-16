/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Stéphane Fourrier
 */

package org.nuxeo.opensocial.container.client.external.opensocial;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class OpenSocialPresenterTest {
    private String opensocialUrl = "url?container=test&nocache=test&country=test&lang=fr_fr&view=test&mid=test&parent=test&permission=test&url=test&up_defaultFolder=test&up_0=0&debug=0&st=test&rpctoken=test";

    @Test
    public void iCanChangeLangParam() {
        String newOpenSocialUrl = OpenSocialPresenter.changeParam(
                opensocialUrl, OpenSocialPresenter.OS_LANG_ATTRIBUTE, "uk");

        assertEquals(
                "url?container=test&nocache=test&country=test&lang=uk&view=test&mid=test&parent=test&permission=test&url=test&up_defaultFolder=test&up_0=0&debug=0&st=test&rpctoken=test",
                newOpenSocialUrl);
    }

    @Test
    public void iCanChangeViewParam() {
        String newOpenSocialUrl = OpenSocialPresenter.changeParam(
                opensocialUrl, OpenSocialPresenter.OS_VIEW_ATTRIBUTE, "canvas");

        assertEquals(
                "url?container=test&nocache=test&country=test&lang=fr_fr&view=canvas&mid=test&parent=test&permission=test&url=test&up_defaultFolder=test&up_0=0&debug=0&st=test&rpctoken=test",
                newOpenSocialUrl);
    }

    @Test
    public void iCanChangePermissionsParam() {
        String newOpenSocialUrl = OpenSocialPresenter.changeParam(
                opensocialUrl, OpenSocialPresenter.OS_PERMISSIONS_ATTRIBUTE, "[Everything]");

        assertEquals(
                "url?container=test&nocache=test&country=test&lang=fr_fr&view=test&mid=test&parent=test&permission=%5BEverything%5D&url=test&up_defaultFolder=test&up_0=0&debug=0&st=test&rpctoken=test",
                newOpenSocialUrl);
    }

}
