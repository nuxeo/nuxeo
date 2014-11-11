import org.nuxeo.theme.Manager
import org.nuxeo.theme.themes.ThemeManager
import org.nuxeo.theme.editor.PageInfo

themeManager = Manager.getThemeManager()

applicationPath = Request.getParameter("org.nuxeo.theme.application.path")
defaultTheme = ThemeManager.getDefaultTheme(applicationPath)
defaultPageName = defaultTheme.split("/")[1]

currentPagePath = Context.getCookie("nxthemes.theme")

pages = []
if (!currentPagePath || !currentPagePath.contains("/")) {
  currentPagePath = defaultTheme
}

currentThemeName = currentPagePath.split("/")[0]
currentPageName = currentPagePath.split("/")[1]
currentTheme = themeManager.getThemeByName(currentThemeName)

if (currentTheme == null) {
    return pages
}

for (page in ThemeManager.getPagesOf(currentTheme)) {
    pageName = page.getName()
    link = String.format("%s/%s", currentThemeName, pageName)
    className = pageName.equals(currentPageName) ? "selected" : ""
    if (defaultPageName.equals(pageName)) {
        className += " default"
    }
    pages.add(new PageInfo(pageName, link, className))
}

return pages
