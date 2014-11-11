package org.nuxeo.theme.editor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import net.sf.json.JSONObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.Access;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.elements.Element;
import org.nuxeo.theme.elements.ElementFormatter;
import org.nuxeo.theme.elements.PageElement;
import org.nuxeo.theme.elements.ThemeElement;
import org.nuxeo.theme.formats.Format;
import org.nuxeo.theme.formats.FormatType;
import org.nuxeo.theme.formats.layouts.Layout;
import org.nuxeo.theme.formats.styles.Style;
import org.nuxeo.theme.formats.widgets.Widget;
import org.nuxeo.theme.fragments.Fragment;
import org.nuxeo.theme.fragments.FragmentType;
import org.nuxeo.theme.perspectives.PerspectiveManager;
import org.nuxeo.theme.perspectives.PerspectiveType;
import org.nuxeo.theme.presets.PresetManager;
import org.nuxeo.theme.presets.PresetType;
import org.nuxeo.theme.resources.ImageInfo;
import org.nuxeo.theme.resources.ResourceBank;
import org.nuxeo.theme.resources.SkinInfo;
import org.nuxeo.theme.themes.ThemeDescriptor;
import org.nuxeo.theme.themes.ThemeException;
import org.nuxeo.theme.themes.ThemeIOException;
import org.nuxeo.theme.themes.ThemeManager;
import org.nuxeo.theme.themes.ThemeSerializer;
import org.nuxeo.theme.types.TypeFamily;
import org.nuxeo.theme.views.ViewType;

@Path("/nxthemes-editor")
@WebObject(type = "nxthemes-editor", administrator = Access.GRANT)
@Produces(MediaType.TEXT_HTML)
public class Main extends ModuleRoot {

    private static final Log log = LogFactory.getLog(Main.class);

    @GET
    @Path("perspectiveSelector")
    public Object renderPerspectiveSelector(
            @QueryParam("org.nuxeo.theme.application.path") String path,
            @QueryParam("org.nuxeo.theme.application.name") String name) {
        return getTemplate("perspectiveSelector.ftl").arg("perspectives",
                getPerspectives());
    }

    @GET
    @Path("themeSelector")
    public Object renderThemeSelector(
            @QueryParam("org.nuxeo.theme.application.path") String path,
            @QueryParam("org.nuxeo.theme.application.name") String name) {
        return getTemplate("themeSelector.ftl").arg("current_theme_name",
                getCurrentThemeName(path, name)).arg("themes",
                getWorkspaceThemes(path, name));
    }

    @GET
    @Path("pageSelector")
    public Object renderPageSelector(
            @QueryParam("org.nuxeo.theme.application.path") String path,
            @QueryParam("org.nuxeo.theme.application.name") String name) {
        return getTemplate("pageSelector.ftl").arg("current_theme_name",
                getCurrentThemeName(path, name)).arg("pages",
                getPages(path, name));
    }

    @GET
    @Path("canvasModeSelector")
    public Object renderCanvasModeSelector(
            @QueryParam("org.nuxeo.theme.application.path") String path,
            @QueryParam("org.nuxeo.theme.application.name") String name) {
        return getTemplate("canvasModeSelector.ftl");
    }

    @GET
    @Path("themeOptions")
    public Object renderThemeOptions(
            @QueryParam("org.nuxeo.theme.application.path") String path,
            @QueryParam("org.nuxeo.theme.application.name") String name) {
        String currentThemeName = getCurrentThemeName(path, name);
        String templateEngine = getTemplateEngine(path);
        ThemeDescriptor currentThemeDef = ThemeManager.getThemeDescriptorByThemeName(
                templateEngine, currentThemeName);
        return getTemplate("themeOptions.ftl").arg("current_theme",
                currentThemeDef);
    }

    @GET
    @Path("presetManager")
    public Object renderPresetManager(
            @QueryParam("org.nuxeo.theme.application.path") String path,
            @QueryParam("org.nuxeo.theme.application.name") String name) {
        return getTemplate("presetManager.ftl").arg("current_theme_name",
                getCurrentThemeName(path, name)).arg("preset_manager_mode",
                getPresetManagerMode()).arg("selected_preset_category",
                getSelectedPresetCategory()).arg("preset_groups",
                getPresetGroupsForSelectedCategory()).arg(
                "selected_preset_group", getSelectedPresetGroup());
    }

    @GET
    @Path("styleManager")
    public Object renderStyleManager(
            @QueryParam("org.nuxeo.theme.application.path") String path,
            @QueryParam("org.nuxeo.theme.application.name") String name) {
        String currentThemeName = getCurrentThemeName(path, name);
        ResourceBank resourceBank = getCurrentThemeBank(currentThemeName);
        List<Style> styles = Editor.getNamedStyles(currentThemeName,
                resourceBank);

        Style selectedStyle = getSelectedNamedStyle();
        if (!styles.contains(selectedStyle) && !styles.isEmpty()) {
            selectedStyle = styles.get(0);
        }
        List<Style> rootStyles = new ArrayList<Style>();
        for (Style style : styles) {
            if (ThemeManager.getAncestorFormatOf(style) == null) {
                rootStyles.add(style);
            }
        }

        String templateEngine = getTemplateEngine(path);
        ThemeDescriptor currentThemeDef = ThemeManager.getThemeDescriptorByThemeName(
                templateEngine, currentThemeName);
        return getTemplate("styleManager.ftl").arg("current_theme",
                currentThemeDef).arg("named_styles", styles).arg(
                "style_manager_mode", getStyleManagerMode()).arg(
                "selected_named_style", selectedStyle).arg(
                "selected_named_style_css",
                getRenderedPropertiesForNamedStyle(selectedStyle)).arg(
                "current_theme_name", currentThemeName).arg("page_styles",
                Editor.getPageStyles(currentThemeName)).arg("root_styles",
                rootStyles);
    }

    @GET
    @Path("themeActions")
    public Object renderThemeActions(
            @QueryParam("org.nuxeo.theme.application.path") String path,
            @QueryParam("org.nuxeo.theme.application.name") String name) {
        String currentThemeName = getCurrentThemeName(path, name);
        String templateEngine = getTemplateEngine(path);
        String currentPagePath = getCurrentPagePath(path, name);
        String currentpageName = ThemeManager.getPageNameFromPagePath(currentPagePath);
        ThemeDescriptor currentThemeDef = ThemeManager.getThemeDescriptorByThemeName(
                templateEngine, currentThemeName);
        return getTemplate("themeActions.ftl").arg("theme", currentThemeDef).arg(
                "current_page_path", currentPagePath).arg("current_page_name",
                currentpageName);
    }

    @GET
    @Path("dashboardActions")
    public Object renderDashboardActions(
            @QueryParam("org.nuxeo.theme.application.path") String path,
            @QueryParam("org.nuxeo.theme.application.name") String name) {
        String currentThemeName = getCurrentThemeName(path, name);
        String templateEngine = getTemplateEngine(path);
        ThemeDescriptor currentThemeDef = ThemeManager.getThemeDescriptorByThemeName(
                templateEngine, currentThemeName);
        return getTemplate("dashboardActions.ftl").arg("theme", currentThemeDef);
    }

    @GET
    @Path("editorActions")
    public Object renderEditorActions(
            @QueryParam("org.nuxeo.theme.application.path") String path,
            @QueryParam("org.nuxeo.theme.application.name") String name) {
        return getTemplate("editorActions.ftl");
    }

    @GET
    @Path("cssEditor")
    public Object renderCssEditor(
            @QueryParam("org.nuxeo.theme.application.path") String path,
            @QueryParam("org.nuxeo.theme.application.name") String name) {
        String currentThemeName = getCurrentThemeName(path, name);
        ResourceBank currentThemeBank = getCurrentThemeBank(currentThemeName);
        List<Style> styles = Editor.getNamedStyles(currentThemeName,
                currentThemeBank);
        Style themeSkin = Editor.getThemeSkin(currentThemeName);
        String templateEngine = getTemplateEngine(path);
        ThemeDescriptor currentThemeDef = ThemeManager.getThemeDescriptorByThemeName(
                templateEngine, currentThemeName);

        return getTemplate("cssEditor.ftl").arg("theme", currentThemeDef).arg(
                "named_styles", styles).arg("style_manager_mode",
                getStyleManagerMode()).arg("theme_skin", themeSkin).arg(
                "theme_skin_css", getRenderedPropertiesForNamedStyle(themeSkin)).arg(
                "current_theme_name", currentThemeName).arg("current_bank",
                currentThemeBank).arg("current_theme", currentThemeDef);
    }

    @GET
    @Path("themeBrowser")
    public Object renderThemeBrowser(
            @QueryParam("org.nuxeo.theme.application.path") String path,
            @QueryParam("org.nuxeo.theme.application.name") String name) {
        String currentThemeName = getCurrentThemeName(path, name);
        Set<ThemeDescriptor> availableThemes = new LinkedHashSet<ThemeDescriptor>();
        Set<ThemeInfo> workspaceThemes = getWorkspaceThemes(path, name);
        Set<String> workspaceThemeNames = SessionManager.getWorkspaceThemeNames();
        String templateEngine = getTemplateEngine(path);

        for (ThemeDescriptor themeDef : ThemeManager.getThemeDescriptors()) {
            if (themeDef.isCustomized()) {
                continue;
            }
            List<String> templateEngines = themeDef.getTemplateEngines();
            if (templateEngines != null
                    && !templateEngines.contains(templateEngine)) {
                continue;
            }
            if (!workspaceThemeNames.contains(themeDef.getName())) {
                availableThemes.add(themeDef);
            }
        }
        return getTemplate("themeBrowser.ftl").arg("current_theme_name",
                currentThemeName).arg("available_themes", availableThemes).arg(
                "workspace_themes", workspaceThemes);
    }

    @GET
    @Path("viewModes")
    public Object renderViewModes(
            @QueryParam("org.nuxeo.theme.application.path") String path,
            @QueryParam("org.nuxeo.theme.application.name") String name) {
        return getTemplate("viewModes.ftl");
    }

    @GET
    @Path("undoActions")
    public Object renderUndoActions(
            @QueryParam("org.nuxeo.theme.application.path") String path,
            @QueryParam("org.nuxeo.theme.application.name") String name) {
        String themeName = getCurrentThemeName(path, name);
        UndoBuffer undoBuffer = SessionManager.getUndoBuffer(themeName);
        return getTemplate("undoActions.ftl").arg("current_theme_name",
                themeName).arg("undo_buffer", undoBuffer);
    }

    @GET
    @Path("fragmentFactory")
    public Object renderFragmentFactory(
            @QueryParam("org.nuxeo.theme.application.path") String path,
            @QueryParam("org.nuxeo.theme.application.name") String name) {
        String currentThemeName = getCurrentThemeName(path, name);
        String fragmentType = getSelectedFragmentType();
        String fragmentView = getSelectedFragmentView();
        String fragmentStyle = getSelectedFragmentStyle();
        String templateEngine = getTemplateEngine(path);
        ResourceBank resourceBank = getCurrentThemeBank(currentThemeName);
        return getTemplate("fragmentFactory.ftl").arg("current_theme_name",
                getCurrentThemeName(path, name)).arg("selected_fragment_type",
                fragmentType).arg("selected_fragment_view", fragmentView).arg(
                "selected_fragment_style", fragmentStyle).arg("fragments",
                Editor.getFragments(templateEngine)).arg("styles",
                Editor.getNamedStyles(currentThemeName, resourceBank)).arg(
                "views", Editor.getViews(fragmentType, templateEngine)).arg(
                "selected_element_id", getSelectedElementId());
    }

    @GET
    @Path("elementEditor")
    public Object renderElementEditor(
            @QueryParam("org.nuxeo.theme.application.path") String path,
            @QueryParam("org.nuxeo.theme.application.name") String name) {
        return getTemplate("elementEditor.ftl").arg("selected_element",
                getSelectedElement());
    }

    @GET
    @Path("elementDescription")
    public Object renderElementDescription(
            @QueryParam("org.nuxeo.theme.application.path") String path,
            @QueryParam("org.nuxeo.theme.application.name") String name) {
        return getTemplate("elementDescription.ftl").arg("selected_element",
                getSelectedElement());
    }

    @GET
    @Path("elementPadding")
    public Object renderElementPadding(
            @QueryParam("org.nuxeo.theme.application.path") String path,
            @QueryParam("org.nuxeo.theme.application.name") String name) {
        return getTemplate("elementPadding.ftl").arg("selected_element",
                getSelectedElement()).arg("padding_of_selected_element",
                getPaddingOfSelectedElement());
    }

    @GET
    @Path("elementProperties")
    public Object renderElementProperties(
            @QueryParam("org.nuxeo.theme.application.path") String path,
            @QueryParam("org.nuxeo.theme.application.name") String name) {
        return getTemplate("elementProperties.ftl").arg("selected_element",
                getSelectedElement()).arg("element_properties",
                getSelectedElementProperties());
    }

    @GET
    @Path("elementStyle")
    public Object renderElementStyle(
            @QueryParam("org.nuxeo.theme.application.path") String path,
            @QueryParam("org.nuxeo.theme.application.name") String name) {
        String currentThemeName = getCurrentThemeName(path, name);
        ResourceBank resourceBank = getCurrentThemeBank(currentThemeName);
        return getTemplate("elementStyle.ftl").arg("selected_element",
                getSelectedElement()).arg("style_of_selected_element",
                getStyleOfSelectedElement()).arg("current_theme_name",
                getCurrentThemeName(path, name)).arg(
                "style_layers_of_selected_element",
                getStyleLayersOfSelectedElement()).arg(
                "inherited_style_name_of_selected_element",
                getInheritedStyleNameOfSelectedElement()).arg("named_styles",
                Editor.getNamedStyles(currentThemeName, resourceBank));
    }

    @GET
    @Path("elementWidget")
    public Object renderElementWidget(
            @QueryParam("org.nuxeo.theme.application.path") String path,
            @QueryParam("org.nuxeo.theme.application.name") String name) {
        return getTemplate("elementWidget.ftl").arg("selected_element",
                getSelectedElement()).arg("selected_view_name",
                getViewNameOfSelectedElement()).arg(
                "view_names_for_selected_element",
                getViewNamesForSelectedElement(path));
    }

    @GET
    @Path("elementVisibility")
    public Object renderElementVisibility(
            @QueryParam("org.nuxeo.theme.application.path") String path,
            @QueryParam("org.nuxeo.theme.application.name") String name) {
        return getTemplate("elementVisibility.ftl").arg("selected_element",
                getSelectedElement()).arg("perspectives_of_selected_element",
                getPerspectivesOfSelectedElement()).arg(
                "is_selected_element_always_visible",
                isSelectedElementAlwaysVisible()).arg("perspectives",
                getPerspectives());
    }

    @GET
    @Path("stylePicker")
    public Object renderStylePicker(
            @QueryParam("org.nuxeo.theme.application.path") String path,
            @QueryParam("org.nuxeo.theme.application.name") String name) {
        String currentThemeName = getCurrentThemeName(path, name);
        ResourceBank resourceBank = getCurrentThemeBank(currentThemeName);
        return getTemplate("stylePicker.ftl").arg("resource_bank", resourceBank).arg(
                "style_category", getSelectedStyleCategory()).arg(
                "current_theme_name", getCurrentThemeName(path, name)).arg(
                "selected_preset_group", getSelectedPresetGroup()).arg(
                "preset_groups", getPresetGroupsForSelectedCategory()).arg(
                "presets_for_selected_group",
                getPresetsForSelectedGroup(path, name));
    }

    @GET
    @Path("areaStyleChooser")
    public Object renderAreaStyleChooser(
            @QueryParam("org.nuxeo.theme.application.path") String path,
            @QueryParam("org.nuxeo.theme.application.name") String name) {
        String currentThemeName = getCurrentThemeName(path, name);
        ResourceBank resourceBank = getCurrentThemeBank(currentThemeName);
        return getTemplate("areaStyleChooser.ftl").arg("resource_bank",
                resourceBank).arg("style_category", getSelectedStyleCategory()).arg(
                "current_theme_name", getCurrentThemeName(path, name)).arg(
                "preset_groups", getPresetGroupsForSelectedCategory()).arg(
                "presets_for_selected_group",
                getPresetsForSelectedGroup(path, name)).arg(
                "selected_preset_group", getSelectedPresetGroup());
    }

    @GET
    @Path("styleProperties")
    public Object renderStyleProperties(
            @QueryParam("org.nuxeo.theme.application.path") String path,
            @QueryParam("org.nuxeo.theme.application.name") String name) {
        return getTemplate("styleProperties.ftl").arg("selected_element",
                getSelectedElement()).arg("style_edit_mode", getStyleEditMode()).arg(
                "style_selectors", getStyleSelectorsForSelectedElement()).arg(
                "rendered_style_properties",
                getRenderedStylePropertiesForSelectedElement()).arg(
                "selected_style_selector", getSelectedStyleSelector()).arg(
                "element_style_properties",
                getStylePropertiesForSelectedElement()).arg(
                "all_style_properties",
                getAvailableStylePropertiesForSelectedElement()).arg(
                "selected_view_name", getViewNameOfSelectedElement()).arg(
                "selected_css_categories", getSelectedCssCategories());
    }

    @GET
    @Path("render_css_preview")
    public String renderCssPreview(@QueryParam("basePath") String basePath) {
        Style selectedStyleLayer = getSelectedStyleLayer();
        String selectedViewName = getViewNameOfSelectedElement();
        Element selectedElement = getSelectedElement();
        return Editor.renderCssPreview(selectedElement, selectedStyleLayer,
                selectedViewName, basePath);
    }

    // Dashboard
    @GET
    @Path("dashboard")
    public Object renderDashboard(
            @QueryParam("org.nuxeo.theme.application.path") String path,
            @QueryParam("org.nuxeo.theme.application.name") String name) {
        String currentThemeName = getCurrentThemeName(path, name);
        String templateEngine = getTemplateEngine(path);
        ThemeDescriptor currentThemeDef = ThemeManager.getThemeDescriptorByThemeName(
                templateEngine, currentThemeName);
        return getTemplate("dashboard.ftl").arg("current_theme",
                currentThemeDef);
    }

    @GET
    @Path("controlPanel")
    public Object renderControlPanel(
            @QueryParam("org.nuxeo.theme.application.path") String path,
            @QueryParam("org.nuxeo.theme.application.name") String name) {
        String currentThemeName = getCurrentThemeName(path, name);
        String currentSkinName = Editor.getCurrentTopSkinName(currentThemeName);
        String templateEngine = getTemplateEngine(path);
        ThemeDescriptor currentThemeDescriptor = ThemeManager.getThemeDescriptorByThemeName(
                templateEngine, currentThemeName);

        ResourceBank currentThemeBank = getCurrentThemeBank(currentThemeName);

        return getTemplate("controlPanel.ftl").arg("current_theme",
                currentThemeDescriptor).arg("current_skin_name",
                currentSkinName).arg("current_bank", currentThemeBank);
    }

    @GET
    @Path("dashboardPreview")
    public Object renderDashboardPreview(
            @QueryParam("org.nuxeo.theme.application.path") String path,
            @QueryParam("org.nuxeo.theme.application.name") String name) {
        String currentThemeName = getCurrentThemeName(path, name);
        String templateEngine = getTemplateEngine(path);
        String currentPagePath = getCurrentPagePath(path, name);
        String currentpageName = ThemeManager.getPageNameFromPagePath(currentPagePath);
        ThemeDescriptor currentThemeDef = ThemeManager.getThemeDescriptorByThemeName(
                templateEngine, currentThemeName);
        return getTemplate("dashboardPreview.ftl").arg("theme", currentThemeDef).arg(
                "current_page_path", currentPagePath).arg("current_page_name",
                currentpageName);
    }

    @GET
    @Path("skinManager")
    public Object renderSkinManager(
            @QueryParam("org.nuxeo.theme.application.path") String path,
            @QueryParam("org.nuxeo.theme.application.name") String name) {

        String currentThemeName = getCurrentThemeName(path, name);
        String currentSkinName = Editor.getCurrentTopSkinName(currentThemeName);
        String currentBaseSkinName = Editor.getCurrentBaseSkinName(currentThemeName);
        ResourceBank currentThemeBank = getCurrentThemeBank(currentThemeName);

        String templateEngine = getTemplateEngine(path);
        ThemeDescriptor currentThemeDescriptor = ThemeManager.getThemeDescriptorByThemeName(
                templateEngine, currentThemeName);

        List<SkinInfo> skins = new ArrayList<SkinInfo>();
        List<SkinInfo> baseSkins = new ArrayList<SkinInfo>();
        List<String> collections = new ArrayList<String>();
        if (currentThemeBank != null) {
            String bankName = currentThemeBank.getName();
            for (SkinInfo skin : Editor.getBankSkins(bankName)) {
                if (skin.isBase()) {
                    baseSkins.add(skin);
                } else {
                    skins.add(skin);
                }
            }
            collections = Editor.getBankCollections(bankName);
        }
        return getTemplate("skinManager.ftl").arg("current_skin_name",
                currentSkinName).arg("current_base_skin_name",
                currentBaseSkinName).arg("current_theme",
                currentThemeDescriptor).arg("current_bank", currentThemeBank).arg(
                "skins", skins).arg("base_skins", baseSkins).arg("collections",
                collections);
    }

    @GET
    @Path("bankManager")
    public Object renderBankManager(
            @QueryParam("org.nuxeo.theme.application.path") String path,
            @QueryParam("org.nuxeo.theme.application.name") String name) {

        String currentThemeName = getCurrentThemeName(path, name);
        ResourceBank currentThemeBank = getCurrentThemeBank(currentThemeName);

        List<ResourceBank> banks = ThemeManager.getResourceBanks();
        List<String> collections = new ArrayList<String>();

        ResourceBank selectedResourceBank = null;
        if (currentThemeBank != null) {
            selectedResourceBank = currentThemeBank;
            collections = Editor.getBankCollections(currentThemeBank.getName());
        } else {
            String selectedResourceBankName = getSelectedResourceBank();
            if (selectedResourceBankName == null) {
                if (!banks.isEmpty()) {
                    selectedResourceBank = banks.get(0);
                }
                collections = Editor.getBankCollections(selectedResourceBankName);
            } else {
                try {
                    selectedResourceBank = ThemeManager.getResourceBank(selectedResourceBankName);
                } catch (ThemeException e) {
                    e.printStackTrace();
                }
            }
        }
        String templateEngine = getTemplateEngine(path);
        ThemeDescriptor currentThemeDescriptor = ThemeManager.getThemeDescriptorByThemeName(
                templateEngine, currentThemeName);

        String selectedBankCollection = getSelectedBankCollection();

        return getTemplate("bankManager.ftl").arg("current_theme",
                currentThemeDescriptor).arg("current_bank", currentThemeBank).arg(
                "banks", banks).arg("selected_bank", selectedResourceBank).arg(
                "collections", collections).arg("selected_bank_collection",
                selectedBankCollection);
    }

    private ResourceBank getCurrentThemeBank(String themeName) {
        ThemeDescriptor themeDescriptor = ThemeManager.getThemeDescriptorByThemeName(themeName);
        if (themeDescriptor != null) {
            String currentThemeBankName = themeDescriptor.getResourceBankName();
            if (currentThemeBankName != null) {
                try {
                    return ThemeManager.getResourceBank(currentThemeBankName);
                } catch (ThemeException e) {
                    return null;
                }
            }
        }
        return null;
    }

    @POST
    @Path("use_resource_bank")
    public void useResourceBank(@FormParam("theme_src") String themeSrc,
            @FormParam("bank") String bankName) {
        try {
            if ("".equals(bankName)) {
                Editor.useNoResourceBank(themeSrc);
            } else {
                Editor.useResourceBank(themeSrc, bankName);
            }
        } catch (Exception e) {
            throw new ThemeEditorException("Cannot connect to bank: "
                    + bankName, e);
        }
    }

    @POST
    @Path("activate_skin")
    public void activateSkin(@FormParam("theme") String themeName,
            @FormParam("bank") String bankName,
            @FormParam("collection") String collectionName,
            @FormParam("resource") String resourceName,
            @FormParam("base") boolean baseSkin) {
        try {
            Editor.activateSkin(themeName, bankName, collectionName,
                    resourceName, baseSkin);
        } catch (Exception e) {
            throw new ThemeEditorException("Could not activate skin: "
                    + e.getMessage(), e);
        }
    }

    @POST
    @Path("deactivate_skin")
    public void activateSkin(@FormParam("theme") String themeName) {
        try {
            Editor.deactivateSkin(themeName);
        } catch (Exception e) {
            throw new ThemeEditorException("Could not deactivate skin: "
                    + e.getMessage(), e);
        }
    }

    @GET
    @Path("imageManager")
    public Object renderImageManager(
            @QueryParam("org.nuxeo.theme.application.path") String path,
            @QueryParam("org.nuxeo.theme.application.name") String name) {
        String currentThemeName = getCurrentThemeName(path, name);
        ResourceBank currentThemeBank = getCurrentThemeBank(currentThemeName);

        String selectedBankCollection = getSelectedBankCollection();

        List<String> collections = new ArrayList<String>();
        List<ImageInfo> images = new ArrayList<ImageInfo>();
        if (currentThemeBank != null) {
            String bankName = currentThemeBank.getName();
            collections = Editor.getBankCollections(bankName);
            images = Editor.getBankImages(bankName);
        }

        String templateEngine = getTemplateEngine(path);
        ThemeDescriptor currentThemeDescriptor = ThemeManager.getThemeDescriptorByThemeName(
                templateEngine, currentThemeName);

        return getTemplate("imageManager.ftl").arg("current_theme",
                currentThemeDescriptor).arg("current_edit_field",
                getSelectedEditField()).arg("current_bank", currentThemeBank).arg(
                "images", images).arg("collections", collections).arg(
                "selected_bank_collection", selectedBankCollection);
    }

    @GET
    @Path("imageUploaded")
    public Object renderImageUploaded(
            @QueryParam("org.nuxeo.theme.application.path") String path,
            @QueryParam("org.nuxeo.theme.application.name") String name) {
        return getTemplate("imageUploaded.ftl");
    }

    public ResourceBank getResourceBank(String bankName) throws ThemeException {
        return ThemeManager.getResourceBank(bankName);
    }

    @GET
    @Path("xml_export")
    public Response xmlExport(@QueryParam("src") String src,
            @QueryParam("download") Integer download,
            @QueryParam("indent") Integer indent) {
        if (src == null) {
            return null;
        }
        Manager.getThemeManager();
        ThemeDescriptor themeDef;
        try {
            themeDef = ThemeManager.getThemeDescriptor(src);
        } catch (ThemeException e) {
            throw new ThemeEditorException(e.getMessage(), e);
        }

        ThemeSerializer serializer = new ThemeSerializer();
        if (indent == null) {
            indent = 0;
        }

        String xml;
        try {
            xml = serializer.serializeToXml(src, indent);
        } catch (ThemeIOException e) {
            throw new ThemeEditorException(e.getMessage(), e);
        }

        ResponseBuilder builder = Response.ok(xml);
        if (download != null) {
            builder.header("Content-disposition", String.format(
                    "attachment; filename=theme-%s.xml", themeDef.getName()));
        }
        builder.type("text/xml");
        return builder.build();
    }

    @POST
    @Path("clear_selections")
    public void clearSelections() {
        SessionManager.setElementId(null);
        SessionManager.setStyleEditMode(null);
        SessionManager.setStyleLayerId(null);
        SessionManager.setStyleSelector(null);
        SessionManager.setNamedStyleId(null);
        SessionManager.setStyleCategory(null);
        SessionManager.setPresetGroup(null);
        SessionManager.setPresetCategory(null);
        SessionManager.setClipboardElementId(null);
        SessionManager.setFragmentType(null);
        SessionManager.setFragmentView(null);
        SessionManager.setFragmentStyle(null);
    }

    @POST
    @Path("select_element")
    public void selectElement(@FormParam("id") String id) {
        SessionManager.setElementId(id);
        // clean up
        SessionManager.setStyleEditMode(null);
        SessionManager.setStyleLayerId(null);
        SessionManager.setNamedStyleId(null);
        SessionManager.setStyleSelector(null);
        SessionManager.setStyleCategory(null);
        SessionManager.setPresetGroup(null);
        SessionManager.setPresetCategory(null);
        SessionManager.setFragmentType(null);
        SessionManager.setFragmentView(null);
        SessionManager.setFragmentStyle(null);
    }

    @POST
    @Path("add_page")
    public String addPage(@FormParam("path") String pagePath) {
        try {
            return Editor.addPage(pagePath);
        } catch (Exception e) {
            throw new ThemeEditorException(e.getMessage(), e);
        }
    }

    @POST
    @Path("add_theme")
    public String addTheme(@FormParam("name") String name) {
        try {
            return Editor.addTheme(name);
        } catch (Exception e) {
            throw new ThemeEditorException(e.getMessage(), e);
        }
    }

    @POST
    @Path("uncustomize_theme")
    public String uncustomizeTheme(@FormParam("src") String src) {
        try {
            return Editor.uncustomizeTheme(src);
        } catch (Exception e) {
            throw new ThemeEditorException(e.getMessage(), e);
        }
    }

    @POST
    @Path("align_element")
    public void alignElement(@FormParam("id") String id,
            @FormParam("position") String position) {
        Element element = ThemeManager.getElementById(id);
        try {
            Editor.alignElement(element, position);
        } catch (Exception e) {
            throw new ThemeEditorException(e.getMessage(), e);
        }

    }

    @POST
    @Path("assign_style_property")
    public void assignStyleProperty(@FormParam("element_id") String id,
            @FormParam("property") String propertyName,
            @FormParam("value") String value) {
        Element element = ThemeManager.getElementById(id);
        try {
            Editor.assignStyleProperty(element, propertyName, value);
        } catch (Exception e) {
            throw new ThemeEditorException(e.getMessage(), e);
        }
    }

    @POST
    @Path("copy_element")
    public void copyElement(@FormParam("id") String id) {
        SessionManager.setClipboardElementId(id);
    }

    @POST
    @Path("set_preset_category")
    public void setPresetCategory(@FormParam("theme_name") String themeName,
            @FormParam("preset_name") String presetName,
            @FormParam("category") String category) {
        try {
            Editor.setPresetCategory(themeName, presetName, category);
        } catch (Exception e) {
            throw new ThemeEditorException(e.getMessage(), e);
        }
    }

    @POST
    @Path("copy_preset")
    public void copyPreset(@FormParam("id") String id) {
        SessionManager.setClipboardPresetId(id);
    }

    @POST
    @Path("paste_preset")
    public void pastePreset(@FormParam("theme_name") String themeName,
            @FormParam("preset_name") String newPresetName) {
        String presetName = getClipboardPreset();
        if (presetName == null) {
            throw new ThemeEditorException("Nothing to paste");
        }
        PresetType preset = PresetManager.getPresetByName(presetName);
        if (preset == null) {
            throw new ThemeEditorException("Preset not found: " + presetName);
        }
        try {
            Editor.addPreset(themeName, newPresetName, preset.getCategory(),
                    preset.getValue());
        } catch (Exception e) {
            throw new ThemeEditorException(e.getMessage(), e);
        }
    }

    @POST
    @Path("create_named_style")
    public void createNamedStyle(@FormParam("id") String id,
            @FormParam("theme_name") String themeName,
            @FormParam("style_name") String styleName) {
        Element element = null;
        if (id == null) {
            element = ThemeManager.getElementById(id);
        }
        try {
            Editor.createNamedStyle(element, styleName, themeName);
        } catch (Exception e) {
            throw new ThemeEditorException(e.getMessage(), e);
        }
    }

    @POST
    @Path("create_style")
    public void createStyle() {
        Element element = getSelectedElement();
        try {
            Editor.createStyle(element);
        } catch (Exception e) {
            throw new ThemeEditorException(e.getMessage(), e);
        }
    }

    @POST
    @Path("delete_element")
    public void deleteElement(@FormParam("id") String id) {
        Element element = ThemeManager.getElementById(id);
        try {
            Editor.deleteElement(element);
        } catch (Exception e) {
            throw new ThemeEditorException(e.getMessage(), e);
        }
    }

    @POST
    @Path("delete_named_style")
    public void deleteNamedStyle(@FormParam("id") String id,
            @FormParam("theme_name") String themeName,
            @FormParam("style_name") String styleName) {
        Element element = id == null ? null : ThemeManager.getElementById(id);
        try {
            Editor.deleteNamedStyle(element, styleName, themeName);
        } catch (Exception e) {
            throw new ThemeEditorException(e.getMessage(), e);
        }
    }

    @POST
    @Path("duplicate_element")
    public String duplicateElement(@FormParam("id") String id) {
        Element element = ThemeManager.getElementById(id);
        try {
            Integer res = Editor.duplicateElement(element);
            return String.valueOf(res);
        } catch (Exception e) {
            throw new ThemeEditorException(e.getMessage(), e);
        }
    }

    @POST
    @Path("insert_fragment")
    public void insertFragment(@FormParam("dest_id") String destId,
            @FormParam("type_name") String typeName,
            @FormParam("style_name") String styleName) {
        Element destElement = ThemeManager.getElementById(destId);
        try {
            Editor.insertFragment(destElement, typeName, styleName);
        } catch (Exception e) {
            throw new ThemeEditorException(e.getMessage(), e);
        }
    }

    @POST
    @Path("insert_section_after")
    public void insertSectionAfter(@FormParam("id") String id) {
        Element element = ThemeManager.getElementById(id);
        try {
            Editor.insertSectionAfter(element);
        } catch (Exception e) {
            throw new ThemeEditorException(e.getMessage(), e);
        }
    }

    @POST
    @Path("select_preset_manager_mode")
    public void selectPresetManagerMode(@FormParam("mode") String mode) {
        SessionManager.setPresetManagerMode(mode);
    }

    @POST
    @Path("select_fragment_type")
    public void selectFragmentType(@FormParam("type") String type) {
        SessionManager.setFragmentType(type);
        SessionManager.setFragmentView(null);
        SessionManager.setFragmentStyle(null);
    }

    @POST
    @Path("select_fragment_view")
    public void selectFragmentView(@FormParam("view") String view) {
        SessionManager.setFragmentView(view);
        SessionManager.setFragmentStyle(null);
    }

    @POST
    @Path("select_fragment_style")
    public void selectFragmentStyle(@FormParam("style") String style) {
        SessionManager.setFragmentStyle(style);
    }

    @POST
    @Path("select_resource_bank")
    public void selectResourceBank(@FormParam("bank") String bankName) {
        SessionManager.setSelectedResourceBank(bankName);
    }

    public static String getSelectedResourceBank() {
        return SessionManager.getSelectedResourceBank();
    }

    @POST
    @Path("add_preset")
    public String addPreset(@FormParam("theme_name") String themeName,
            @FormParam("preset_name") String presetName,
            @FormParam("category") String category,
            @FormParam("value") String value) {
        if (value == null) {
            value = "";
        }
        try {
            return Editor.addPreset(themeName, presetName, category, value);
        } catch (Exception e) {
            throw new ThemeEditorException(e.getMessage(), e);
        }
    }

    @POST
    @Path("convert_to_preset")
    public void convertValueToPreset(@FormParam("theme_name") String themeName,
            @FormParam("preset_name") String presetName,
            @FormParam("category") String category,
            @FormParam("value") String value) {
        if (value == null) {
            throw new ThemeEditorException("Preset value cannot be null");
        }
        try {
            Editor.convertCssValueToPreset(themeName, category, presetName,
                    value);
        } catch (Exception e) {
            throw new ThemeEditorException(e.getMessage(), e);
        }
    }

    @POST
    @Path("edit_preset")
    public void editPreset(@FormParam("theme_name") String themeName,
            @FormParam("preset_name") String presetName,
            @FormParam("value") String value) {
        try {
            Editor.editPreset(themeName, presetName, value);
        } catch (Exception e) {
            throw new ThemeEditorException(e.getMessage(), e);
        }
    }

    @POST
    @Path("update_presets")
    public void updatePresets(@FormParam("theme_name") String themeName,
            @FormParam("property_map") String property_map) {
        Map<String, String> propertyMap = JSONObject.fromObject(property_map);
        try {
            for (Map.Entry<String, String> preset : propertyMap.entrySet()) {
                Editor.editPreset(themeName, preset.getKey(), preset.getValue());
            }
        } catch (Exception e) {
            throw new ThemeEditorException(e.getMessage(), e);
        }
    }

    @POST
    @Path("rename_preset")
    public void renamePreset(@FormParam("theme_name") String themeName,
            @FormParam("old_name") String oldName,
            @FormParam("new_name") String newName) {
        try {
            Editor.renamePreset(themeName, oldName, newName);
        } catch (Exception e) {
            throw new ThemeEditorException(e.getMessage(), e);
        }
    }

    @POST
    @Path("delete_preset")
    public void deletePreset(@FormParam("theme_name") String themeName,
            @FormParam("preset_name") String presetName) {
        try {
            Editor.deletePreset(themeName, presetName);
        } catch (Exception e) {
            throw new ThemeEditorException(e.getMessage(), e);
        }
    }

    @POST
    @Path("make_element_use_named_style")
    public void makeElementUseNamedStyle(@FormParam("id") String id,
            @FormParam("style_name") String styleName,
            @FormParam("theme_name") String themeName) {
        Element element = ThemeManager.getElementById(id);
        try {
            Editor.makeElementUseNamedStyle(element, styleName, themeName);
        } catch (Exception e) {
            throw new ThemeEditorException(e.getMessage(), e);
        }
    }

    @POST
    @Path("set_style_inheritance")
    public void makeStyleInherit(@FormParam("style_name") String styleName,
            @FormParam("ancestor_name") String ancestorName,
            @FormParam("theme_name") String themeName) {
        try {
            Editor.setStyleInheritance(styleName, ancestorName, themeName);
        } catch (Exception e) {
            throw new ThemeEditorException(e.getMessage(), e);
        }
    }

    @POST
    @Path("remove_style_inheritance")
    public void removeStyleInheritance(
            @FormParam("style_name") String styleName,
            @FormParam("theme_name") String themeName) {
        try {
            Editor.removeStyleInheritance(styleName, themeName);
        } catch (Exception e) {
            throw new ThemeEditorException(e.getMessage(), e);
        }
    }

    @POST
    @Path("move_element")
    public void moveElement(@FormParam("src_id") String srcId,
            @FormParam("dest_id") String destId,
            @FormParam("order") Integer order) {
        Element srcElement = ThemeManager.getElementById(srcId);
        Element destElement = ThemeManager.getElementById(destId);
        try {
            Editor.moveElement(srcElement, destElement, order);
        } catch (Exception e) {
            throw new ThemeEditorException(e.getMessage(), e);
        }
    }

    @POST
    @Path("paste_element")
    public void pasteElement(@FormParam("dest_id") String destId) {
        String id = getClipboardElement();
        if (id == null) {
            throw new ThemeEditorException("Nothing to paste");
        }
        Element element = ThemeManager.getElementById(id);
        try {
            Editor.pasteElement(element, destId);
        } catch (Exception e) {
            throw new ThemeEditorException(e.getMessage(), e);
        }
    }

    @POST
    @Path("repair_theme")
    public void repairTheme(@FormParam("src") String src) {
        try {
            Editor.repairTheme(src);
        } catch (Exception e) {
            throw new ThemeEditorException(e.getMessage(), e);
        }
    }

    @POST
    @Path("save_theme")
    public void saveTheme(@FormParam("src") String src) {
        try {
            Editor.saveTheme(src);
        } catch (Exception e) {
            throw new ThemeEditorException(e.getMessage(), e);
        }
    }

    @POST
    @Path("load_theme")
    public void loadTheme(@FormParam("src") String src) {
        try {
            Editor.loadTheme(src);
        } catch (Exception e) {
            throw new ThemeEditorException(e.getMessage(), e);
        }
    }

    @POST
    @Path("delete_theme")
    public void deleteTheme(@FormParam("src") String src) {
        try {
            Editor.deleteTheme(src);
        } catch (Exception e) {
            throw new ThemeEditorException(e.getMessage(), e);
        }
    }

    @POST
    @Path("delete_page")
    public void deletePage(@FormParam("page_path") String pagePath) {
        try {
            Editor.deletePage(pagePath);
        } catch (Exception e) {
            throw new ThemeEditorException(e.getMessage(), e);
        }
    }

    @POST
    @Path("select_preset_group")
    public void selectPresetGroup(@FormParam("group") String group) {
        SessionManager.setPresetGroup(group);
    }

    @POST
    @Path("select_preset_category")
    public void selectPresetCategory(@FormParam("category") String category) {
        SessionManager.setPresetCategory(category);
    }

    @POST
    @Path("select_bank_collection")
    public void selectBankCollection(@FormParam("collection") String collection) {
        SessionManager.setSelectedBankCollection(collection);
    }

    @POST
    @Path("set_page_styles")
    @SuppressWarnings("unchecked")
    public void setPageStyles(@FormParam("theme_name") String themeName,
            @FormParam("property_map") String property_map) {
        Map<String, String> propertyMap = JSONObject.fromObject(property_map);
        try {
            Editor.setPageStyles(themeName, propertyMap);
        } catch (Exception e) {
            throw new ThemeEditorException(e.getMessage(), e);
        }
    }

    @POST
    @Path("select_style_category")
    public void selectStyleCategory(@FormParam("category") String category) {
        SessionManager.setStyleCategory(category);
    }

    @POST
    @Path("select_style_edit_mode")
    public void selectStyleEditMode(@FormParam("mode") String mode) {
        SessionManager.setStyleEditMode(mode);
    }

    @POST
    @Path("toggle_css_category")
    public void toggleCssCategory(@FormParam("name") String name) {
        SessionManager.toggleCssCategory(name);
    }

    @POST
    @Path("collapse_css_categories")
    public void collapseCssCategories() {
        SessionManager.setSelectedCssCategories(new ArrayList<String>());
    }

    @POST
    @Path("expand_css_categories")
    @SuppressWarnings("unchecked")
    public void expandCssCategories() {
        Properties cssStyleCategories = org.nuxeo.theme.editor.Utils.getCssStyleCategories();
        List<String> allCssCategories = (List<String>) Collections.list(cssStyleCategories.propertyNames());
        SessionManager.setSelectedCssCategories(allCssCategories);
    }

    @POST
    @Path("select_style_layer")
    public void selectStyleLayer(@FormParam("uid") String uid) {
        Style layer = (Style) ThemeManager.getFormatById(uid);
        if (layer != null) {
            SessionManager.setStyleLayerId(uid);
        }
    }

    @POST
    @Path("select_named_style")
    public void selectNamedStyle(@FormParam("uid") String uid) {
        Style style = (Style) ThemeManager.getFormatById(uid);
        if (style != null) {
            SessionManager.setNamedStyleId(uid);
        }
    }

    @POST
    @Path("select_style_selector")
    public void selectStyleSelector(@FormParam("selector") String selector) {
        SessionManager.setStyleSelector(selector);
    }

    @POST
    @Path("select_style_manager_mode")
    public void selectStyleManagerMode(@FormParam("mode") String mode) {
        SessionManager.setStyleManagerMode(mode);
    }

    @POST
    @Path("update_element_description")
    public void updateElementDescription(@FormParam("id") String id,
            @FormParam("description") String description) {
        Element element = ThemeManager.getElementById(id);
        try {
            Editor.updateElementDescription(element, description);
        } catch (Exception e) {
            throw new ThemeEditorException(e.getMessage(), e);
        }
    }

    @POST
    @Path("update_element_properties")
    @SuppressWarnings("unchecked")
    public void updateElementProperties(@FormParam("id") String id,
            @FormParam("property_map") String properties) {
        Map<String, String> propertyMap = JSONObject.fromObject(properties);
        Element element = ThemeManager.getElementById(id);
        try {
            Editor.updateElementProperties(element, propertyMap);
        } catch (Exception e) {
            throw new ThemeEditorException(e.getMessage(), e);
        }
    }

    @POST
    @Path("update_element_width")
    public void updateElementWidth(@FormParam("id") String id,
            @FormParam("width") String width) {
        Format layout = ThemeManager.getFormatById(id);
        try {
            Editor.updateElementWidth(layout, width);
        } catch (Exception e) {
            throw new ThemeEditorException(e.getMessage(), e);
        }
    }

    @POST
    @Path("update_element_style_css")
    public void updateElementStyleCss(@FormParam("id") String id,
            @FormParam("view_name") String viewName,
            @FormParam("css_source") String cssSource) {
        Element element = ThemeManager.getElementById(id);
        Style selectedStyleLayer = getSelectedStyleLayer();
        try {
            Editor.updateElementStyleCss(element, selectedStyleLayer, viewName,
                    cssSource);
        } catch (Exception e) {
            throw new ThemeEditorException(e.getMessage(), e);
        }
    }

    @POST
    @Path("update_named_style_css")
    public void updateNamedStyleCss(@FormParam("style_uid") String style_uid,
            @FormParam("css_source") String cssSource,
            @FormParam("theme_name") String themeName) {
        Style style = (Style) ThemeManager.getFormatById(style_uid);
        try {
            Editor.updateNamedStyleCss(style, cssSource, themeName);
        } catch (Exception e) {
            throw new ThemeEditorException(e.getMessage(), e);
        }
    }

    @POST
    @Path("restore_named_style")
    public void restoreNamedStyle(@FormParam("style_uid") String style_uid,
            @FormParam("theme_name") String themeName) {
        Style style = (Style) ThemeManager.getFormatById(style_uid);
        try {
            Editor.restoreNamedStyle(style, themeName);
        } catch (Exception e) {
            throw new ThemeEditorException(e.getMessage(), e);
        }
    }

    @POST
    @Path("split_element")
    public void splitElement(@FormParam("id") String id) {
        Element element = ThemeManager.getElementById(id);
        try {
            Editor.splitElement(element);
        } catch (Exception e) {
            throw new ThemeEditorException(e.getMessage(), e);
        }
    }

    @POST
    @Path("update_element_style")
    @SuppressWarnings("unchecked")
    public void updateElementStyle(@FormParam("id") String id,
            @FormParam("path") String path,
            @FormParam("view_name") String viewName,
            @FormParam("property_map") String property_map) {
        Map<String, String> propertyMap = JSONObject.fromObject(property_map);
        Element element = ThemeManager.getElementById(id);
        Style currentStyleLayer = getSelectedStyleLayer();
        try {
            Editor.updateElementStyle(element, currentStyleLayer, path,
                    viewName, propertyMap);
        } catch (Exception e) {
            throw new ThemeEditorException(e.getMessage(), e);
        }
    }

    @POST
    @Path("update_element_visibility")
    @SuppressWarnings("unchecked")
    public void updateElementVisibility(@FormParam("id") String id,
            @FormParam("perspectives") List<String> perspectives,
            @FormParam("always_visible") Boolean alwaysVisible) {
        Element element = ThemeManager.getElementById(id);
        try {
            Editor.updateElementVisibility(element, perspectives, alwaysVisible);
        } catch (Exception e) {
            throw new ThemeEditorException(e.getMessage(), e);
        }
    }

    @POST
    @Path("update_element_layout")
    @SuppressWarnings("unchecked")
    public void updateElementPadding(
            @FormParam("property_map") String property_map) {
        Map<String, String> propertyMap = JSONObject.fromObject(property_map);
        Element element = getSelectedElement();
        try {
            Editor.updateElementLayout(element, propertyMap);
        } catch (Exception e) {
            throw new ThemeEditorException(e.getMessage(), e);
        }
    }

    @POST
    @Path("update_element_widget")
    public void updateElementWidget(@FormParam("id") String id,
            @FormParam("view_name") String viewName) {
        Element element = ThemeManager.getElementById(id);
        try {
            Editor.updateElementWidget(element, viewName);
        } catch (Exception e) {
            throw new ThemeEditorException(e.getMessage(), e);
        }
    }

    @POST
    @Path("delete_style_view")
    public void deleteStyleView(@FormParam("style_uid") String styleUid,
            @FormParam("view_name") String viewName) {
        Style style = (Style) ThemeManager.getFormatById(styleUid);
        try {
            Editor.deleteStyleView(style, viewName);
        } catch (Exception e) {
            throw new ThemeEditorException(e.getMessage(), e);
        }
    }

    @POST
    @Path("add_theme_to_workspace")
    public void addThemeToWorkspace(@FormParam("name") String name) {
        Set<String> themes = SessionManager.getWorkspaceThemeNames();
        if (!themes.contains(name)) {
            themes.add(name);
        }
        SessionManager.setWorkspaceThemeNames(themes);
    }

    @POST
    @Path("remove_theme_from_workspace")
    public void removeThemeFromWorkspace(@FormParam("name") String name) {
        Set<String> themes = SessionManager.getWorkspaceThemeNames();
        if (themes == null) {
            themes = new HashSet<String>();
        }
        if (themes.contains(name)) {
            themes.remove(name);
        }
        SessionManager.setWorkspaceThemeNames(themes);
    }

    @POST
    @Path("undo")
    public String undo(@FormParam("theme_name") String themeName) {
        try {
            return Editor.undo(themeName);
        } catch (Exception e) {
            throw new ThemeEditorException(e.getMessage(), e);
        }
    }

    /* API */

    public static ThemeDescriptor getThemeDescriptor(String themeName) {
        try {
            return ThemeManager.getThemeDescriptor(themeName);
        } catch (ThemeException e) {
            throw new ThemeEditorException(e.getMessage(), e);
        }
    }

    public static String getSelectedElementId() {
        return SessionManager.getElementId();
    }

    public static Element getSelectedElement() {
        String id = getSelectedElementId();
        if (id == null) {
            return null;
        }
        Element element = ThemeManager.getElementById(id);
        // to avoid possible class cast exceptions
        if (!(element instanceof Element)) {
            return null;
        }
        return element;
    }

    public static String getClipboardElement() {
        return SessionManager.getClipboardElementId();
    }

    public static String getClipboardPreset() {
        return SessionManager.getClipboardPresetId();
    }

    public static List<StyleLayer> getStyleLayersOfSelectedElement() {
        List<StyleLayer> layers = new ArrayList<StyleLayer>();
        Style style = getStyleOfSelectedElement();
        if (style == null) {
            return layers;
        }
        Style selectedStyleLayer = getSelectedStyleLayer();
        layers.add(new StyleLayer("This style", style.getUid(),
                style == selectedStyleLayer || selectedStyleLayer == null));
        for (Format ancestor : ThemeManager.listAncestorFormatsOf(style)) {
            layers.add(new StyleLayer(ancestor.getName(), ancestor.getUid(),
                    ancestor == selectedStyleLayer));
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

    public static List<String> getPerspectivesOfSelectedElement() {
        Element selectedElement = getSelectedElement();
        List<String> perspectives = new ArrayList<String>();
        for (PerspectiveType perspectiveType : Manager.getPerspectiveManager().getPerspectivesFor(
                selectedElement)) {
            perspectives.add(perspectiveType.name);
        }
        return perspectives;
    }

    public static String getStyleEditMode() {
        return SessionManager.getStyleEditMode();
    }

    public static List<String> getStyleSelectorsForSelectedElement() {
        String viewName = getViewNameOfSelectedElement();
        Style style = getStyleOfSelectedElement();
        Style selectedStyleLayer = getSelectedStyleLayer();
        List<String> selectors = new ArrayList<String>();
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
            for (String path : paths) {
                selectors.add(path);
            }
        }
        return selectors;
    }

    public static List<StyleFieldProperty> getStylePropertiesForSelectedElement() {
        Style style = getStyleOfSelectedElement();
        Style selectedStyleLayer = getSelectedStyleLayer();
        if (selectedStyleLayer != null) {
            style = selectedStyleLayer;
        }
        List<StyleFieldProperty> fieldProperties = new ArrayList<StyleFieldProperty>();
        if (style == null) {
            return fieldProperties;
        }
        String path = getSelectedStyleSelector();
        if (path == null) {
            return fieldProperties;
        }
        String viewName = getViewNameOfSelectedElement();
        if (style.getName() != null) {
            viewName = "*";
        }
        Properties properties = style.getPropertiesFor(viewName, path);

        int idx = 0;
        Properties cssProperties = org.nuxeo.theme.html.CSSUtils.getCssProperties();
        if (properties != null) {
            Enumeration<?> propertyNames = properties.propertyNames();
            while (propertyNames.hasMoreElements()) {
                String name = (String) propertyNames.nextElement();
                String value = properties == null ? ""
                        : properties.getProperty(name, "");
                String type = cssProperties.getProperty(name, "");
                String id = "p" + idx;
                fieldProperties.add(new StyleFieldProperty(name, value, type,
                        id));
                idx += 1;
            }
        }
        return fieldProperties;
    }

    public static Map<String, List<StyleFieldProperty>> getAvailableStylePropertiesForSelectedElement() {
        String viewName = getViewNameOfSelectedElement();
        Style style = getStyleOfSelectedElement();
        Style selectedStyleLayer = getSelectedStyleLayer();
        if (selectedStyleLayer != null) {
            style = selectedStyleLayer;
        }
        Map<String, List<StyleFieldProperty>> styleFieldProperties = new LinkedHashMap<String, List<StyleFieldProperty>>();
        if (style == null) {
            return styleFieldProperties;
        }
        String path = getSelectedStyleSelector();
        if (path == null) {
            return styleFieldProperties;
        }
        if (style.getName() != null) {
            viewName = "*";
        }
        Properties styleProperties = style.getPropertiesFor(viewName, path);
        Properties cssProperties = org.nuxeo.theme.html.CSSUtils.getCssProperties();
        Properties cssStyleCategories = org.nuxeo.theme.editor.Utils.getCssStyleCategories();
        Enumeration<?> cssStyleCategoryNames = cssStyleCategories.propertyNames();

        int idx = 0;
        while (cssStyleCategoryNames.hasMoreElements()) {
            String cssStyleCategoryName = (String) cssStyleCategoryNames.nextElement();
            List<StyleFieldProperty> fieldProperties = new ArrayList<StyleFieldProperty>();
            for (String name : cssStyleCategories.getProperty(
                    cssStyleCategoryName).split(",")) {
                String value = styleProperties == null ? ""
                        : styleProperties.getProperty(name, "");
                String type = cssProperties.getProperty(name, "");
                String id = "s" + idx;
                fieldProperties.add(new StyleFieldProperty(name, value, type,
                        id));
                idx += 1;
            }
            styleFieldProperties.put(cssStyleCategoryName, fieldProperties);
        }
        return styleFieldProperties;
    }

    public static String getInheritedStyleNameOfSelectedElement() {
        Style style = getStyleOfSelectedElement();
        Style ancestor = (Style) ThemeManager.getAncestorFormatOf(style);
        if (ancestor != null) {
            return ancestor.getName();
        }
        return "";
    }

    public static String getSelectedStyleSelector() {
        return SessionManager.getStyleSelector();
    }

    public static Style getSelectedStyleLayer() {
        String selectedStyleLayerId = getSelectedStyleLayerId();
        if (selectedStyleLayerId == null) {
            return null;
        }
        Format format = ThemeManager.getFormatById(selectedStyleLayerId);
        if (!(format instanceof Style)) {
            return null;
        }
        return (Style) format;
    }

    public static String getSelectedStyleLayerId() {
        return SessionManager.getStyleLayerId();
    }

    public static List<String> getSelectedCssCategories() {
        return SessionManager.getSelectedCssCategories();
    }

    public static String getSelectedNamedStyleId() {
        return SessionManager.getNamedStyleId();
    }

    public static Style getSelectedNamedStyle() {
        String selectedNamedStyleId = getSelectedNamedStyleId();
        if (selectedNamedStyleId == null) {
            return null;
        }
        Format format = ThemeManager.getFormatById(selectedNamedStyleId);
        if (!(format instanceof Style)) {
            return null;
        }
        return (Style) format;
    }

    public static Style getStyleOfSelectedElement() {
        Element element = getSelectedElement();
        if (element == null) {
            return null;
        }
        FormatType styleType = (FormatType) Manager.getTypeRegistry().lookup(
                TypeFamily.FORMAT, "style");
        return (Style) ElementFormatter.getFormatByType(element, styleType);
    }

    public static PaddingInfo getPaddingOfSelectedElement() {
        Element element = getSelectedElement();
        String top = "";
        String bottom = "";
        String left = "";
        String right = "";
        if (element != null) {
            Layout layout = (Layout) ElementFormatter.getFormatFor(element,
                    "layout");
            top = layout.getProperty("padding-top");
            bottom = layout.getProperty("padding-bottom");
            left = layout.getProperty("padding-left");
            right = layout.getProperty("padding-right");
        }
        return new PaddingInfo(top, bottom, left, right);
    }

    public static String getRenderedStylePropertiesForSelectedElement() {
        Style style = getStyleOfSelectedElement();
        Style currentStyleLayer = getSelectedStyleLayer();
        if (currentStyleLayer != null) {
            style = currentStyleLayer;
        }
        if (style == null) {
            return "";
        }
        List<String> viewNames = new ArrayList<String>();
        String viewName = getViewNameOfSelectedElement();
        if (style.getName() != null) {
            viewName = "*";
        }
        viewNames.add(viewName);
        boolean IGNORE_VIEW_NAME = true;
        boolean IGNORE_CLASSNAME = true;
        boolean INDENT = true;
        return org.nuxeo.theme.html.CSSUtils.styleToCss(style, viewNames,
                IGNORE_VIEW_NAME, IGNORE_CLASSNAME, INDENT);
    }

    public static String getRenderedPropertiesForNamedStyle(Style style) {
        if (style == null) {
            return "";
        }
        boolean IGNORE_VIEW_NAME = false;
        boolean IGNORE_CLASSNAME = true;
        boolean INDENT = true;
        return org.nuxeo.theme.html.CSSUtils.styleToCss(style,
                style.getSelectorViewNames(), IGNORE_VIEW_NAME,
                IGNORE_CLASSNAME, INDENT);
    }

    public static Widget getWidgetOfSelectedElement() {
        Element element = getSelectedElement();
        if (element == null) {
            return null;
        }
        FormatType widgetType = (FormatType) Manager.getTypeRegistry().lookup(
                TypeFamily.FORMAT, "widget");
        return (Widget) ElementFormatter.getFormatByType(element, widgetType);
    }

    public static String getViewNameOfSelectedElement() {
        Widget widget = getWidgetOfSelectedElement();
        if (widget == null) {
            return "";
        }
        return widget.getName();
    }

    public static List<String> getViewNamesForSelectedElement(
            String applicationPath) {
        Element selectedElement = getSelectedElement();
        String templateEngine = getTemplateEngine(applicationPath);
        List<String> viewNames = new ArrayList<String>();
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
            if (!"*".equals(viewName)
                    && templateEngine.equals(viewTemplateEngine)) {
                viewNames.add(viewName);
            }
        }
        return viewNames;
    }

    public static List<FieldProperty> getSelectedElementProperties() {
        Element selectedElement = getSelectedElement();
        return org.nuxeo.theme.editor.Utils.getPropertiesOf(selectedElement);
    }

    /* Presets */

    public static List<String> getPresetGroupsForSelectedCategory() {
        return getPresetGroups(getSelectedStyleCategory());
    }

    public static List<String> getPresetGroups(String category) {
        List<String> groups = new ArrayList<String>();
        List<String> groupNames = new ArrayList<String>();
        for (PresetType preset : PresetManager.getGlobalPresets(null, category)) {
            String group = preset.getGroup();
            if (!groupNames.contains(group)) {
                groups.add(group);
            }
            groupNames.add(group);
        }
        return groups;
    }

    public static List<PresetType> getGlobalPresets(String group) {
        return new ArrayList<PresetType>(PresetManager.getGlobalPresets(group,
                null));
    }

    public static List<PresetType> getCustomPresets(String themeName,
            String category) {
        if ("".equals(category)) {
            category = null;
        }
        return new ArrayList<PresetType>(PresetManager.getCustomPresets(
                themeName, category));
    }

    public List<PresetType> getPresetsForSelectedGroup(String applicationPath,
            String name) {
        String category = getSelectedStyleCategory();
        String group = getSelectedPresetGroup();
        String themeName = getCurrentThemeName(applicationPath, name);
        List<PresetType> presetTypes = group == null ? PresetManager.getCustomPresets(
                themeName, category)
                : PresetManager.getGlobalPresets(group, category);
        return new ArrayList<PresetType>(presetTypes);
    }

    public static String getPresetManagerMode() {
        return SessionManager.getPresetManagerMode();
    }

    public static String getStyleManagerMode() {
        return SessionManager.getStyleManagerMode();
    }

    // TODO: use CSSUtils.expandVariables(...)
    public String resolveVariables(String themeName, String resourceBankName,
            List<ImageInfo> images, String value) {
        if (images == null) {
            return value;
        }

        String contextPath = VirtualHostHelper.getContextPathProperty();

        // basePath
        String basePath = ctx.getBasePath();
        value = value.replaceAll("\\$\\{basePath\\}",
                Matcher.quoteReplacement(basePath));

        value = PresetManager.resolvePresets(themeName, value);

        // images
        for (ImageInfo image : images) {
            String path = image.getPath();
            value = value.replaceAll(Matcher.quoteReplacement(path),
                    Matcher.quoteReplacement(String.format(
                            "%s/nxthemes-images/%s/%s", contextPath,
                            resourceBankName, path.replace(" ", "%20"))));
        }
        return value;
    }

    public static List<String> getUnidentifiedPresetNames(String themeName) {
        return PresetManager.getUnidentifiedPresetNames(themeName);
    }

    public static String renderStyleView(Style style, String viewName) {
        List<String> viewNames = new ArrayList<String>();
        viewNames.add(viewName);
        return org.nuxeo.theme.html.CSSUtils.styleToCss(style, viewNames, true,
                true, true);
    }

    public static List<String> getHardcodedColors(String themeName) {
        return Editor.getHardcodedColors(themeName);
    }

    public static List<String> getHardcodedImages(String themeName) {
        return Editor.getHardcodedImages(themeName);
    }

    /* Session */

    public static String getSelectedPresetGroup() {
        return SessionManager.getPresetGroup();
    }

    public static String getSelectedPresetCategory() {
        return SessionManager.getPresetCategory();
    }

    public static String getSelectedBankCollection() {
        return SessionManager.getSelectedBankCollection();
    }

    public static String getSelectedStyleCategory() {
        return SessionManager.getStyleCategory();
    }

    public static String getSelectedFragmentType() {
        return SessionManager.getFragmentType();
    }

    public static String getSelectedFragmentView() {
        return SessionManager.getFragmentView();
    }

    public static String getSelectedFragmentStyle() {
        return SessionManager.getFragmentStyle();
    }

    public static String getTemplateEngine(String applicationPath) {
        return ThemeManager.getTemplateEngineName(applicationPath);
    }

    public static String getDefaultTheme(String applicationPath, String name) {
        String defaultTheme = ThemeManager.getDefaultTheme(applicationPath);
        if (defaultTheme == null || defaultTheme.equals("")) {
            String moduleName = WebEngine.getActiveContext().getModule().getName();
            defaultTheme = ThemeManager.getDefaultTheme(applicationPath, name,
                    moduleName);
        }
        return defaultTheme;
    }

    public static String getCurrentPagePath(String applicationPath, String name) {
        String defaultTheme = getDefaultTheme(applicationPath, name);
        String currentPagePath = WebEngine.getActiveContext().getCookie(
                "nxthemes.theme");
        if (currentPagePath == null) {
            currentPagePath = defaultTheme;
        }
        return currentPagePath;
    }

    public static String getCurrentThemeName(String applicationPath, String name) {
        String defaultTheme = getDefaultTheme(applicationPath, name);
        String currentPagePath = WebEngine.getActiveContext().getCookie(
                "nxthemes.theme");
        if (currentPagePath == null) {
            return defaultTheme.split("/")[0];
        }
        return currentPagePath.split("/")[0];
    }

    public static List<PageInfo> getPages(String applicationPath, String name) {
        ThemeManager themeManager = Manager.getThemeManager();
        String currentPagePath = WebEngine.getActiveContext().getCookie(
                "nxthemes.theme");
        String defaultTheme = getDefaultTheme(applicationPath, name);
        String defaultPageName = defaultTheme.split("/")[1];

        List<PageInfo> pages = new ArrayList<PageInfo>();
        if (currentPagePath == null || !currentPagePath.contains("/")) {
            currentPagePath = defaultTheme;
        }

        String currentThemeName = currentPagePath.split("/")[0];
        String currentPageName = currentPagePath.split("/")[1];
        ThemeElement currentTheme = themeManager.getThemeByName(currentThemeName);

        if (currentTheme == null) {
            return pages;
        }

        boolean first = true;
        for (PageElement page : ThemeManager.getPagesOf(currentTheme)) {
            String pageName = page.getName();
            String link = String.format("%s/%s", currentThemeName, pageName);
            String className = pageName.equals(currentPageName) ? "selected"
                    : "";
            if (defaultPageName.equals(pageName)) {
                className += " default";
            }
            if (first) {
                className += " first";
                first = false;
            }
            pages.add(new PageInfo(pageName, link, className));
        }
        return pages;
    }

    public static List<ThemeInfo> getThemes(String applicationPath, String name) {
        List<ThemeInfo> themes = new ArrayList<ThemeInfo>();
        String defaultTheme = getDefaultTheme(applicationPath, name);
        String defaultPageName = defaultTheme.split("/")[1];
        String templateEngine = getTemplateEngine(applicationPath);
        for (String themeName : ThemeManager.getThemeNames(templateEngine)) {
            String path = String.format("%s/%s", themeName, defaultPageName);
            Boolean selected = false;
            themes.add(new ThemeInfo(themeName, path, selected));
        }
        return themes;
    }

    public static ThemeManager getThemeManager() {
        return Manager.getThemeManager();
    }

    public static Style getThemeSkin(String themeName) {
        return Editor.getThemeSkin(themeName);
    }

    public static Set<ThemeInfo> getWorkspaceThemes(String path, String name) {
        String currentThemeName = getCurrentThemeName(path, name);
        String templateEngine = getTemplateEngine(path);
        Set<String> workspaceThemeNames = SessionManager.getWorkspaceThemeNames();
        Set<ThemeInfo> workspaceThemes = new LinkedHashSet<ThemeInfo>();
        Set<String> compatibleThemes = ThemeManager.getThemeNames(templateEngine);
        if (!workspaceThemeNames.contains(currentThemeName)) {
            workspaceThemeNames.add(currentThemeName);
        }
        for (String themeName : workspaceThemeNames) {
            if (compatibleThemes.contains(themeName)) {
                String pagePath = String.format("%s/default", themeName);
                workspaceThemes.add(new ThemeInfo(themeName, pagePath,
                        themeName == currentThemeName));
            }
        }
        return workspaceThemes;
    }

    public static void createFragmentPreview(String currentThemeName) {
        Editor.createFragmentPreview(currentThemeName);
    }

    public static SkinInfo getSkinInfo(String bankName, String skinName) {
        return Editor.getSkinInfo(bankName, skinName);
    }

    /*
     * Forms
     */
    @POST
    @Path("select_edit_field")
    public void selectEditField(@FormParam("field_name") String fieldName) {
        SessionManager.setSelectedEditField(fieldName);
    }

    public static String getSelectedEditField() {
        return SessionManager.getSelectedEditField();
    }

    public static List<Style> listNamedStylesDirectlyInheritingFrom(Style style) {
        return Editor.listNamedStylesDirectlyInheritingFrom(style);
    }

}
