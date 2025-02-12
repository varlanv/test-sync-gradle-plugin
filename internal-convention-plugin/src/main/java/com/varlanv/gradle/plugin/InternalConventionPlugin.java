package com.varlanv.gradle.plugin;

import lombok.RequiredArgsConstructor;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.VersionCatalogsExtension;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.plugins.jvm.JvmTestSuite;
import org.gradle.api.plugins.quality.*;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.VariantVersionMappingStrategy;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.gradle.jvm.toolchain.JvmVendorSpec;
import org.gradle.plugin.devel.plugins.JavaGradlePluginPlugin;
import org.gradle.plugins.ide.idea.IdeaPlugin;
import org.gradle.plugins.ide.idea.model.IdeaModel;
import org.gradle.plugins.signing.SigningExtension;
import org.gradle.plugins.signing.SigningPlugin;
import org.gradle.testing.base.TestingExtension;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("UnstableApiUsage")
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class InternalConventionPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        // -------------------- Variables start --------------------
        var extensions = project.getExtensions();
        var providers = project.getProviders();
        var pluginManager = project.getPluginManager();
        var tasks = project.getTasks();
        var projectName = project.getName();
        var repositories = project.getRepositories();
        var dependencies = project.getDependencies();
        var components = project.getComponents();
        var configurations = project.getConfigurations();
        var rootDir = project.getRootDir().toPath();
        var projectLayout = project.getLayout();
        var projectPath = project.getPath();
        var internalEnvironment = Optional.ofNullable((InternalEnvironment) extensions.findByName(InternalEnvironment.name()))
            .orElseGet(() -> new InternalEnvironment(
                providers.environmentVariable("CI").isPresent(),
                false
            ));
        var internalProperties = Optional.ofNullable((InternalProperties) extensions.findByName(InternalProperties.name()))
            .orElseGet(() -> new InternalProperties(((VersionCatalogsExtension) extensions.getByName("versionCatalogs")).named("libs")));
        var internalConventionExtension = Optional.ofNullable((InternalConventionExtension) extensions.findByName(InternalConventionExtension.name()))
            .orElseGet(() -> extensions.create(InternalConventionExtension.name(), InternalConventionExtension.class));
        internalConventionExtension.getIntegrationTestName().convention("integrationTest");
        internalConventionExtension.getInternalModule().convention(false);
        var isGradlePlugin = projectName.endsWith("plugin");
        var javaVersion = 8;
        var jdkVersion = 21;
        var internalJavaVersion = 21;
        var jvmVendor = JvmVendorSpec.AZUL;
        // -------------------- Variables end --------------------


        // -------------------- Configure repositories start --------------------
        if (internalEnvironment.isLocal()) {
            repositories.add(repositories.mavenLocal());
        }
        repositories.add(repositories.mavenCentral());
        // -------------------- Configure repositories end --------------------

        // -------------------- Apply common plugins start --------------------
        if (isGradlePlugin) {
            pluginManager.apply(JavaGradlePluginPlugin.class);
        }
        pluginManager.withPlugin(
            "java",
            ignore -> {
                pluginManager.apply(PmdPlugin.class);
                pluginManager.apply(CheckstylePlugin.class);
                if (internalEnvironment.isLocal()) {
                    pluginManager.apply(IdeaPlugin.class);
                    var idea = (IdeaModel) extensions.getByName("idea");
                    idea.getModule().setDownloadJavadoc(true);
                    idea.getModule().setDownloadSources(true);
                }
            }
        );
        // -------------------- Apply common plugins end --------------------

        project.afterEvaluate(ignore -> {
                // Need to run these things after project evaluate, so that InternalConventionExtension values are initialized
                // -------------------- Configure Java start --------------------
                pluginManager.withPlugin(
                    "java",
                    plugin -> {
                        var java = (JavaPluginExtension) extensions.getByName("java");
                        java.withSourcesJar();
                        if (internalEnvironment.isCi()) {
                            java.setSourceCompatibility(JavaLanguageVersion.of(javaVersion));
                            java.setTargetCompatibility(JavaLanguageVersion.of(javaVersion));
                        } else {
                            java.toolchain((spec -> {
                                spec.getLanguageVersion().set(JavaLanguageVersion.of(internalConventionExtension.getInternalModule().get() ? internalJavaVersion : jdkVersion));
                                spec.getVendor().set(jvmVendor);
                            }));
                        }
                        tasks.named("compileJava", JavaCompile.class).configure(javaCompile -> {
                            var compileOpts = javaCompile.getOptions();
                            if (!internalConventionExtension.getInternalModule().get()) {
                                compileOpts.getRelease().set(javaVersion);
                            }
                            compileOpts.getCompilerArgs().add("-Xlint:-options");
                        });

                        // -------------------- Add common dependencies start --------------------
                        var jetbrainsAnnotations = internalProperties.getLib("jetbrains-annotations");
                        dependencies.add(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME, jetbrainsAnnotations);
                        dependencies.add(JavaPlugin.TEST_COMPILE_ONLY_CONFIGURATION_NAME, jetbrainsAnnotations);
                        dependencies.add(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME, internalProperties.getLib("assertj-core"));
                        dependencies.add(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME, internalProperties.getLib("junit-jupiter-api"));
                        dependencies.add(JavaPlugin.TEST_RUNTIME_ONLY_CONFIGURATION_NAME, internalProperties.getLib("junit-platform-launcher"));

                        var lombokDependency = internalProperties.getLib("lombok");
                        dependencies.add(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME, lombokDependency);
                        dependencies.add(JavaPlugin.ANNOTATION_PROCESSOR_CONFIGURATION_NAME, lombokDependency);
                        dependencies.add(JavaPlugin.TEST_COMPILE_ONLY_CONFIGURATION_NAME, lombokDependency);
                        dependencies.add(JavaPlugin.TEST_ANNOTATION_PROCESSOR_CONFIGURATION_NAME, lombokDependency);

                        if (!internalEnvironment.isTest() && !projectPath.equals(":common-test")) {
                            dependencies.add(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME, dependencies.project(Collections.singletonMap("path", ":common-test")));
                        }
                        // -------------------- Add common dependencies end --------------------
                    }
                );
                // -------------------- Configure Java end --------------------

                // -------------------- Configure libraries publishing start --------------------
                if (!isGradlePlugin) {
                    pluginManager.withPlugin(
                        "maven-publish",
                        plugin -> {
                            pluginManager.apply(SigningPlugin.class);
                            var publishingExtension = extensions.getByType(PublishingExtension.class);
                            var signingExtension = extensions.getByType(SigningExtension.class);

                            var javaPluginExtension = extensions.getByType(JavaPluginExtension.class);
                            javaPluginExtension.withJavadocJar();
                            javaPluginExtension.withSourcesJar();

                            var createdMavenPublication = publishingExtension.getPublications().create(
                                "mavenJava",
                                MavenPublication.class,
                                mavenPublication -> {
                                    mavenPublication.from(components.getByName("java"));
                                    mavenPublication.versionMapping(
                                        versionMappingStrategy -> {
                                            versionMappingStrategy.usage(
                                                "java-api",
                                                variantVersionMappingStrategy ->
                                                    variantVersionMappingStrategy.fromResolutionOf(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME));
                                            versionMappingStrategy.usage("java-runtime", VariantVersionMappingStrategy::fromResolutionResult);
                                        }
                                    );
                                    mavenPublication.pom(pom -> {
                                        pom.getUrl().set("https://github.com/varlanv/test-sync-gradle-plugin");
                                        pom.licenses(licenses -> {
                                            licenses.license(license -> {
                                                license.getName().set("MIT License");
                                                license.getUrl().set("https://mit-license.org/");
                                            });
                                        });
                                        pom.developers(developers -> {
                                            developers.developer(developer -> {
                                                developer.getId().set("varlanv96");
                                                developer.getName().set("Vladyslav Varlan");
                                                developer.getEmail().set("varlanv96@gmail.com");
                                            });
                                        });
                                    });
                                }
                            );
                            signingExtension.sign(createdMavenPublication);
                        }
                    );
                }
                // -------------------- Configure publishing end --------------------

                // -------------------- Configure tests start --------------------
                pluginManager.withPlugin(
                    "java",
                    plugin -> {
                        var testing = (TestingExtension) extensions.getByName("testing");
                        var suites = testing.getSuites();
                        var integrationTestTaskName = internalConventionExtension.getIntegrationTestName().get();
                        var integrationTestSuite = suites.register(
                            integrationTestTaskName,
                            JvmTestSuite.class
                        );
                        suites.configureEach(
                            suite -> {
                                if (suite instanceof JvmTestSuite jvmTestSuite) {
                                    jvmTestSuite.useJUnitJupiter();
                                    jvmTestSuite.dependencies(
                                        jvmComponentDependencies -> {
                                            var implementation = jvmComponentDependencies.getImplementation();
                                            implementation.add(jvmComponentDependencies.project());
                                        }
                                    );
                                    jvmTestSuite.sources(s -> {
                                        var compileJavaTaskName = s.getCompileJavaTaskName();
                                        tasks.named(compileJavaTaskName, JavaCompile.class).configure(compileTestJava -> {
                                            var compileOpts = compileTestJava.getOptions();
                                            compileOpts.getRelease().set(internalJavaVersion);
                                            compileOpts.getCompilerArgs().add("-Xlint:-options");
                                        });
                                    });
                                    jvmTestSuite.getTargets().all(
                                        target -> target.getTestTask().configure(
                                            test -> {
                                                test.getOutputs().upToDateWhen(task -> false);
                                                test.testLogging(
                                                    logging -> {
                                                        logging.setShowStandardStreams(true);
                                                        logging.setShowStackTraces(true);
                                                    }
                                                );
                                                test.setFailFast(internalEnvironment.isCi());
                                                var environment = new HashMap<>(test.getEnvironment());
                                                environment.put("TESTCONTAINERS_REUSE_ENABLE", "true");
                                                test.setEnvironment(environment);
                                                var memory = test.getName().equals(JavaPlugin.TEST_TASK_NAME) ? "512m" : "1024m";
                                                test.setJvmArgs(
                                                    Stream.of(
                                                            test.getJvmArgs(),
                                                            Arrays.asList("-Xms" + memory, "-Xmx" + memory),
                                                            Arrays.asList("-XX:TieredStopAtLevel=1", "-noverify")
                                                        )
                                                        .flatMap(Collection::stream)
                                                        .collect(Collectors.toList())
                                                );
                                            }
                                        )
                                    );
                                }
                            }
                        );
                        tasks.named("check", task -> task.dependsOn(integrationTestSuite));

                        // configure integration test configurations
                        configurations.named(
                            integrationTestTaskName + "Implementation",
                            configuration -> configuration.extendsFrom(configurations.getByName(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME))
                        );
                        configurations.named(
                            integrationTestTaskName + "AnnotationProcessor",
                            configuration -> configuration.extendsFrom(configurations.getByName(JavaPlugin.TEST_ANNOTATION_PROCESSOR_CONFIGURATION_NAME))
                        );
                        configurations.named(
                            integrationTestTaskName + "CompileOnly",
                            configuration -> configuration.extendsFrom(configurations.getByName(JavaPlugin.TEST_COMPILE_ONLY_CONFIGURATION_NAME))
                        );
                        configurations.named(
                            integrationTestTaskName + "RuntimeOnly",
                            configuration -> configuration.extendsFrom(configurations.getByName(JavaPlugin.TEST_RUNTIME_ONLY_CONFIGURATION_NAME))
                        );
                    }
                );
                // -------------------- Configure tests start --------------------

                // -------------------- Configure static analysis start --------------------
                var staticAnalyseFolder = rootDir.resolve(".config").resolve("static-analyse");

                // Configure aggregate static analysis tasks
                var staticAnalyseMain = tasks.register(
                    "staticAnalyseMain",
                    task -> {
                        task.setGroup("static analysis");
                        task.setDescription("Run static analysis on main sources");
                    }
                );
                var staticAnalyseTest = tasks.register(
                    "staticAnalyseTest",
                    task -> {
                        task.setGroup("static analysis");
                        task.setDescription("Run static analysis on test sources");
                    }
                );
                tasks.register(
                    "staticAnalyseFull",
                    task -> {
                        task.setGroup("static analysis");
                        task.setDescription("Run static analysis on all sources");
                        task.dependsOn(staticAnalyseMain, staticAnalyseTest);
                    }
                );

                tasks.named(
                    "check",
                    task -> {
                        task.dependsOn(staticAnalyseMain);
                    }
                );

                // Configure pmd
                pluginManager.withPlugin(
                    "pmd",
                    pmd -> {
                        var pmdExtension = (PmdExtension) extensions.getByName("pmd");
                        pmdExtension.setRuleSetFiles(
                            projectLayout.files(
                                staticAnalyseFolder.resolve("pmd.xml")
                            )
                        );
                        pmdExtension.setToolVersion(internalProperties.getVersion("pmdTool"));
                        var pmdMainTask = tasks.named(
                            "pmdMain",
                            Pmd.class,
                            pmdTask -> pmdTask.setRuleSetFiles(
                                projectLayout.files(
                                    staticAnalyseFolder.resolve("pmd.xml")
                                )
                            )
                        );
                        var pmdTestTasks = Stream.of(JavaPlugin.TEST_TASK_NAME, internalConventionExtension.getIntegrationTestName().get())
                            .map(testTaskName -> "pmd" + capitalize(testTaskName))
                            .map(
                                taskName -> tasks.named(
                                    taskName,
                                    Pmd.class,
                                    pmdTask -> pmdTask.setRuleSetFiles(
                                        projectLayout.files(
                                            staticAnalyseFolder.resolve("pmd-test.xml")
                                        )
                                    )
                                )
                            )
                            .toList();
                        staticAnalyseMain.configure(task -> task.dependsOn(pmdMainTask));
                        staticAnalyseTest.configure(task -> task.dependsOn(pmdTestTasks));
                    }
                );

                // Configure checkstyle
                pluginManager.withPlugin(
                    "checkstyle",
                    checkstyle -> {
                        var checkstyleExtension = extensions.getByType(CheckstyleExtension.class);
                        checkstyleExtension.setToolVersion(internalProperties.getVersion("checkstyleTool"));
                        checkstyleExtension.setMaxWarnings(0);
                        checkstyleExtension.setMaxErrors(0);
                        checkstyleExtension.setConfigFile(staticAnalyseFolder.resolve("checkstyle.xml").toFile());

                        var checkstyleMainTask = tasks.named("checkstyleMain");
                        var checkstyleTestTasks = Stream.of("test", internalConventionExtension.getIntegrationTestName().get())
                            .map(string -> "checkstyle" + string.substring(0, 1).toUpperCase() + string.substring(1))
                            .map(taskName -> tasks.named(taskName, Task.class))
                            .collect(Collectors.toList());

                        staticAnalyseMain.configure(task -> task.dependsOn(checkstyleMainTask));
                        staticAnalyseTest.configure(task -> task.dependsOn(checkstyleTestTasks));
                    }
                );
                // -------------------- Configure static analysis end --------------------
            }
        );
    }

    private String capitalize(String string) {
        var chars = string.toCharArray();
        chars[0] = Character.toUpperCase(chars[0]);
        return new String(chars);
    }
}
