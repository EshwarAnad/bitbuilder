package io.vexelabs.bitbuilder.llvm.integration.jni

import io.vexelabs.bitbuilder.llvm.executionengine.GenericValue
import io.vexelabs.bitbuilder.llvm.ir.CallConvention
import io.vexelabs.bitbuilder.llvm.ir.Context
import io.vexelabs.bitbuilder.llvm.ir.IntPredicate
import io.vexelabs.bitbuilder.llvm.ir.PassManager
import io.vexelabs.bitbuilder.llvm.support.VerifierFailureAction
import org.bytedeco.llvm.global.LLVM
import org.spekframework.spek2.Spek
import kotlin.test.assertEquals

internal object Factorial : Spek({
    test("translated factorial example") {
        // any call to LLVM.x is not implemented for bitbuilder-llvm yet
        LLVM.LLVMLinkInMCJIT()
        LLVM.LLVMInitializeNativeAsmPrinter()
        LLVM.LLVMInitializeNativeAsmParser()
        LLVM.LLVMInitializeNativeDisassembler()
        LLVM.LLVMInitializeNativeTarget()

        val ctx = Context()
        val module = ctx.createModule("factorial")
        val builder = ctx.createBuilder()
        val i32 = ctx.getIntType(32)

        val factorialType = ctx.getFunctionType(
            i32,
            i32,
            variadic = false
        )
        val factorial = module.createFunction("factorial", factorialType).apply {
            setCallConvention(CallConvention.CCall)
        }

        val n = factorial.getParameter(0)
        val entry = factorial.createBlock("entry")
        val then = factorial.createBlock("then")
        val otherwise = factorial.createBlock("otherwise")
        val exit = factorial.createBlock("exit")

        builder.apply {
            setPositionAtEnd(entry) // enter function

            val condition = createICmp(
                lhs = n,
                predicate = IntPredicate.EQ,
                rhs = i32.getConstant(0),
                variable = "n == 0"
            ) // compare param n with 0

            val resultIfTrue = i32.getConstant(1)

            // jump based on condition
            createCondBr(condition, then, otherwise)
            setPositionAtEnd(then) // enter then block
            createBr(exit) // jump to exit
            setPositionAtEnd(otherwise) // enter otherwise block

            val nMinusOne = createSub(
                lhs = n,
                rhs = i32.getConstant(1),
                variable = "n - 1"
            ) // subtract 1 from n
            val recursiveCall = createCall(
                function = factorial,
                arguments = listOf(nMinusOne),
                variable = "factorial(n - 1)"
            ) // call self recursively
            val resultIfFalse = createMul(
                lhs = n,
                rhs = recursiveCall,
                variable = "n * factorial(n - 1)"
            )

            createBr(exit) // jump to exit block
            setPositionAtEnd(exit)

            val result = createPhi(
                incoming = i32,
                variable = "result"
            ).apply {
                addIncoming(
                    values = listOf(resultIfTrue, resultIfFalse),
                    blocks = listOf(then, otherwise)
                )
            }
            createRet(result)
        }

        module.verify(VerifierFailureAction.PrintMessage)

        val compiler = module.createJITCompiler(2)
        val pass = PassManager(
            LLVM.LLVMCreatePassManager()
        )

        LLVM.LLVMAddConstantPropagationPass(pass.ref)
        LLVM.LLVMAddInstructionCombiningPass(pass.ref)
        LLVM.LLVMAddPromoteMemoryToRegisterPass(pass.ref)
        LLVM.LLVMAddGVNPass(pass.ref)
        LLVM.LLVMAddCFGSimplificationPass(pass.ref)
        LLVM.LLVMRunPassManager(pass.ref, module.ref)

        val args = GenericValue(
            type = i32,
            number = 10L, // factorial(10),
            isSigned = false
        )

        val genericValueResult = compiler.runFunction(
            function = factorial,
            values = listOf(args)
        )
        val result = genericValueResult.toInt(true)

        assertEquals(3628800, result)
    }
})
