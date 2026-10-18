package org.plos_clan.kbpf.lib

import org.plos_clan.kbpf.BpfRuntime
import org.plos_clan.kbpf.util.BpfException

typealias HelperFunction = (runtime: BpfRuntime, arg0: ULong, arg1: ULong, arg2: ULong, arg3: ULong, arg4: ULong) -> ULong

interface HelperLibraryImpl {
    fun getHelpers(): Map<Int, HelperFunction>
    fun getKfunc(): Map<Int, HelperFunction>
}

class HelperLibrary(val runtime: BpfRuntime, val maxHelpers: Int = 256) {
    val helpers = arrayOfNulls<HelperFunction>(maxHelpers)
    val kfuncs = arrayOfNulls<HelperFunction>(maxHelpers)

    fun registerHelper(library: HelperLibraryImpl) {
        library.getHelpers().forEach {
            if(it.key >= maxHelpers) throw BpfException("Out of bounds helper {id=${it.key} > max=$maxHelpers}", runtime)
            if(helpers[it.key] != null) throw BpfException("id=${it.key} has helper", runtime)
            helpers[it.key] = it.value
        }
        library.getKfunc().forEach {
            if(it.key >= maxHelpers) throw BpfException("Out of bounds helper {id=${it.key} > max=$maxHelpers}", runtime)
            if(kfuncs[it.key] != null) throw BpfException("id=${it.key} has kfunc", runtime)
            kfuncs[it.key] = it.value
        }
    }

    fun callHelper(id: Int, arg0: ULong, arg1: ULong, arg2: ULong, arg3: ULong, arg4: ULong): ULong {
        val helper = helpers[id] ?: throw BpfException("cannot find helper $id", runtime)
        return helper(runtime, arg0, arg1, arg2, arg3, arg4)
    }

    fun callKfunc(id: Int, arg0: ULong, arg1: ULong, arg2: ULong, arg3: ULong, arg4: ULong): ULong {
        val kfunc = kfuncs[id] ?: throw BpfException("cannot find kfunc $id", runtime)
        return kfunc(runtime, arg0, arg1, arg2, arg3, arg4)
    }
}