package io.vexelabs.bitbuilder.llvm.unit.internal

import io.vexelabs.bitbuilder.internal.fromLLVMBool
import io.vexelabs.bitbuilder.internal.toLLVMBool
import org.spekframework.spek2.Spek
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal object ConversionsTest : Spek({
    group("int to boolean conversion") {
        test("0 and 1 match false and true") {
            assertTrue { 1.fromLLVMBool() }
            assertFalse { 0.fromLLVMBool() }
        }

        test("any positive and negative values also convert") {
            assertTrue { 100.fromLLVMBool() }
            assertFalse { (-200123).fromLLVMBool() }
        }
    }

    group("boolean to int conversion") {
        test("true and false match 1 and 0") {
            assertEquals(1, true.toLLVMBool())
            assertEquals(0, false.toLLVMBool())
        }
    }
})
