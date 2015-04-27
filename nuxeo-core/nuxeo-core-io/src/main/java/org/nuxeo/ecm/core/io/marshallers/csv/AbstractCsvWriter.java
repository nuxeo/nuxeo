package org.nuxeo.ecm.core.io.marshallers.csv;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

import org.nuxeo.ecm.core.io.registry.MarshallerRegistry;
import org.nuxeo.ecm.core.io.registry.Writer;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.core.io.registry.reflect.Supports;

@Supports("text/csv")
public abstract class AbstractCsvWriter<EntityType> implements Writer<EntityType> {

    /**
     * The current {@link RenderingContext}.
     */
    @Inject
    protected RenderingContext ctx;

    /**
     * The marshaller registry.
     */
    @Inject
    protected MarshallerRegistry registry;

    @Override
    public boolean accept(Class<?> clazz, Type genericType, MediaType mediatype) {
        return true;
    }

    @Override
    public void write(EntityType entity, Class<?> clazz, Type genericType, MediaType mediatype, OutputStream out)
            throws IOException {
        List<CsvContributor<?>> generator = ctx.getParameter("_CSV_COLUMNS");
        if (generator == null) {
            generator = getColumns();
        }
        boolean headerWrited = ctx.getBooleanParameter("_CSV_HEADER_WRITED");
        if (!headerWrited) {
            boolean first = true;
            for (CsvContributor<?> columns : generator) {
                for (String column : columns.getHeaders()) {
                    if (first) {
                        first = false;
                    } else {
                        out.write(';');
                    }
                    out.write(escape(column).getBytes());
                }
            }
            ctx.setParameterValues("_CSV_HEADER_WRITED", true);
        }
        newLine(out);
        write(entity, generator, out);
        out.flush();
    }

    public void write(EntityType entity, List<CsvContributor<?>> generator, OutputStream out)
            throws IOException {
        boolean first = true;
        for (CsvContributor<?> columns : generator) {
            Map<String, String> values = columns.getValues(entity);
            for (String column : columns.getHeaders()) {
                if (first) {
                    first = false;
                } else {
                    out.write(';');
                }
                String value = values.get(column);
                value = value != null ? value : "";
                out.write(escape(value).getBytes());
            }
        }
    }

    public void newLine(OutputStream out) throws IOException {
        out.write('\n');
        out.flush();
    }

    private final String escape(String column) {
        return column;
    }

    public abstract List<CsvContributor<?>> getColumns();

}
