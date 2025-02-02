package pers.zhc.tools.utils

import pers.zhc.tools.jni.JNI.Utf8

/**
 * @author bczhc
 */
class CodepointIterator(s: String): Iterator<Int>, Iterable<Int> {
    private val addr = Utf8.getCodepointIterator(s)

    override fun hasNext(): Boolean {
        return Utf8.hasNext(addr)
    }

    override fun next(): Int {
        return Utf8.next(addr)
    }

    private fun release() {
        Utf8.release(addr)
    }

    override fun iterator(): Iterator<Int> {
        return this
    }

    /**
     * Method for finalizing this object for JVM
     */
    protected fun finalize() {
        if (addr != 0L) {
            release()
        }
    }
}