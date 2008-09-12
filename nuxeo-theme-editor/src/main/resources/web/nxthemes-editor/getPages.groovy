
import org.nuxeo.theme.Manager
import org.nuxeo.theme.themes.ThemeManager

class PageInfo {
    String name;
    String link;
    String className;
    PageInfo(name, link, className) {
        this.name = name;
        this.link = link;
        this.className = className;
    }
}

applicationPath = Request.getParameter("org.nuxeo.theme.application.path")

defaultTheme = ThemeManager.getDefaultTheme(applicationPath)
defaultPageName = defaultTheme.split("/")[1]

currentPagePath = Context.getCookie("nxthemes.theme", defaultTheme)
currentThemeName = currentPagePath.split("/")[0]
currentPageName = currentPagePath.split("/")[1]

themeManager = Manager.getThemeManager();
currentTheme = themeManager.getThemeByName(currentThemeName);

availablePages = []
for (page in ThemeManager.getPagesOf(currentTheme)) {
    pageName = page.getName();
    link = String.format("%s/%s", currentThemeName, pageName);
    className = pageName.equals(currentPageName) ? "selected" : "";
    if (defaultPageName.equals(pageName)) {
        className += " default";
    }
    availablePages.add(new PageInfo(pageName, link, className));
}

return availablePages;
