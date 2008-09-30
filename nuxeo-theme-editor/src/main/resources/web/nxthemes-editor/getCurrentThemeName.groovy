import org.nuxeo.theme.Manager
import org.nuxeo.theme.themes.ThemeManager
import org.nuxeo.theme.editor.PageInfo

applicationPath = Request.getParameter("org.nuxeo.theme.application.path")
defaultTheme = ThemeManager.getDefaultTheme(applicationPath)

currentPagePath = Context.getCookie("nxthemes.theme", defaultTheme)
currentThemeName = currentPagePath.split("/")[0]

return currentThemeName
