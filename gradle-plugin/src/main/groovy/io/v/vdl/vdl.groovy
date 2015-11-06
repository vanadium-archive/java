// Defines tasks for building the VDL tool and generating VDL files.

package io.v.vdl

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.Exec

import java.util.jar.JarEntry
import java.util.jar.JarFile

class VdlPlugin implements Plugin<Project> {
    String inputPath
    String outputPath

    void apply(Project project) {
        project.extensions.create('vdl', VdlConfiguration)

        def extractTask = project.task('extractVdl', type: Copy) {
            from {
                getVdlToolJarFiles(project, project.buildscript.configurations.classpath)
            }
            into { new File(project.buildDir, 'vdltool') }
        }
        def generateTask = project.task('generateVdl', type: Exec) {
        }
        def vdlTask = project.task('vdl') {
            group = "Build"
            description('Generates Java vdl source using the vdl tool')
        }
        def prepareTask = project.task('prepareVdl') {
            doLast {
                extractTransitiveVdlFilesAndUpdateInputPaths(project)
                List<String> outDirs = getJavaOutDirs(project)
                generateTask.environment(VDLPATH: project.vdl.inputPaths.join(":"))
                List<String> commandLine = ['build/vdltool/vdl-' + getOsName(),
                                            '--builtin_vdlroot', 'generate',
                                            '--lang=java',
                                            "--java-out-dir=" + outDirs.join(",")
                ]
                if (!project.vdl.packageTranslations.isEmpty()) {
                    commandLine.add('--java-out-pkg=' + project.vdl.packageTranslations.join(','))
                }
                commandLine.add('all')
                generateTask.commandLine(commandLine)
            }
        }
        def removeVdlRootTask = project.task('removeVdlRoot', type: Delete) {
            onlyIf { !project.vdl.generateVdlRoot }
            delete project.vdl.outputPath + '/io/v/v23/vdlroot/'
        }
        extractTask.dependsOn(prepareTask)
        generateTask.dependsOn(extractTask)
        removeVdlRootTask.dependsOn(generateTask)
        vdlTask.dependsOn(removeVdlRootTask)

        if (project.hasProperty('clean')) {
            project.clean.delete(project.vdl.outputPath)
        }

        if (project.plugins.hasPlugin('java')) {
            project.compileJava.dependsOn(vdlTask)
            project.sourceSets.main.java.srcDirs += project.vdl.outputPath
        }

        if (project.plugins.hasPlugin('com.android.library') || project.plugins.hasPlugin('com.android.application')) {
            project.tasks.'preBuild'.dependsOn(vdlTask)
            project.android.sourceSets.main.java.srcDirs += project.vdl.outputPath
        }
    }

    public static void extractTransitiveVdlFilesAndUpdateInputPaths(Project project) {
        boolean addTransitiveVdlDir = false
        // Go through the dependencies of all configurations looking for jar files containing
        // VDL files.
        project.configurations.each({
            it.each({
                if (it.getName().endsWith('.jar') && it.exists()) {
                    addTransitiveVdlDir = extractVdlFiles(it, new File(project.getProjectDir(), project.vdl.transitiveVdlDir)) || addTransitiveVdlDir
                }
            })
        })

        // Now recursively descend through the project's project dependencies looking for
        // VDL projects.
        Set<String> inputPaths = new LinkedHashSet<>()
        addVdlInputPathsForProject(project, inputPaths)
        // Copy any VDL files in any input path to the transitive VDL directory.
        project.copy {
            inputPaths.each({
                from(it) {
                    include '**/*.vdl'
                    includeEmptyDirs = false
                }
            })

            into project.vdl.transitiveVdlDir
        }
        addTransitiveVdlDir = !inputPaths.isEmpty() || addTransitiveVdlDir

        if (addTransitiveVdlDir) {
            project.vdl.inputPaths += project.vdl.transitiveVdlDir
        }
    }

    public static List<String> getJavaOutDirs(Project project) {
        List<String> result = new ArrayList<>()
        for (String inputPath : project.vdl.inputPaths) {
            if (!inputPath.startsWith(project.vdl.transitiveVdlDir)) {
                result.add(inputPath + '->' + project.vdl.outputPath)
            }
        }
        result.add(project.vdl.transitiveVdlDir + "->" + project.vdl.transitiveVdlDir)
        return result
    }

    /**
     * Extracts any vdl files in the given jar file to the given destination directory. Returns
     * {@code true} if any files were extracted.
     */
    private static boolean extractVdlFiles(File jarFile, File destination) {
        JarFile jar = new JarFile(jarFile)
        Enumeration<JarEntry> entries = jar.entries()
        boolean extracted = false
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement()
            if (entry.isDirectory() || !entry.getName().endsWith('.vdl')) {
                continue
            }
            // Create the file's directory.
            File destinationFile = new File(destination, entry.getName())
            destinationFile.getParentFile().mkdirs()
            InputStream input = jar.getInputStream(entry)
            OutputStream output = new FileOutputStream(destinationFile)
            copyStream(input, output)
            extracted = true
            input.close()
            output.close()
        }
        return extracted
    }

    private static void copyStream(InputStream source, OutputStream destination) {
        byte[] buffer = new byte[1 << 16]
        while (true) {
            int r = source.read(buffer)
            if (r == -1) {
                return
            }
            destination.write(buffer, 0, r)
        }
    }

    /**
     * Returns a list of {@link java.io.File} instances representing VDL binaries.
     */
    static List<File> getVdlToolJarFiles(Project project, FileCollection files) {
        List<File> result = new ArrayList<File>()

        for (File file : files.findAll({
            it.name.contains 'gradle-plugin'
        })) {
            project.zipTree(file).findAll({ it.name.startsWith("vdl-") }).each({
                result.add(project.resources.text.fromArchiveEntry(file, it.name).asFile())
            })
        }

        return result
    }

    private static void addVdlInputPathsForProject(Project project, Set<String> inputPaths) {
        if (project == null) {
            return
        }
        Object extension = project.extensions.findByName('vdl')
        if (extension != null) {
            extension.inputPaths.each {
                File newPath = new File(it)
                if (!newPath.isAbsolute()) {
                    newPath = new File(project.getProjectDir(), it)
                }
                inputPaths.add(newPath.getAbsolutePath())
            }
        }
        project.configurations.each({
            it.dependencies.each({
                if (it instanceof ProjectDependency) {
                    addVdlInputPathsForProject(it.getDependencyProject(), inputPaths)
                }
            })
        })
    }

    private static isMacOsX() {
        return System.properties['os.name'].toLowerCase().contains("os x")
    }

    private static isLinux() {
        return System.properties['os.name'].toLowerCase().contains("linux")
    }

    static String getOsName() {
        if (isLinux()) {
            return "linux";
        } else if (isMacOsX()) {
            return "macosx";
        } else {
            throw new IllegalStateException("Unsupported operating system " + System.properties.'os.name')
        }
    }
}

class VdlConfiguration {
    String vanadiumRoot
    List<String> inputPaths = []
    String outputPath = "generated-src/vdl"
    String transitiveVdlDir = "generated-src/transitive-vdl"
    String vdlToolPath = ""
    List<String> packageTranslations = []

    // If true, code generated for the vdlroot vdl package will be emitted.
    // Typically, users will want to leave this set to false as they will
    // already get the vdlroot package by depending on the :lib project.
    boolean generateVdlRoot = false;
}

