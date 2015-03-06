// Defines tasks for building the VDL tool and generating VDL files.

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.Exec;
import org.gradle.api.InvalidUserDataException;

class BuildVdlToolTask extends Exec {
    {
        description('Builds the vdl tool')
        commandLine('v23', 'go', 'install', 'v.io/x/ref/cmd/vdl')
    }
}


class VdlPlugin implements Plugin<Project> {
    String inputPath
    String outputPath

    void apply(Project project) {
        project.extensions.create('vdl', VdlConfiguration)
        def buildTask = project.task('buildVdl', type: BuildVdlToolTask)
	def generateTask = project.task('generateVdl', type: Exec) {
            group = "Build"
            description('Generates Java vdl source using the vdl tool')
	}
	def prepareTask = project.task('prepareVdl') {
	    doLast {
                generateTask.environment(VDLPATH: project.vdl.inputPath)
                generateTask.commandLine(System.env.VANADIUM_ROOT + '/release/go/bin/vdl', 'generate', '--lang=java', "--java_out_dir=${project.vdl.outputPath}", 'all')

	    }
	}
	prepareTask.dependsOn(buildTask)
        generateTask.dependsOn(prepareTask)
        project.clean.delete(project.vdl.outputPath)

	if (project.plugins.hasPlugin('java')) {
            project.compileJava.dependsOn(generateTask)
            project.sourceSets.main.java.srcDirs += project.vdl.outputPath
	}

	if (project.plugins.hasPlugin('android')) {
            project.tasks.'preBuild'.dependsOn(generateTask)
            project.android.sourceSets.main.java.srcDirs += project.vdl.outputPath
	}
    }
}

class VdlConfiguration {
    String inputPath = null;
    String outputPath = "generated-src/vdl";
}
