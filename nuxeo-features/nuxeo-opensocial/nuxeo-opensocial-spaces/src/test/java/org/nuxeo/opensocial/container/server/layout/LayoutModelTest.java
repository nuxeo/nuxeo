package org.nuxeo.opensocial.container.server.layout;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Iterator;

import org.junit.Test;
import org.nuxeo.opensocial.container.shared.layout.api.YUIComponent;
import org.nuxeo.opensocial.container.shared.layout.api.YUILayout;
import org.nuxeo.opensocial.container.shared.layout.enume.YUISideBarStyle;
import org.nuxeo.opensocial.container.shared.layout.enume.YUISize;
import org.nuxeo.opensocial.container.shared.layout.enume.YUITemplate;
import org.nuxeo.opensocial.container.shared.layout.impl.YUIAbstractBodySize;
import org.nuxeo.opensocial.container.shared.layout.impl.YUIComponentZoneImpl;
import org.nuxeo.opensocial.container.shared.layout.impl.YUICustomBodySize;
import org.nuxeo.opensocial.container.shared.layout.impl.YUIFixedBodySize;
import org.nuxeo.opensocial.container.shared.layout.impl.YUILayoutFactory;
import org.nuxeo.opensocial.container.shared.layout.impl.YUIUnitImpl;

public class LayoutModelTest {
    @Test
    public void iCanCreateACustomLayout() throws IOException {
        // Le layout que l'on souhaite réalisé est le suivant :
        // ___________________________________________
        // | _______________________________________ |
        // | | | |
        // | | 1 | |
        // | | | |
        // | |_______________________________________| |
        // | ___________________ __________________ |
        // | | || | |
        // | | 2 || 3 | |
        // | | || | |
        // | |___________________||__________________| |
        // |___________________________________________|

        // Création d'un Layout avec taille fixe
        YUIAbstractBodySize size = new YUIFixedBodySize(YUISize.YUI_BS_750_PX);
        YUISideBarStyle sideBar = YUISideBarStyle.YUI_SB_LEFT_160PX;

        YUILayout layout = YUILayoutFactory.createLayout(size, true, true,
                sideBar);
        // Test de validation de la taille du body
        assertEquals(layout.getBodySize().getCSS(), "doc");
        assertEquals(layout.getBodySize().getSize(), 750);

        // Création d'un Layout avec taille donnée
        YUIAbstractBodySize size2 = new YUICustomBodySize(40);
        YUILayout layout2 = YUILayoutFactory.createLayout(size2, true, true,
                sideBar);
        // Test de validation de la taille du body
        layout2.setBodySize(new YUICustomBodySize(40));
        assertEquals(layout2.getBodySize().getCSS(), "custom-doc");
        assertEquals(layout2.getBodySize().getSize(), 40);

        // Ajout d'une premiere zone au layout 1
        YUIComponent zone1 = new YUIComponentZoneImpl(YUITemplate.YUI_ZT_100);
        layout.getContent().addComponent(zone1);
        // Ajout d'une seconde zone au layout 2
        YUIComponent zone2 = new YUIComponentZoneImpl(YUITemplate.YUI_ZT_25_75);
        layout.getContent().addComponent(zone2);
        // Récupération des composants
        Iterator<YUIComponent> itr = layout.getContent().getComponents().iterator();

        assertEquals(((YUIComponentZoneImpl) zone1).getCSS(), "");
        assertEquals(
                ((YUIComponentZoneImpl) zone1).getTemplate().getNumberOfComponents(),
                1);
        assertEquals((YUIComponent) itr.next(), zone1);
        assertEquals(((YUIComponentZoneImpl) zone2).getCSS(), "yui-gf");
        assertEquals(
                ((YUIComponentZoneImpl) zone2).getTemplate().getNumberOfComponents(),
                2);
        assertEquals((YUIComponent) itr.next(), zone2);

        // Ajout d'une unité à la zone 1
        YUIComponent unite1 = new YUIUnitImpl();
        ((YUIComponentZoneImpl) zone1).addComponent(unite1);

        // Ajout deux deux unités à la zone 2
        YUIComponent unite2 = new YUIUnitImpl();
        ((YUIComponentZoneImpl) zone2).addComponent(unite2);
        YUIComponent unite3 = new YUIUnitImpl();
        ((YUIComponentZoneImpl) zone2).addComponent(unite3);
    }

    // public void createHTMLFile(YUILayout layout, String fileName) throws
    // IOException
    // {
    // PrintWriter HTMLFile;
    //
    // HTMLFile = new PrintWriter(new BufferedWriter
    // (new FileWriter(fileName + ".html")));
    //
    // HTMLFile.println("<html>");
    // HTMLFile.println("  <head>");
    // HTMLFile.println("    <link rel=\"stylesheet\" type=\"text/css\" href=\"http://yui.yahooapis.com/2.8.1/build/reset-fonts-grids/reset-fonts-grids.css\">");
    // HTMLFile.println("  </head>");
    // HTMLFile.println("  <body>");
    // HTMLFile.println(new YUILayoutHtmlAdapter(layout).toHtml());
    // HTMLFile.println("  </body>");
    // HTMLFile.println("</html>");
    // HTMLFile.close();
    //
    // }
}
