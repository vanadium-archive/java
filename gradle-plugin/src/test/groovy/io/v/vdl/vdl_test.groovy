import com.google.common.base.Charsets
import com.google.common.io.Files
import io.v.vdl.VdlPlugin
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

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

    private static void createVdlToolJar(File outputFile, String entryName, String vdlBinContents) {
        JarOutputStream outputStream = new JarOutputStream(new FileOutputStream(outputFile))
        outputStream.putNextEntry(new ZipEntry(entryName))
        outputStream.write(vdlBinContents.getBytes(Charsets.US_ASCII))
        outputStream.close()
    }
}