// FILE: shared/src/wasmJsMain/kotlin/me/voltual/pyrolysis/util/WasmStreamHelper.kt
package me.voltual.pyrolysis.util

import me.voltual.pyrolysis.BrowserFile
import kotlin.js.Promise
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get

// 使用 @JsFun 定义底层的 JS 调用，这是 Wasm 最稳定的互操作方式
@JsFun("(file) => file.stream().getReader()")
internal external fun getReader(file: JsAny): JsAny

@JsFun("(reader) => reader.read()")
internal external fun readChunk(reader: JsAny): Promise<JsAny>

@JsFun("(result) => result.done")
internal external fun isDone(result: JsAny): Boolean

@JsFun("(result) => result.value")
internal external fun getValue(result: JsAny): Uint8Array?

@JsFun("(reader) => reader.releaseLock()")
internal external fun releaseReaderLock(reader: JsAny): Unit