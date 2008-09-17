
import org.nuxeo.theme.Manager
import org.nuxeo.theme.themes.ThemeManager
import org.nuxeo.theme.elements.ThemeElement;
import org.nuxeo.theme.elements.PageElement;
import org.nuxeo.theme.elements.ElementFactory;
import org.nuxeo.theme.formats.FormatFactory;
import org.nuxeo.theme.elements.ElementFormatter;
import org.nuxeo.theme.formats.Format;


name = Request.getParameter("name")

ThemeManager themeManager = Manager.getThemeManager()

if (themeManager.getThemeByName(name) != null) {
    return "";
}

// theme
ThemeElement theme = (ThemeElement) ElementFactory.create("theme");
theme.setName(name);
Format themeWidget = FormatFactory.create("widget");
themeWidget.setName("theme view");
themeManager.registerFormat(themeWidget);
ElementFormatter.setFormat(theme, themeWidget);

// default page
PageElement page = (PageElement) ElementFactory.create("page");
page.setName("default");
Format pageWidget = FormatFactory.create("widget");
themeManager.registerFormat(pageWidget);
pageWidget.setName("page frame");
Format pageLayout = FormatFactory.create("layout");
themeManager.registerFormat(pageLayout);
Format pageStyle = FormatFactory.create("style");
themeManager.registerFormat(pageStyle);
ElementFormatter.setFormat(page, pageWidget);
ElementFormatter.setFormat(page, pageStyle);
ElementFormatter.setFormat(page, pageLayout);

theme.addChild(page);

themeManager.registerTheme(theme);
return String.format("%s/%s", name, "default");

