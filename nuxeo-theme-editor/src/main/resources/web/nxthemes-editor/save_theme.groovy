
import org.nuxeo.theme.themes.ThemeManager

src = Request.getParameter("src")
indent = Request.getParameter("indent")

try {
    ThemeManager.saveTheme(src, indent);
} catch (ThemeIOException e) {;
    return false;
}

return true
