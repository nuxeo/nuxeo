package org.nuxeo.opensocial.container.factory.utils;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.nuxeo.opensocial.container.client.bean.PreferencesBean;

public class UrlBuilderTest {

  @Test
  public void iCanGetUserPrefsForUrl() throws Exception {
    List<PreferencesBean> prefs = new ArrayList<PreferencesBean>();
    prefs.add(new PreferencesBean("dataType", "defaultValue", "displayName",
        null, "p1", "val1"));
    prefs.add(new PreferencesBean("dataType", "defaultValue2", "displayName",
        null, "p2", null));
    String userPrefs = UrlBuilder.getUserPrefs(prefs);

    String result = "&up_p1=val1&up_p2=defaultValue2";
    assertEquals(userPrefs, result);

  }
}
