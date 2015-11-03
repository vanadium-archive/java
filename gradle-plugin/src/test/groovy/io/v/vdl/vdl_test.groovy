import com.google.common.base.Charsets
import com.google.common.io.Files
import io.v.vdl.VdlConfiguration
import io.v.vdl.VdlPlugin
import org.gradle.api.Project
import org.gradle.api.internal.ClosureBackedAction
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

import static com.google.common.truth.Truth.assertThat

class VdlPluginTest {
    @Test
    public void vdlPluginTest() {
        Project project = ProjectBuilder.builder().build()

        createVdlToolJar(new File(project.getProjectDir(), "test-gradle-plugin-vdltool-arch1.jar"), "vdl-somearch", "blahface")
        createVdlToolJar(new File(project.getProjectDir(), "test-gradle-plugin-vdltool-arch2.jar"), "vdl-macosx", "hello")

        List<File> vdlToolFiles = VdlPlugin.getVdlToolJarFiles(project,
                project.files(
                        'test-gradle-plugin-vdltool-arch1.jar',
                        'test-gradle-plugin-vdltool-arch2.jar'));
        assertThat(vdlToolFiles).named('VDL jar files').isNotEmpty()

        Map<String, String> fileContents = new HashMap<String, String>()
        // Read all the files
        for (File f : vdlToolFiles) {
            fileContents.put(f.getName(), Files.toString(f, Charsets.US_ASCII))
        }

        assertThat(fileContents).hasSize(2)
        assertThat(fileContents).containsEntry("vdl-somearch", "blahface")
        assertThat(fileContents).containsEntry("vdl-macosx", "hello")
    }

    @Test
    public void addsVdlFilesToOutputJar() {
        Project project = ProjectBuilder.builder().build()
        project.pluginManager.apply 'java'
        project.pluginManager.apply VdlPlugin.class

        // Create a test directory and put some vdl file in it.
        File tempDir = Files.createTempDir()
        try {
            Files.write("something\n", new File(tempDir, "test.vdl"), Charsets.UTF_8)

            // Create an empty subdirectory and assert that it is not in the output.
            assertThat(new File(tempDir, "emptyDir").mkdir()).isTrue()
            project.extensions.configure(VdlConfiguration, new ClosureBackedAction<VdlConfiguration>({
                inputPaths += tempDir.getAbsolutePath()
            }))
            project.evaluate()
            assertThat(project.sourceSets.main.resources).containsExactly(new File(tempDir, "test.vdl"))
        } finally {
            tempDir.deleteDir()
        }
    }

    @Test
    public void extractsAndUsesIncludedVdlFiles() {
        Project project = ProjectBuilder.builder().build()
        project.pluginManager.apply 'java'
        project.pluginManager.apply VdlPlugin.class

        // Create a jar that has some VDL file in it.
        File jarFile = File.createTempFile("mydep", ".jar");
        try {
            ZipOutputStream stream = new ZipOutputStream(new FileOutputStream(jarFile))
            byte[] output = "hello world".getBytes(Charsets.US_ASCII)
            stream.putNextEntry(new JarEntry("whatever/test.vdl"))
            stream.write(output)
            stream.closeEntry()
            stream.flush()
            stream.close()
            project.repositories.flatDir(dirs: jarFile.getParent())
            project.dependencies.add('compile', project.files(jarFile.getAbsolutePath()))
            project.evaluate()

            // Assert that whatever/test.vdl appears in the VDLPATH.
            List<File> paths = project.vdl.inputPaths.collectMany { project.fileTree(it).collect() }
            assertThat(paths.count { it.getPath().endsWith('whatever/test.vdl') }).isEqualTo(1)
        } finally {
            jarFile.delete();
        }
    }

    private static void createVdlToolJar(File outputFile, String entryName, String vdlBinContents) {
        JarOutputStream outputStream = new JarOutputStream(new FileOutputStream(outputFile))
        outputStream.putNextEntry(new ZipEntry(entryName))
        outputStream.write(vdlBinContents.getBytes(Charsets.US_ASCII))
        outputStream.close()
    }
}