
import org.nuxeo.theme.Manager
import org.nuxeo.theme.themes.ThemeManager
import org.nuxeo.theme.presets.PresetType
import org.nuxeo.theme.types.TypeFamily
import org.nuxeo.theme.types.Type
import org.nuxeo.theme.editor.StyleCategory
import org.nuxeo.theme.editor.PresetInfo

category = Context.getCookie("nxthemes.editor.styleCategory");
group = Context.getCookie("nxthemes.editor.presetGroup");

presets = []

for (type in Manager.getTypeRegistry().getTypes(TypeFamily.PRESET)) {
    PresetType preset = (PresetType) type;
    if (!preset.getCategory().equals(category)) {
        continue;
    }
    if (!preset.getGroup().equals(group)) {
        continue;
    }
    presets.add(new PresetInfo(preset));
}

return presets;
