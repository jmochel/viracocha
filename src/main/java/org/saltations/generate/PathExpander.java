package org.saltations.generate;

import jakarta.inject.Singleton;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.Version;
import freemarker.cache.StringTemplateLoader;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

/**
 * Expands Freemarker expressions in a single path segment string (e.g. {@code foo-${bar}-baz})
 * using a string-keyed model. Uses the same {@code ${name}} style as path scanning in
 * {@link org.saltations.infra.FreemarkerVariableExtractor}; full Freemarker template syntax
 * in the segment is supported via Freemarker processing.
 */
@Singleton
public final class PathExpander {

    private static final Version FM_VERSION = Configuration.VERSION_2_3_34;

    /**
     * Expands one path segment using the given model. Missing variables cause Freemarker to throw;
     * this method wraps failures as {@link IllegalArgumentException} with the segment text attached.
     *
     * @param segment a single path segment (not a full path with separators)
     * @param model variable names to string values; empty map if none
     */
    public String expandSegment(String segment, Map<String, String> model) {
        if (segment == null) {
            throw new IllegalArgumentException("segment must not be null");
        }
        Map<String, String> root = model != null ? model : Map.of();
        var loader = new StringTemplateLoader();
        loader.putTemplate("seg", segment);
        var cfg = new Configuration(FM_VERSION);
        cfg.setTemplateLoader(loader);
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        try {
            var template = cfg.getTemplate("seg");
            var out = new StringWriter();
            template.process(root, out);
            return out.toString();
        } catch (TemplateException | IOException e) {
            throw new IllegalArgumentException("Failed to expand path segment: " + segment, e);
        }
    }
}
