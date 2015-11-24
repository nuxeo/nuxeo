package org.nuxeo.opensocial.container.client.external.opensocial;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class OpenSocialPresenterTest {
    private String opensocialUrl = "url?container=test&nocache=test&country=test&lang=fr_fr&view=test&mid=test&parent=test&permission=test&url=test&up_defaultFolder=test&up_0&debug=0&st=test&rpctoken=test";

    @Test
    public void iCanChangeLangParam() {
        String newOpenSocialUrl = OpenSocialPresenter.changeParam(
                opensocialUrl, OpenSocialPresenter.OS_LANG_ATTRIBUTE, "uk");

        assertEquals(
                "url?container=test&nocache=test&country=test&lang=uk&view=test&mid=test&parent=test&permission=test&url=test&up_defaultFolder=test&up_0&debug=0&st=test&rpctoken=test",
                newOpenSocialUrl);
    }

    @Test
    public void iCanChangeViewParam() {
        String newOpenSocialUrl = OpenSocialPresenter.changeParam(
                opensocialUrl, OpenSocialPresenter.OS_VIEW_ATTRIBUTE, "canvas");

        assertEquals(
                "url?container=test&nocache=test&country=test&lang=fr_fr&view=canvas&mid=test&parent=test&permission=test&url=test&up_defaultFolder=test&up_0&debug=0&st=test&rpctoken=test",
                newOpenSocialUrl);
    }
}
