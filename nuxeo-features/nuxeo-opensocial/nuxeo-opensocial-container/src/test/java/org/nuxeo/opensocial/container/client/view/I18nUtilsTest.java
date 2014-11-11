package org.nuxeo.opensocial.container.client.view;

import static org.junit.Assert.*;

import org.junit.Test;

public class I18nUtilsTest {


    @Test
    public void extractI18nLabels() throws Exception {
        String pref = "__MSG_title__";
        String pref2 = "KML";

        assertTrue(I18nUtils.isI18nLabel(pref));
        assertFalse(I18nUtils.isI18nLabel(pref2));

        assertEquals("title", I18nUtils.getI18nKey(pref));

    }
}
