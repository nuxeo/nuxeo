import org.nuxeo.theme.Manager
import org.nuxeo.theme.themes.ThemeManager
import org.nuxeo.theme.editor.ThemeInfo

defaultTheme = Context.runScript("getDefaultTheme.groovy")

themes = []
if (!defaultTheme.contains("/")) {
    return themes
}

defaultThemeName = defaultTheme.split("/")[0]
defaultPageName = defaultTheme.split("/")[1]

currentThemeName = Context.runScript("getCurrentThemeName.groovy")

for (themeName in Manager.getThemeManager().getThemeNames()) {
  link = String.format("%s/%s", themeName, defaultPageName)
  className = themeName.equals(currentThemeName) ? "selected" : ""
  if (link.equals(defaultThemeName)) {
    className += " default"
  }
  themes.add(new ThemeInfo(themeName, link, className))
}

return themes

