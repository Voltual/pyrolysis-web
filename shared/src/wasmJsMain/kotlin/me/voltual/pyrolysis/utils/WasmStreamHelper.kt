package me.voltual.pyrolysis.utils

import kotlin.js.Promise
import org.khronos.webgl.Uint8Array

/**
 * 使用 @JsFun 直接定义底层的 JS 调用。
 * JsAny 是 Kotlin/Wasm 与 JS 交互的基础类型。
 */
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