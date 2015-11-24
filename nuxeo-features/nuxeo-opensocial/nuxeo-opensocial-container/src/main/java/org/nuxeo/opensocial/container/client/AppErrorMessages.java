package org.nuxeo.opensocial.container.client;

import com.google.gwt.i18n.client.Messages;

/**
 * @author Stéphane Fourrier
 */
public interface AppErrorMessages extends Messages {
    String unitIsNotEmpty();

    String zoneIsNotEmpty();

    String noZoneCreated();

    String cannotLoadLayout();

    String cannotReachServer();

    String applicationNotCorrectlySet();

    String cannotUpdateLayout();

    String cannotUpdateFooter();

    String cannotCreateZone();

    String cannotUpdateZone();

    String cannotUpdateSideBar();

    String cannotUpdateHeader();

    String cannotDeleteZone();

    String cannotCreateWebContent();

    String cannotLoadWebContents();

    String cannotUpdateAllWebContents();

    String cannotUpdateWebContent();

    String cannotDeleteWebContent();

    String cannotLoadContainerBuilder();

    String cannotAddExternalWebContent(String type);

    String cannotFindWebContent();

    String preferenceDoesNotExist(String name);
}
