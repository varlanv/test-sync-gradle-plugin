package com.varlanv.gradle.plugin;

import lombok.RequiredArgsConstructor;
import lombok.val;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.VersionCatalogsExtension;
import org.gradle.api.plugins.GroovyPlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.plugins.jvm.JvmTestSuite;
import org.gradle.api.plugins.quality.*;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.VariantVersionMappingStrategy;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.tasks.compile.GroovyCompile;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.gradle.jvm.toolchain.JavaToolchainService;
import org.gradle.jvm.toolchain.JvmVendorSpec;
import org.gradle.plugin.devel.plugins.JavaGradlePluginPlugin;
import org.gradle.plugins.ide.idea.IdeaPlugin;
import org.gradle.plugins.ide.idea.model.IdeaModel;
import org.gradle.plugins.signing.SigningExtension;
import org.gradle.plugins.signing.SigningPlugin;
import org.gradle.testing.base.TestingExtension;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class InternalConventionPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        // -------------------- Variables start --------------------
        val extensions = project.getExtensions();
        val providers = project.getProviders();
        val pluginManager = project.getPluginManager();
        val tasks = project.getTasks();
        val projectName = project.getName();
        val repositories = project.getRepositories();
        val dependencies = project.getDependencies();
        val components = project.getComponents();
        val configurations = project.getConfigurations();
        val rootDir = project.getRootDir().toPath();
        val projectLayout = project.getLayout();
        val internalEnvironment = Optional.ofNullable((InternalEnvironment) extensions.findByName(InternalEnvironment.name()))
            .orElseGet(() -> new InternalEnvironment(
                providers.environmentVariable("CI").isPresent(),
                false
            ));
        val internalProperties = Optional.ofNullable((InternalProperties) extensions.findByName(InternalProperties.name()))
            .orElseGet(() -> new InternalProperties(((VersionCatalogsExtension) extensions.getByName("versionCatalogs")).named("libs")));
        val internalConventionExtension = Optional.ofNullable((InternalConventionExtension) extensions.findByName(InternalConventionExtension.name()))
            .orElseGet(() -> extensions.create(InternalConventionExtension.name(), InternalConventionExtension.class));
        internalConventionExtension.getIntegrationTestName().convention("integrationTest");
        val isGradlePlugin = projectName.endsWith("plugin");
        val javaToolchainService = extensions.getByType(JavaToolchainService.class);
        val sourceJavaVersion = 8;
        val targetJavaVersion = 8;
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
//                pluginManager.apply(CheckstylePlugin.class);
                pluginManager.apply(GroovyPlugin.class);
                if (internalEnvironment.isLocal()) {
                    pluginManager.apply(IdeaPlugin.class);
                    val idea = (IdeaModel) extensions.getByName("idea");
                    idea.getModule().setDownloadJavadoc(true);
                    idea.getModule().setDownloadSources(true);
                }
            }
        );
        // -------------------- Apply common plugins end --------------------

        // -------------------- Configure Java start --------------------
        pluginManager.withPlugin(
            "java",
            plugin -> {
                val java = (JavaPluginExtension) extensions.getByName("java");
                java.withSourcesJar();
                if (internalEnvironment.isCi()) {
                    java.setSourceCompatibility(JavaLanguageVersion.of(sourceJavaVersion));
                    java.setTargetCompatibility(JavaLanguageVersion.of(targetJavaVersion));
                } else {
                    java.toolchain(
                        toolchain -> {
                            toolchain.getVendor().set(JvmVendorSpec.AZUL);
                            toolchain.getLanguageVersion().set(JavaLanguageVersion.of(targetJavaVersion));
                        }
                    );
                }

                // -------------------- Add common dependencies start --------------------
                val jetbrainsAnnotations = internalProperties.getLib("jetbrains-annotations");
                dependencies.add(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME, jetbrainsAnnotations);
                dependencies.add(JavaPlugin.TEST_COMPILE_ONLY_CONFIGURATION_NAME, jetbrainsAnnotations);
                dependencies.add(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME, internalProperties.getLib("assertj-core"));
                dependencies.add(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME, internalProperties.getLib("junit-jupiter-api"));
                dependencies.add(JavaPlugin.TEST_RUNTIME_ONLY_CONFIGURATION_NAME, internalProperties.getLib("junit-platform-launcher"));

                val lombokDependency = internalProperties.getLib("lombok");
                dependencies.add(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME, lombokDependency);
                dependencies.add(JavaPlugin.ANNOTATION_PROCESSOR_CONFIGURATION_NAME, lombokDependency);
                dependencies.add(JavaPlugin.TEST_COMPILE_ONLY_CONFIGURATION_NAME, lombokDependency);
                dependencies.add(JavaPlugin.TEST_ANNOTATION_PROCESSOR_CONFIGURATION_NAME, lombokDependency);

                dependencies.add(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME, internalProperties.getLib("groovy-all"));
                // -------------------- Add common dependencies end --------------------
            }
        );
        // -------------------- Configure Java end --------------------
        // -------------------- Configure Groovy start --------------------
        pluginManager.withPlugin(
            "groovy",
            (plugin) -> {
                tasks.withType(GroovyCompile.class).configureEach(groovyCompile -> {
                    groovyCompile.getGroovyOptions().setConfigurationScript(rootDir.resolve(".config").resolve("compiler-config.groovy").toFile());
                });
            });
        // -------------------- Configure Groovy end --------------------

        // -------------------- Configure libraries publishing start --------------------
        if (!isGradlePlugin) {
            pluginManager.withPlugin(
                "maven-publish",
                plugin -> {
                    pluginManager.apply(SigningPlugin.class);
                    val publishingExtension = extensions.getByType(PublishingExtension.class);
                    val signingExtension = extensions.getByType(SigningExtension.class);

                    val javaPluginExtension = extensions.getByType(JavaPluginExtension.class);
                    javaPluginExtension.withJavadocJar();
                    javaPluginExtension.withSourcesJar();

                    val createdMavenPublication = publishingExtension.getPublications().create(
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

        project.afterEvaluate(ignore -> {
                // -------------------- Configure tests start --------------------
                // Need to run these things after project evaluate, so that InternalConventionExtension values are initialized
                pluginManager.withPlugin(
                    "java",
                    plugin -> {
                        val testing = (TestingExtension) extensions.getByName("testing");
                        val suites = testing.getSuites();
                        val integrationTestTaskName = internalConventionExtension.getIntegrationTestName().get();
                        val integrationTestSuite = suites.register(
                            integrationTestTaskName,
                            JvmTestSuite.class,
                            suite -> suite.getTargets().all(
                                target -> target.getTestTask().configure(
                                    test -> test.getJavaLauncher().set(javaToolchainService.launcherFor(
                                            config -> {
                                                config.getLanguageVersion().set(JavaLanguageVersion.of(targetJavaVersion));
                                                config.getVendor().set(JvmVendorSpec.AZUL);
                                            }
                                        )
                                    )
                                )
                            )
                        );
                        suites.configureEach(
                            suite -> {
                                if (suite instanceof JvmTestSuite) {
                                    val jvmTestSuite = (JvmTestSuite) suite;
                                    jvmTestSuite.useJUnitJupiter();
                                    jvmTestSuite.dependencies(
                                        jvmComponentDependencies -> {
                                            val implementation = jvmComponentDependencies.getImplementation();
                                            implementation.add(jvmComponentDependencies.project());
                                        }
                                    );
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
                                                val environment = new HashMap<>(test.getEnvironment());
                                                environment.put("TESTCONTAINERS_REUSE_ENABLE", "true");
                                                test.setEnvironment(environment);
                                                val memory = test.getName().equals(JavaPlugin.TEST_TASK_NAME) ? "128m" : "512m";
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
                val staticAnalyseFolder = rootDir.resolve(".config").resolve("static-analyse");

                // Configure aggregate static analysis tasks
                val staticAnalyseMain = tasks.register(
                    "staticAnalyseMain",
                    task -> {
                        task.setGroup("static analysis");
                        task.setDescription("Run static analysis on main sources");
                    }
                );
                val staticAnalyseTest = tasks.register(
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
                        val pmdExtension = (PmdExtension) extensions.getByName("pmd");
                        pmdExtension.setRuleSetFiles(
                            projectLayout.files(
                                staticAnalyseFolder.resolve("pmd.xml")
                            )
                        );
                        pmdExtension.setToolVersion(internalProperties.getVersion("pmdTool"));
                        val pmdMainTask = tasks.named(
                            "pmdMain",
                            Pmd.class,
                            pmdTask -> pmdTask.setRuleSetFiles(
                                projectLayout.files(
                                    staticAnalyseFolder.resolve("pmd.xml")
                                )
                            )
                        );
//                        val pmdTestTasks = Stream.of(JavaPlugin.TEST_TASK_NAME, internalConventionExtension.getIntegrationTestName().get())
//                            .map(testTaskName -> "pmd" + capitalize(testTaskName))
//                            .map(
//                                taskName -> tasks.named(
//                                    taskName,
//                                    Pmd.class,
//                                    pmdTask -> pmdTask.setRuleSetFiles(
//                                        projectLayout.files(
//                                            staticAnalyseFolder.resolve("pmd-test.xml")
//                                        )
//                                    )
//                                )
//                            )
//                            .toList();
                        staticAnalyseMain.configure(task -> task.dependsOn(pmdMainTask));
//                        staticAnalyseTest.configure(task -> task.dependsOn(pmdTestTasks));
                    }
                );

                // Configure checkstyle
                pluginManager.withPlugin(
                    "checkstyle",
                    checkstyle -> {
                        val checkstyleExtension = extensions.getByType(CheckstyleExtension.class);
                        checkstyleExtension.setToolVersion(internalProperties.getVersion("checkstyleTool"));
                        checkstyleExtension.setMaxWarnings(0);
                        checkstyleExtension.setMaxErrors(0);
                        checkstyleExtension.setConfigFile(staticAnalyseFolder.resolve("checkstyle.xml").toFile());

                        val checkstyleMainTask = tasks.named("checkstyleMain");
//                        val checkstyleTestTasks = Stream.of("test", internalConventionExtension.getIntegrationTestName().get())
//                            .map(string -> "checkstyle" + string.substring(0, 1).toUpperCase() + string.substring(1))
//                            .map(taskName -> tasks.named(taskName, Task.class))
//                            .collect(Collectors.toList());

                        staticAnalyseMain.configure(task -> task.dependsOn(checkstyleMainTask));
//                        staticAnalyseTest.configure(task -> task.dependsOn(checkstyleTestTasks));
                    }
                );
                // -------------------- Configure static analysis end --------------------
            }
        );
    }

    private String capitalize(String string) {
        val chars = string.toCharArray();
        chars[0] = Character.toUpperCase(chars[0]);
        return new String(chars);
    }
}
