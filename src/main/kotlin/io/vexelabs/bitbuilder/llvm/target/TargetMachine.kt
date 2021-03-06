package io.vexelabs.bitbuilder.llvm.target

import io.vexelabs.bitbuilder.internal.resourceScope
import io.vexelabs.bitbuilder.internal.toLLVMBool
import io.vexelabs.bitbuilder.internal.toResource
import io.vexelabs.bitbuilder.llvm.internal.contracts.ContainsReference
import io.vexelabs.bitbuilder.llvm.internal.contracts.Disposable
import io.vexelabs.bitbuilder.llvm.ir.Module
import io.vexelabs.bitbuilder.llvm.support.MemoryBuffer
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.llvm.LLVM.LLVMMemoryBufferRef
import org.bytedeco.llvm.LLVM.LLVMTargetMachineRef
import org.bytedeco.llvm.global.LLVM
import java.io.File

/**
 * Interface to llvm::TargetMachine
 *
 * A target machine is a target triple which may own multiple [Target]s which
 * it will be capable of compiling to.
 *
 * @see LLVMTargetMachineRef
 */
public class TargetMachine internal constructor() :
    ContainsReference<LLVMTargetMachineRef>, Disposable {
    public override lateinit var ref: LLVMTargetMachineRef
        internal set
    public override var valid: Boolean = true

    public constructor(llvmRef: LLVMTargetMachineRef) : this() {
        ref = llvmRef
    }

    /**
     * Get the start of the target iterator
     *
     * @see LLVM.LLVMGetFirstTarget
     */
    public fun getTargetIterator(): Target.Iterator? {
        val target = LLVM.LLVMGetFirstTarget()

        return target?.let { Target.Iterator(it) }
    }

    /**
     * Get the target for this target machine
     *
     * @see LLVM.LLVMGetTargetMachineTarget
     */
    public fun getTarget(): Target {
        val target = LLVM.LLVMGetTargetMachineTarget(ref)

        return Target(target)
    }

    /**
     * Get the target triple
     *
     * @see LLVM.LLVMGetTargetMachineTriple
     */
    public fun getTriple(): String {
        return LLVM.LLVMGetTargetMachineTriple(ref).string
    }

    /**
     * Get the target cpu
     *
     * @see LLVM.LLVMGetTargetMachineCPU
     */
    public fun getCPU(): String {
        return LLVM.LLVMGetTargetMachineCPU(ref).string
    }

    /**
     * Get the target features
     *
     * @see LLVM.LLVMGetTargetMachineFeatureString
     */
    public fun getFeatures(): String {
        return LLVM.LLVMGetTargetMachineFeatureString(ref).string
    }

    /**
     * Set whether target uses verbose asm
     *
     * @see LLVM.LLVMSetTargetMachineAsmVerbosity
     */
    public fun setAsmVerbosity(verboseAsm: Boolean) {
        LLVM.LLVMSetTargetMachineAsmVerbosity(ref, verboseAsm.toLLVMBool())
    }

    /**
     * Emit the given [module] to the target [file]
     *
     * This emits the given [module] into either an assembly file or an
     * object file
     *
     * @see LLVM.LLVMTargetMachineEmitToFile
     * @throws RuntimeException
     */
    public fun emitToFile(
        module: Module,
        file: File,
        fileType: CodeGenFileType
    ) {
        val buf = BytePointer(256).toResource()

        resourceScope(buf) {
            val filePath = BytePointer(file.absolutePath)
            val result = LLVM.LLVMTargetMachineEmitToFile(
                ref,
                module.ref,
                filePath,
                fileType.value,
                it
            )

            if (result != 0) {
                throw RuntimeException(it.string)
            }
        }
    }

    /**
     * Emit the given [module] to a memory buffer
     *
     * This compiles the given [module] into either an assembly file or an
     * object file
     *
     * @see LLVM.LLVMTargetMachineEmitToMemoryBuffer
     * @throws RuntimeException
     */
    public fun emitToMemoryBuffer(
        module: Module,
        fileType: CodeGenFileType
    ): MemoryBuffer {
        val buf = BytePointer(256).toResource()

        return resourceScope(buf) {
            val outBuf = LLVMMemoryBufferRef()
            val result = LLVM.LLVMTargetMachineEmitToMemoryBuffer(
                ref,
                module.ref,
                fileType.value,
                it,
                outBuf
            )

            if (result != 0) {
                outBuf.deallocate()
                throw RuntimeException(it.string)
            }

            return@resourceScope MemoryBuffer(outBuf)
        }
    }

    /**
     * Create a target data layout from a target machine
     *
     * @see LLVM.LLVMCreateTargetDataLayout
     */
    public fun createTargetData(): TargetData {
        val ref = LLVM.LLVMCreateTargetDataLayout(ref)

        return TargetData(ref)
    }

    public override fun dispose() {
        require(valid) { "This target machine has already been disposed." }

        valid = false

        LLVM.LLVMDisposeTargetMachine(ref)
    }
}
