package org.nuxeo.ecm.webdav;

import org.junit.Test;

import javax.servlet.http.HttpServletRequest;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UtilTest {

    @Test
    public void getTokenFromHeadersReturnsRightToken() {
        String TOKEN = "tititoto2010";
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("If")).thenReturn("<urn:uuid:" + TOKEN + ">");

        String result = Util.getTokenFromHeaders("If", request);
        assertThat(result, is(TOKEN));
    }

}
