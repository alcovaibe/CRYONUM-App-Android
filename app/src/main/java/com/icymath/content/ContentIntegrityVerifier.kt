package com.icymath.content

import java.io.File

class ContentIntegrityVerifier {
    fun verify(file: File, expected: ContentManifestFile): Boolean {
        return file.isFile &&
            file.length() == expected.sizeBytes &&
            ContentStorage.hasPdfMagic(file) &&
            ContentStorage.sha256(file) == expected.sha256
    }
}
