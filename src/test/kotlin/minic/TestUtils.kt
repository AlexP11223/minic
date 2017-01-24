package minic

import org.apache.commons.io.IOUtils
import java.io.File
import java.nio.charset.Charset

class JavaTestUtils {
    companion object {
        val javaPath: String

        init {
            val binDir = System.getProperty("java.home") + "/bin/"
            if (File(binDir).exists()) {
                javaPath = binDir + "java"
            } else {
                javaPath = "java"
            }
        }
    }
}

class ProcessTestUtils {
    companion object {
        /**
         * runs the specified program (with args and optionally setting the working dir)
         * and returns output (stdout + stderr)
         */
        fun run(program: String, args: String, workingDir: String? = null): String {
            val pb = ProcessBuilder(program, args)
            if (workingDir != null) {
                pb.directory(File(workingDir))
            }
            pb.redirectErrorStream(true)
            val process = pb.start()
            val output = IOUtils.toString(process.inputStream, Charset.defaultCharset())
            return output.replace("\r", "").trim()
        }
    }
}
