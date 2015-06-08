// Defines tasks for building the VDL tool and generating VDL files.

package io.v.vdl;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.Delete;
import org.gradle.api.tasks.Exec;
import org.gradle.api.InvalidUserDataException;

class VdlPlugin implements Plugin<Project> {
    String inputPath
    String outputPath

    void apply(Project project) {
        project.extensions.create('vdl', VdlConfiguration)

        def extractTask = project.task('extractVdl', type: Copy) {
            from {
	        project.resources.text.fromArchiveEntry(project.buildscript.configurations.classpath.findAll { it.name.contains 'gradle-plugin' }, 'vdl').asFile()
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
                generateTask.environment(VDLPATH: project.vdl.inputPaths.join(":"))
                generateTask.commandLine('build/vdltool/vdl', 'generate', '--lang=java', "--java-out-dir=${project.vdl.outputPath}", 'all')
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
}

