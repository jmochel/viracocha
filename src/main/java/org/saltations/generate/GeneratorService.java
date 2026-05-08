package org.saltations.generate;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.Version;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.saltations.config.ConfigService;
import org.saltations.model.MappingEntry;
import org.saltations.model.ArchetypeEntry;
import org.saltations.model.ProjectEntry;
import org.saltations.model.ViracochaConfig;
import org.saltations.infra.HiddenPathFilter;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Walks archetype trees, merges parameters, expands path segments and template bodies, and writes
 * into the project workspace (or simulates with {@code dryRun}).
 */
@Singleton
public class GeneratorService {

    private static final Version FM_VERSION = Configuration.VERSION_2_3_34;

    private final ConfigService configService;
    private final PathExpander pathExpander;
    private final Configuration fmContentConfig;

    @Inject
    public GeneratorService(ConfigService configService, PathExpander pathExpander) {
        this.configService = configService;
        this.pathExpander = pathExpander;
        this.fmContentConfig = new Configuration(FM_VERSION);
        this.fmContentConfig.setDefaultEncoding("UTF-8");
        this.fmContentConfig.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
    }

    /**
     * @param verbose when true, populate {@link GenerationResult#verboseLines()} with workspace-relative paths
     */
    public GenerationResult generate(String projectName, boolean dryRun, boolean verbose) throws IOException {
        ViracochaConfig config = configService.load();
        ProjectEntry project = config.getProjects().stream()
            .filter(p -> p.getName().equals(projectName))
            .findFirst()
            .orElse(null);
        if (project == null) {
            throw new IllegalArgumentException("Project '" + projectName + "' not found.");
        }

        Path workspace = Path.of(project.getPath()).normalize().toAbsolutePath();
        int generated = 0;
        int skipped = 0;
        int failed = 0;
        List<String> verboseLines = verbose ? new ArrayList<>() : null;

        for (MappingEntry mapping : project.getMappings()) {
            ArchetypeEntry archetype = config.getArchetypes().stream()
                .filter(pe -> pe.getName().equals(mapping.getArchetypeName()))
                .findFirst()
                .orElse(null);
            if (archetype == null) {
                throw new IllegalArgumentException("Archetype '" + mapping.getArchetypeName() + "' not found in config.");
            }

            Map<String, String> model = mergeParameters(project, mapping);
            Path archetypeRoot = Path.of(archetype.getPath()).normalize().toAbsolutePath();

            Path destRoot;
            try {
                destRoot = resolveMappingWorkspacePath(workspace, mapping.getWorkspacePath());
            } catch (IllegalArgumentException e) {
                failed++;
                if (verbose) {
                    verboseLines.add("Failed " + mapping.getWorkspacePath() + " (" + e.getMessage() + ")");
                }
                continue;
            }

            List<Path> files;
            try (Stream<Path> walk = Files.walk(archetypeRoot)) {
                files = walk
                    .filter(Files::isRegularFile)
                    .filter(p -> !HiddenPathFilter.hasHiddenPathSegment(archetypeRoot, p))
                    .sorted(Comparator.comparing(p -> archetypeRoot.relativize(p).toString()))
                    .collect(Collectors.toList());
            }

            for (Path sourceFile : files) {
                Path relFromArchetype = archetypeRoot.relativize(sourceFile);

                Path expandedRel;
                try {
                    expandedRel = expandRelativePath(relFromArchetype, model);
                } catch (IllegalArgumentException e) {
                    failed++;
                    if (verbose) {
                        String hint = Path.of(mapping.getWorkspacePath()).resolve(relFromArchetype).toString().replace('\\', '/');
                        verboseLines.add("Failed " + hint + " (path expansion: " + e.getMessage() + ")");
                    }
                    continue;
                }

                Path target = destRoot.resolve(expandedRel).normalize().toAbsolutePath();

                if (!target.startsWith(workspace)) {
                    failed++;
                    if (verbose) {
                        verboseLines.add("Failed " + workspace.relativize(target).toString().replace('\\', '/')
                            + " (escapes workspace)");
                    }
                    continue;
                }

                String relToWs = workspace.relativize(target).toString().replace('\\', '/');

                if (Files.exists(target)) {
                    if (Files.isRegularFile(target)) {
                        skipped++;
                        if (verbose) {
                            verboseLines.add("Skipped " + relToWs);
                        }
                        continue;
                    }
                    if (Files.isDirectory(target)) {
                        failed++;
                        if (verbose) {
                            verboseLines.add("Failed " + relToWs + " (exists as directory)");
                        }
                        continue;
                    }
                }

                Path parent = target.getParent();
                if (parent != null && Files.exists(parent) && !Files.isDirectory(parent)) {
                    failed++;
                    if (verbose) {
                        verboseLines.add("Failed " + relToWs + " (parent path blocked by file)");
                    }
                    continue;
                }

                String content;
                try {
                    content = Files.readString(sourceFile, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    failed++;
                    if (verbose) {
                        verboseLines.add("Failed " + relToWs + " (read: " + e.getMessage() + ")");
                    }
                    continue;
                }

                String rendered;
                try {
                    rendered = renderTemplate(sourceFile.toString(), content, model);
                } catch (IOException | TemplateException e) {
                    failed++;
                    if (verbose) {
                        verboseLines.add("Failed " + relToWs + " (template: " + e.getMessage() + ")");
                    }
                    continue;
                }

                if (!dryRun) {
                    try {
                        if (parent != null) {
                            Files.createDirectories(parent);
                        }
                        Files.writeString(target, rendered, StandardCharsets.UTF_8);
                    } catch (IOException e) {
                        failed++;
                        if (verbose) {
                            verboseLines.add("Failed " + relToWs + " (write: " + e.getMessage() + ")");
                        }
                        continue;
                    }
                }

                generated++;
                if (verbose) {
                    verboseLines.add("Created " + relToWs);
                }
            }
        }

        return new GenerationResult(generated, skipped, failed, verbose ? List.copyOf(verboseLines) : List.of());
    }

    private Map<String, String> mergeParameters(ProjectEntry project, MappingEntry mapping) {
        Map<String, String> merged = new LinkedHashMap<>();
        if (project.getParameters() != null) {
            merged.putAll(project.getParameters());
        }
        if (mapping.getParameters() != null) {
            mapping.getParameters().forEach(merged::put);
        }
        return merged;
    }

    private Path resolveMappingWorkspacePath(Path projectWorkspace, String workspacePath) {
        if (workspacePath == null || workspacePath.isBlank()) {
            throw new IllegalArgumentException("Mapping workspace path must not be empty.");
        }
        Path destRoot = projectWorkspace.resolve(workspacePath).normalize().toAbsolutePath();
        if (!destRoot.startsWith(projectWorkspace)) {
            throw new IllegalArgumentException("Mapping workspace path escapes project workspace.");
        }
        return destRoot;
    }

    private Path expandRelativePath(Path relativePath, Map<String, String> model) {
        Path acc = Path.of(pathExpander.expandSegment(relativePath.getName(0).toString(), model));
        for (int i = 1; i < relativePath.getNameCount(); i++) {
            String seg = relativePath.getName(i).toString();
            acc = acc.resolve(pathExpander.expandSegment(seg, model));
        }
        return acc;
    }

    private String renderTemplate(String sourceName, String content, Map<String, String> model)
        throws IOException, TemplateException {
        Template template = new Template(sourceName, new StringReader(content), fmContentConfig);
        StringWriter out = new StringWriter();
        template.process(model, out);
        return out.toString();
    }
}
