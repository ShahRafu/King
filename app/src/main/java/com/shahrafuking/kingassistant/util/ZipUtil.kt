package com.shahrafuking.kingassistant.util

import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ZipUtil {
    fun zipFiles(outputFile: File, inputs: List<File>) {
        ZipOutputStream(BufferedOutputStream(FileOutputStream(outputFile))).use { zos ->
            for (file in inputs) {
                if (!file.exists()) continue
                FileInputStream(file).use { fis ->
                    val entry = ZipEntry(file.name)
                    zos.putNextEntry(entry)
                    val buffer = ByteArray(4096)
                    var len: Int
                    BufferedInputStream(fis).use { bis ->
                        while (bis.read(buffer).also { len = it } > 0) {
                            zos.write(buffer, 0, len)
                        }
                    }
                    zos.closeEntry()
                }
            }
        }
    }
}
