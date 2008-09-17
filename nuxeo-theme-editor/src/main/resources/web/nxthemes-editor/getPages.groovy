
import org.nuxeo.theme.Manager
import org.nuxeo.theme.themes.ThemeManager
import org.nuxeo.theme.editor.PageInfo

applicationPath = Request.getParameter("org.nuxeo.theme.application.path")

defaultTheme = ThemeManager.getDefaultTheme(applicationPath)
defaultPageName = defaultTheme.split("/")[1]

currentPagePath = Context.getCookie("nxthemes.theme", defaultTheme)

pages = []
if (!currentPagePath || !currentPagePath.contains("/")) {
  return pages
}

currentThemeName = currentPagePath.split("/")[0]
currentPageName = currentPagePath.split("/")[1]

themeManager = Manager.getThemeManager();
currentTheme = themeManager.getThemeByName(currentThemeName);

for (page in ThemeManager.getPagesOf(currentTheme)) {
    pageName = page.getName();
    link = String.format("%s/%s", currentThemeName, pageName);
    className = pageName.equals(currentPageName) ? "selected" : "";
    if (defaultPageName.equals(pageName)) {
        className += " default";
    }
    pages.add(new PageInfo(pageName, link, className));
}

return pages;
