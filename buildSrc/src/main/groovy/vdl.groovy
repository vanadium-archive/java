// Defines tasks for building the VDL tool and generating VDL files.

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.Delete;
import org.gradle.api.tasks.Exec;
import org.gradle.api.InvalidUserDataException;

class VdlPlugin implements Plugin<Project> {
    String inputPath
    String outputPath

    void apply(Project project) {
        project.extensions.create('vdl', VdlConfiguration)

        def buildTask = project.task('buildVdl', type: Exec) {
            description('Builds the vdl tool')
        }
        def generateTask = project.task('generateVdl', type: Exec) {
        }
        def vdlTask = project.task('vdl') {
            group = "Build"
            description('Generates Java vdl source using the vdl tool')
        }
        def prepareTask = project.task('prepareVdl') {
            doLast {
                def runPath = System.env.PATH + File.pathSeparator + project.vdl.getVanadiumRoot() + '/bin'
                buildTask.environment(PATH: runPath, V23_ROOT: project.vdl.getVanadiumRoot())
                buildTask.commandLine(project.vdl.getVanadiumRoot() + '/bin/v23', 'go', 'install', 'v.io/x/ref/cmd/vdl')
                generateTask.environment(VDLPATH: project.vdl.inputPaths.join(":"), V23_ROOT: project.vdl.getVanadiumRoot())
                generateTask.commandLine(project.vdl.getVanadiumRoot() + '/release/go/bin/vdl',
                        'generate', '--lang=java', "--java-out-dir=${project.vdl.outputPath}", 'all')
            }
        }
        def removeVdlRootTask = project.task('removeVdlRoot', type: Delete) {
            onlyIf { !project.vdl.generateVdlRoot }
            delete project.vdl.outputPath + '/io/v/v23/vdlroot/'
        }
        buildTask.dependsOn(prepareTask)
        generateTask.dependsOn(buildTask)
        removeVdlRootTask.dependsOn(generateTask)
        vdlTask.dependsOn(removeVdlRootTask)
        project.clean.delete(project.vdl.outputPath)

        if (project.plugins.hasPlugin('java')) {
            project.compileJava.dependsOn(vdlTask)
            project.sourceSets.main.java.srcDirs += project.vdl.outputPath
        }

        if (project.plugins.hasPlugin('com.android.library') || project.plugins.hasPlugin('com.android.application')) {
            project.tasks.'preBuild'.dependsOn(vdlTask)
            project.android.sourceSets.main.java.srcDirs += project.vdl.outputPath
        }
    }
}

class VdlConfiguration {
    String vanadiumRoot
    List<String> inputPaths = []
    String outputPath = "generated-src/vdl"

    // If true, code generated for the vdlroot vdl package will be emitted.
    // Typically, users will want to leave this set to false as they will
    // already get the vdlroot package by depending on the :lib project.
    boolean generateVdlRoot = false;

    def getVanadiumRoot() {
        if (vanadiumRoot != null) {
            return vanadiumRoot;
        }
        if (System.properties.vanadiumRoot != null) {
            return System.properties.vanadiumRoot
        }
        if (System.env.V23_ROOT != null && !"".equals(System.env.V23_ROOT)) {
            return System.env.V23_ROOT
        }
        throw new InvalidUserDataException("V23_ROOT not specified")
    }
}
