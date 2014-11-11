import org.nuxeo.theme.Manager
import org.nuxeo.theme.themes.ThemeManager
import org.nuxeo.theme.editor.PageInfo

defaultTheme = Context.runScript("getDefaultTheme.groovy")

currentPagePath = Context.getCookie("nxthemes.theme", defaultTheme)
currentThemeName = currentPagePath.split("/")[0]

return currentThemeName
