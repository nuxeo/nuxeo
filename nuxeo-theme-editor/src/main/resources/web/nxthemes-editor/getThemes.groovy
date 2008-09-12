
import org.nuxeo.theme.Manager
import org.nuxeo.theme.themes.ThemeManager

themes = [];

class ThemeInfo {
    String name;
    String link;
    String className;

    ThemeInfo(String name, String link, String className) {
        this.name = name;
        this.link = link;
        this.className = className;
    }
}


applicationPath = Request.getParameter("org.nuxeo.theme.application.path")

defaultTheme = ThemeManager.getDefaultTheme(applicationPath)
defaultThemeName = defaultTheme.split("/")[0]
defaultPageName = defaultTheme.split("/")[1]

currentPagePath = Context.getCookie("nxthemes.theme", defaultTheme)

currentThemeName = currentPagePath.split("/")[0]

themeManager = Manager.getThemeManager();
for (themeName in themeManager.getThemeNames()) {
  link = String.format("%s/%s", themeName, defaultPageName);
  className = themeName.equals(currentThemeName) ? "selected" : "";
  if (link.equals(defaultThemeName)) {
    className += " default";
  }
  themes.add(new ThemeInfo(themeName, link, className));
}

return themes;

