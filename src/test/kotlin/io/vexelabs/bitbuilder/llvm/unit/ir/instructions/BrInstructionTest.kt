package io.vexelabs.bitbuilder.llvm.unit.ir.instructions

import io.vexelabs.bitbuilder.internal.cast
import io.vexelabs.bitbuilder.llvm.ir.Builder
import io.vexelabs.bitbuilder.llvm.ir.Context
import io.vexelabs.bitbuilder.llvm.ir.Module
import io.vexelabs.bitbuilder.llvm.ir.values.constants.ConstantInt
import io.vexelabs.bitbuilder.llvm.setup
import org.spekframework.spek2.Spek
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class BrInstructionTest : Spek({
    setup()

    val builder: Builder by memoized()
    val module: Module by memoized()
    val context: Context by memoized()

    test("create unconditional branch") {
        val i32 = context.getIntType(32)
        val fnTy = context.getFunctionType(i32, variadic = false)
        val function = module.createFunction("test", fnTy)
        val destination = function.createBlock("Entry")
        val subject = builder.createBr(destination)

        assertFalse { subject.isConditional() }
    }

    test("create conditional branch") {
        val i32 = context.getIntType(32)
        val i1 = context.getIntType(1)
        val fnTy = context.getFunctionType(i32, variadic = false)
        val function = module.createFunction("test", fnTy)
        val then = function.createBlock("then")
        val otherwise = function.createBlock("otherwise")
        val condition = i1.getConstant(1)

        val subject = builder.createCondBr(condition, then, otherwise)
        val foundCondition = cast<ConstantInt>(condition)

        assertEquals(condition.ref, foundCondition.ref)
        assertTrue { subject.isConditional() }
    }
})
