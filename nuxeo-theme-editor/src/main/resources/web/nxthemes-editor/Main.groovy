package nxthemesEditor;

import java.io.*;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import org.nuxeo.ecm.core.rest.*;
import org.nuxeo.ecm.webengine.model.*;
import org.nuxeo.ecm.webengine.model.impl.*;
import org.nuxeo.ecm.webengine.model.exceptions.*;
import org.nuxeo.ecm.webengine.*;
import org.nuxeo.theme.*;
import org.nuxeo.theme.elements.*;
import org.nuxeo.theme.formats.*;
import org.nuxeo.theme.formats.widgets.*;
import org.nuxeo.theme.formats.styles.*;
import org.nuxeo.theme.fragments.*;
import org.nuxeo.theme.templates.*;
import org.nuxeo.theme.themes.*;
import org.nuxeo.theme.types.*;
import org.nuxeo.theme.perspectives.*;
import org.nuxeo.theme.views.*;
import org.nuxeo.theme.editor.*;


@WebModule(name="nxthemes-editor")

@Path("/nxthemes-editor")
@Produces(["text/html", "*/*"])
public class Main extends DefaultModule {

  @GET @POST
  @Path("perspectiveSelector")
  public Object renderPerspectiveSelector(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("perspectiveSelector.ftl").arg("path", path);
  }

  @GET @POST
  @Path("themeSelector")
  public Object renderThemeSelector(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("themeSelector.ftl").arg("current_theme_name", getCurrentThemeName(path)).arg("themes", getThemes(path)).arg("pages", getPages(path));
  }

  @GET @POST
  @Path("canvasModeSelector")
  public Object renderCanvasModeSelector(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("canvasModeSelector.ftl");
  }

  @GET @POST
  @Path("backToCanvas")
  public Object renderBackToCanvas(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("backToCanvas.ftl");
  }

  @GET @POST
  @Path("themeManager")
  public Object renderThemeManager(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("themeManager.ftl");
  }

  @GET @POST
  @Path("fragmentFactory")
  public Object renderFragmentFactory(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("fragmentFactory.ftl");
  }

  @GET @POST
  @Path("elementEditor")
  public Object renderElementEditor(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("elementEditor.ftl").arg("selected_element", getSelectedElement());
  }

  @GET @POST
  @Path("elementDescription")
  public Object renderElementDescription(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("elementDescription.ftl").arg("selected_element", getSelectedElement());
  }

  @GET @POST
  @Path("elementPadding")
  public Object renderElementPadding(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("elementPadding.ftl").arg("selected_element", getSelectedElement());
  }

  @GET @POST
  @Path("elementProperties")
  public Object renderElementProperties(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("elementProperties.ftl").arg("selected_element", getSelectedElement()).arg("element_properties", getSelectedElementProperties());
  }

  @GET @POST
  @Path("elementStyle")
  public Object renderElementStyle(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("elementStyle.ftl").arg("selected_element", getSelectedElement()).arg("selected_view_name", getViewNameOfSelectedElement()).arg("style_edit_mode", getStyleEditMode()).arg("style_of_selected_element", getStyleOfSelectedElement()).arg("current_theme_name", getCurrentThemeName(path)).arg("style_layers_of_selected_element", getStyleLayersOfSelectedElement());
  }

  @GET @POST
  @Path("elementWidget")
  public Object renderElementWidget(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("elementWidget.ftl").arg("selected_element", getSelectedElement()).arg("selected_view_name", getViewNameOfSelectedElement()).arg("view_names_for_selected_element", getViewNamesForSelectedElement(path));
  }

  @GET @POST
  @Path("elementVisibility")
  public Object renderElementVisibility(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("elementVisibility.ftl").arg("selected_element", getSelectedElement()).arg("is_selected_element_always_visible", isSelectedElementAlwaysVisible()).arg("perspectives", getPerspectives());
  }

  @GET @POST
  @Path("stylePicker")
  public Object renderStylePicker(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("stylePicker.ftl");
  }

  @GET @POST
  @Path("styleProperties")
  public Object renderStyleProperties(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("styleProperties.ftl");
  }
  
  @GET @POST
  @Path("select_element")
  public void selectElement(@QueryParam("id") String id) {
    def ctx = WebEngine.getActiveContext();
    SessionManager.setSelectedElementId(ctx, id);
  }
   
  public static List<FragmentType> getFragments(String applicationPath) {
      def fragments = []
      String templateEngine = getTemplateEngine(applicationPath);
      for (f in Manager.getTypeRegistry().getTypes(TypeFamily.FRAGMENT)) {
          FragmentType fragmentType = (FragmentType) f;
          FragmentInfo fragmentInfo = new FragmentInfo(fragmentType);
          for (ViewType viewType : ThemeManager.getViewTypesForFragmentType(fragmentType)) {
              String viewTemplateEngine = viewType.getTemplateEngine();
              if (!"*".equals(viewType.getViewName()) && templateEngine.equals(viewTemplateEngine)) {
                  fragmentInfo.addView(viewType);
              }
          }
          if (fragmentInfo.size() > 0) {
               fragments.add(fragmentInfo);
          }
      }
      return fragments;
  }
  
  public static Element getSelectedElement() {
    def ctx = WebEngine.getActiveContext();
    String id = SessionManager.getSelectedElementId(ctx);
    if (id == null) {
      return null;
    }
    return ThemeManager.getElementById(id);
  } 
  
  public static List<StyleLayer> getStyleLayersOfSelectedElement() {
      Style style = getStyleOfSelectedElement();
      if (style == null) {
        return [];
      }
      Style selectedStyleLayer = getSelectedStyleLayer();
      def layers = [];
      layers.add(new StyleLayer("This style", style.getUid(), style == selectedStyleLayer || selectedStyleLayer == null));
      for (Format ancestor : ThemeManager.listAncestorFormatsOf(style)) {
          layers.add(1, new StyleLayer(ancestor.getName(), ancestor.getUid(), ancestor == selectedStyleLayer));
      }
      return layers;
  }
  
  public static boolean isSelectedElementAlwaysVisible() {
      Element selectedElement = getSelectedElement();
      return Manager.getPerspectiveManager().isAlwaysVisible(selectedElement);
  }
  
  public static List<PerspectiveType> getPerspectives() {
      return PerspectiveManager.listPerspectives();
  }
  
  public static List<PerspectiveType> getPerspectivesOfSelectedElement() {
      Element selectedElement = getSelectedElement();
      def perspectives = [];
      for (PerspectiveType perspectiveType : Manager.getPerspectiveManager().getPerspectivesFor(selectedElement)) {
          perspectives.add(perspectiveType.name);
      }
      return perspectives;
  }
  
  public static String getStyleEditMode() {
      def ctx = WebEngine.getActiveContext();
      return SessionManager.getStyleEditMode(ctx);
  }
  
  public static List<String> getStyleSelectorsForSelectedElement() {
      Element element = getSelectedElement();
      String viewName = getSelectedViewName();
      Style style = getStyleOfSelectedElement();
      Style selectedStyleLayer = getSelectedStyleLayer();
      def selectors = [];
      if (selectedStyleLayer != null) {
          style = selectedStyleLayer;
      }
      if (style != null) {
          if (style.getName() != null) {
              viewName = "*";
          }
          Set<String> paths = style.getPathsForView(viewName);
          String current = getSelectedStyleSelector();
          if (current != null && !paths.contains(current)) {
              selectors.add(current);
          }
          for (path in paths) {
              selectors.add(path);
          }
      }
      return selectors;
  }
  
  public static String getSelectedStyleSelector() {
      def ctx = WebEngine.getActiveContext();
      return SessionManager.getSelectedStyleSelector(ctx);
  }
  
  public static Style getSelectedStyleLayer() {
      String selectedStyleLayerId = getSelectedStyleLayerId();
      if (selectedStyleLayerId == null) {
        return null;
      }
      return (Style) ThemeManager.getFormatById(selectedStyleLayerId);
  }
  
  public static String getSelectedStyleLayerId() {
      def ctx = WebEngine.getActiveContext();
      return SessionManager.getSelectedStyleLayerId(ctx);
  } 
  
  public static Style getStyleOfSelectedElement() {
      Element element = getSelectedElement();
      if (element == null) {
        return null;
      }
      FormatType styleType = (FormatType) Manager.getTypeRegistry().lookup(TypeFamily.FORMAT, "style");
      return (Style) ElementFormatter.getFormatByType(element, styleType);
  }
  
  public static Widget getWidgetOfSelectedElement() {
    Element element = getSelectedElement();
    if (element == null) {
      return null;
    }
    FormatType widgetType = (FormatType) Manager.getTypeRegistry().lookup(TypeFamily.FORMAT, "widget");
    return (Widget) ElementFormatter.getFormatByType(element, widgetType);
  }
  
  public static String getViewNameOfSelectedElement() {
    Widget widget = getWidgetOfSelectedElement();
    if (widget == null) {
      return "";
    }
    return widget.getName();
  }

  public static List<String> getViewNamesForSelectedElement(String applicationPath) {
      Element selectedElement = getSelectedElement();
      String templateEngine = getTemplateEngine(applicationPath);
      def viewNames = [];
      if (selectedElement == null) {
          return viewNames;
      }
      if (!selectedElement.getElementType().getTypeName().equals("fragment")) {
          return viewNames;
      }
      FragmentType fragmentType = ((Fragment) selectedElement).getFragmentType();
      for (ViewType viewType : ThemeManager.getViewTypesForFragmentType(fragmentType)) {
          String viewName = viewType.getViewName();
          String viewTemplateEngine = viewType.getTemplateEngine();
          if (!"*".equals(viewName) && templateEngine.equals(viewTemplateEngine)) {
              viewNames.add(viewName);
          }
      }
      return viewNames;
  }
   
  public static List<FieldProperty> getSelectedElementProperties() {
      Element selectedElement = getSelectedElement();
      return org.nuxeo.theme.editor.Utils.getPropertiesOf(selectedElement); 
  }
  
  public static String getTemplateEngine(String applicationPath) {
      return ThemeManager.getTemplateEngineName(applicationPath);
  }
  
  public static String getDefaultTheme(String applicationPath) {
    return ThemeManager.getDefaultTheme(applicationPath);
  }

  public static String getCurrentThemeName(String applicationPath) {
    String defaultTheme = getDefaultTheme(applicationPath);
    def ctx = WebEngine.getActiveContext();
    String currentPagePath = ctx.getCookie("nxthemes.theme");
    if (currentPagePath == null) {
      currentPagePath = defaultTheme;
    }
    return currentPagePath.split("/")[0];
  }

  public static List<PageElement> getPages(String applicationPath) {
    ThemeManager themeManager = Manager.getThemeManager();
    def ctx = WebEngine.getActiveContext();
    String currentPagePath = ctx.getCookie("nxthemes.theme");
    String defaultTheme = getDefaultTheme(applicationPath);
    String defaultPageName = defaultTheme.split("/")[1];

    def pages = [];
    if (!currentPagePath || !currentPagePath.contains("/")) {
      currentPagePath = defaultTheme;
    }

    String currentThemeName = currentPagePath.split("/")[0];
    String currentPageName = currentPagePath.split("/")[1];
    ThemeElement currentTheme = themeManager.getThemeByName(currentThemeName);

    if (currentTheme == null) {
      return pages;
    }

    for (PageElement page : ThemeManager.getPagesOf(currentTheme)) {
      String pageName = page.getName();
      String link = String.format("%s/%s", currentThemeName, pageName);
      String className = pageName.equals(currentPageName) ? "selected" : "";
      if (defaultPageName.equals(pageName)) {
        className += " default";
      }
      pages.add(new PageInfo(pageName, link, className));
    }
    return pages;
  }

  public static List<ThemeElement> getThemes(String applicationPath) {
    String defaultTheme = getDefaultTheme(applicationPath);
    def themes = [];
    if (!defaultTheme.contains("/")) {
      return themes;
    }

    String defaultThemeName = defaultTheme.split("/")[0];
    String defaultPageName = defaultTheme.split("/")[1];
    String currentThemeName = getCurrentThemeName(applicationPath);

    for (themeName in Manager.getThemeManager().getThemeNames()) {
      String link = String.format("%s/%s", themeName, defaultPageName);
      String className = themeName.equals(currentThemeName) ? "selected" : "";
      if (link.equals(defaultThemeName)) {
        className += " default";
      }
      themes.add(new ThemeInfo(themeName, link, className));
    }
    return themes;
  }

}

