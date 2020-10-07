/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain.components

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapiext.isUnitTestMode
import org.rust.cargo.toolchain.RsToolchain
import org.rust.cargo.toolchain.impl.parseRustcVersion
import org.rust.openapiext.*
import java.nio.file.Path

class Rustc(private val rustcPath: Path) {

    fun queryVersions(): RsToolchain.VersionInfo {
        if (!isUnitTestMode) {
            checkIsBackgroundThread()
        }
        val lines = GeneralCommandLine(rustcPath)
            .withParameters("--version", "--verbose")
            .execute()
            ?.stdoutLines
        val rustcVersion = lines?.let { parseRustcVersion(it) }
        return RsToolchain.VersionInfo(rustcVersion)
    }

    fun getSysroot(projectDirectory: Path): String? {
        if (!isUnitTestMode) {
            checkIsBackgroundThread()
        }
        val timeoutMs = 10000
        val output = GeneralCommandLine(rustcPath)
            .withCharset(Charsets.UTF_8)
            .withWorkDirectory(projectDirectory)
            .withParameters("--print", "sysroot")
            .execute(timeoutMs)
        return if (output?.isSuccess == true) output.stdout.trim() else null
    }

    fun getStdlibFromSysroot(projectDirectory: Path): VirtualFile? {
        val sysroot = getSysroot(projectDirectory) ?: return null
        val fs = LocalFileSystem.getInstance()
        return fs.refreshAndFindFileByPath(FileUtil.join(sysroot, "lib/rustlib/src/rust"))
    }

    fun getCfgOptions(projectDirectory: Path): List<String>? {
        val timeoutMs = 10000
        val output = GeneralCommandLine(rustcPath)
            .withCharset(Charsets.UTF_8)
            .withWorkDirectory(projectDirectory)
            .withParameters("--print", "cfg")
            .execute(timeoutMs)
        return if (output?.isSuccess == true) output.stdoutLines else null
    }
}
