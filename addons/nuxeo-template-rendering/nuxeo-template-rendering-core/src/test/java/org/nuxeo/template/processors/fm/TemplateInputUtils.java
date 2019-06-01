package org.nuxeo.template.processors.fm;

import org.jetbrains.annotations.NotNull;
import org.nuxeo.template.api.InputType;
import org.nuxeo.template.api.TemplateInput;

import java.util.Date;

public class TemplateInputUtils {

    @NotNull
    public static TemplateInput createContentTemplateInput(String paramName, String targetProperty) {
        TemplateInput param = new TemplateInput(paramName, null);
        param.setType(InputType.Content);
        param.setSource(targetProperty);
        return param;
    }

    @NotNull
    public static TemplateInput createPicturePropertyTemplateProperty(String paramName, String targetProperty) {
        return createPicturePropertyTemplateProperty(paramName, targetProperty, false);
    }

    @NotNull
    public static TemplateInput createPicturePropertyTemplateProperty(String paramName, String targetProperty, boolean autoloop) {
        TemplateInput param = new TemplateInput(paramName, null);
        param.setType(InputType.PictureProperty);
        param.setSource(targetProperty);
        param.setAutoLoop(autoloop);
        return param;
    }

    @NotNull
    public static TemplateInput createDocumentPropertyTemplateInput(String paramName, String targetProperty) {
        return createDocumentPropertyTemplateInput(paramName, targetProperty, false);
    }

    @NotNull
    public static TemplateInput createDocumentPropertyTemplateInput(String paramName, String targetProperty, boolean autoloop) {
        TemplateInput param = new TemplateInput(paramName, null);
        param.setType(InputType.DocumentProperty);
        param.setSource(targetProperty);
        param.setAutoLoop(autoloop);
        return param;
    }

    @NotNull
    public static TemplateInput createStringTemplateInput(String paramName, String value) {
        TemplateInput param = new TemplateInput(paramName, value);
        param.setType(InputType.StringValue);
        return param;
    }

    @NotNull
    public static TemplateInput createBooleanTemplateInput(String paramName, Boolean value) {
        TemplateInput param = new TemplateInput(paramName, value);
        param.setType(InputType.BooleanValue);
        return param;
    }

    @NotNull
    public static TemplateInput createDateTemplateInput(String paramName, Date date) {
        TemplateInput param = new TemplateInput(paramName, date);
        param.setType(InputType.DateValue);
        return param;
    }
}
