package io.huskit.gradle.plugin;

import io.huskit.gradle.plugin.internal.ApplyInternalPluginLogic;
import io.huskit.gradle.plugin.internal.InternalEnvironment;
import io.huskit.gradle.plugin.internal.InternalProperties;
import lombok.RequiredArgsConstructor;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.VersionCatalogsExtension;

import javax.inject.Inject;
import java.util.Objects;

@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class InternalConventionPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        var extensions = project.getExtensions();
        var providers = project.getProviders();
        var environment = (InternalEnvironment) Objects.requireNonNullElseGet(
                extensions.findByName(InternalEnvironment.name()),
                () -> new InternalEnvironment(
                        providers.environmentVariable("CI").isPresent(),
                        false
                ));
        var properties = (InternalProperties) Objects.requireNonNullElseGet(
                extensions.findByName(InternalProperties.name()),
                () -> new InternalProperties(((VersionCatalogsExtension) extensions.getByName("versionCatalogs")).named("libs")));
        var huskitConventionExtension = (HuskitInternalConventionExtension) Objects.requireNonNullElseGet(
                extensions.findByName(HuskitInternalConventionExtension.name()),
                () -> extensions.create(HuskitInternalConventionExtension.name(), HuskitInternalConventionExtension.class));
        huskitConventionExtension.getIntegrationTestName().convention("integrationTest");
        new ApplyInternalPluginLogic(
                project.getPath(),
                project.getPluginManager(),
                project.getRepositories(),
                project.getDependencies(),
                extensions,
                huskitConventionExtension,
                project.getComponents(),
                project.getTasks(),
                project.getConfigurations(),
                environment,
                properties,
                project.getName(),
                project.getGradle().getSharedServices(),
                project.getLayout(),
                project.getRootDir(),
                runnable -> project.afterEvaluate(ignore -> runnable.run())
        ).apply();
    }
}
