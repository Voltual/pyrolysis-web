//参考自https://github.com/coil-kt/coil/blob/main/samples/shared/src/wasmJsMain/kotlin/sample/common/imageLoader.wasmJs.kt
package me.voltual.pyrolysis

import coil3.disk.DiskCache

// We can't write to the file system from a web browser.
internal fun newDiskCache(): DiskCache? {
    return null
}