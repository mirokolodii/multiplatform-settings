/*
 * Copyright 2020 Russell Wolf
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.russhwolf.settings

import kotlinx.cinterop.pointed
import kotlinx.cinterop.toKString
import platform.posix.opendir
import platform.posix.readdir
import platform.posix.remove
import kotlin.test.AfterClass
import kotlin.test.BeforeClass

@UseExperimental(ExperimentalUnsignedTypes::class)
class DbmSettings2Test : BaseSettingsTest(
    platformFactory = object : Settings.Factory {
        override fun create(name: String?): Settings = DbmSettings2(name ?: "dbm")
    },
    hasListeners = false
) {

    private lateinit var initialDbFiles: List<String>

    @BeforeClass
    fun getInitialDbFiles() {
        initialDbFiles = getDbFiles()
    }

    @AfterClass
    fun cleanup() {
        val newDbFiles = getDbFiles() - initialDbFiles
        newDbFiles.forEach {
            remove(it)
        }
    }

    private fun getDbFiles(): List<String> {
        val directory = opendir("./") ?: return emptyList()

        val out = mutableListOf<String>()
        while (true) {
            val entry = readdir(directory) ?: break

            val filename = entry.pointed.d_name.toKString()
            if (filename.endsWith(".db")) {
                out.add(filename)
            }
        }
        return out
    }

//    @Test
//    fun constructor_filename() {
//        val filename = "test_dbm"
//        val settings = DbmSettings(filename)
//
//        memScoped {
//            val dbm = dbm_open(filename, O_RDWR or O_CREAT, (S_IRUSR or S_IWUSR or S_IRGRP or S_IROTH).toUShort())
//                ?: error("error: $errno")
//            dbm.checkError()
//
//            val key = cValue<datum> {
//                val cValues = "key".encodeToByteArray().toCValues()
//                dptr = cValues.ptr
//                dsize = cValues.size.toSize_t()
//            }
//            val value = cValue<datum> {
//                val cValues = "value".encodeToByteArray().toCValues()
//                dptr = cValues.ptr
//                dsize = cValues.size.toSize_t()
//            }
//
//            if (dbm_store(dbm, key, value, DBM_REPLACE) == -1) {
//                dbm.checkError()
//            }
//
//            dbm_close(dbm)
//        }
//
//        assertEquals("value", settings["key", ""])
//    }
}

//private fun CValuesRef<DBM>.checkError() {
//    assertEquals(0, dbm_error(this))
//}
