package top.fifthlight.stubgen

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.min

data class ClassMap(
    val classes: ConcurrentHashMap<String, ClassItem> = ConcurrentHashMap(),
    val cleanLock: Lock = ReentrantLock(),
    val removedClasses: MutableSet<String> = mutableSetOf(),
    var visitCount: AtomicInteger = AtomicInteger(0),
) {
    data class ClassInfo(
        var version: Int,
        val name: String,
        var access: Int,
        var superClass: String?,
        var interfaces: Set<String>,
        var signature: String? = null,
    ) {
        companion object {
            private const val OBJECT_NAME = "java/lang/Object"
        }

        val isInterface: Boolean
            get() = (access and Opcodes.ACC_INTERFACE) != 0
        val isRecord: Boolean
            get() = (access and Opcodes.ACC_RECORD) != 0
        val isFinal: Boolean
            get() = (access and Opcodes.ACC_FINAL) != 0

        init {
            if (!isInterface) {
                require(superClass != null) {
                    "Non-interface class $name have no super class"
                }
            }
        }

        fun isCompatible(other: ClassInfo): Boolean {
            if (this.isRecord != other.isRecord) {
                if (!other.isRecord && other.superClass != OBJECT_NAME) {
                    return false
                }
                if (!this.isRecord && this.superClass != OBJECT_NAME) {
                    return false
                }
            } else if (this.superClass != other.superClass) {
                return false
            }
            val accessMask = (Opcodes.ACC_RECORD or Opcodes.ACC_FINAL).inv()
            return (this.access and accessMask) == (other.access and accessMask)
        }

        fun mergeFrom(from: ClassInfo) {
            if (isRecord != from.isRecord) {
                access = access and Opcodes.ACC_RECORD.inv()
                superClass = OBJECT_NAME
            }
            if (isFinal != from.isFinal) {
                access = access and Opcodes.ACC_FINAL.inv()
            }
            interfaces = interfaces.intersect(from.interfaces)
            version = min(version, from.version)
            if (signature != from.signature) {
                this.signature = null
            }
        }
    }

    data class MethodIdentifier(
        val name: String,
        val descriptor: String,
    )

    data class MethodItem(
        val access: Int,
        var signature: String? = null,
        var visitCount: Int = 1,
    ) {
        val isAbstract: Boolean
            get() = (access and Opcodes.ACC_ABSTRACT) != 0
        val isNative: Boolean
            get() = (access and Opcodes.ACC_NATIVE) != 0

        fun isCompatible(other: MethodItem): Boolean = this.access == other.access

        fun mergeFrom(from: MethodItem) {
            if (signature != from.signature) {
                this.signature = null
            }
        }
    }

    data class FieldItem(
        val type: String,
        val access: Int,
        var staticValue: Any?,
        var signature: String? = null,
        var visitCount: Int = 1,
    ) {
        fun isCompatible(other: FieldItem): Boolean {
            if (this.type != other.type) {
                return false
            }
            if (this.access != other.access) {
                return false
            }
            return true
        }

        fun mergeFrom(from: FieldItem) {
            if (this.staticValue != from.staticValue) {
                this.staticValue = null
            }
            if (signature != from.signature) {
                this.signature = null
            }
        }
    }

    data class InnerClassItem(
        val name: String,
        val outerName: String,
        val innerName: String,
        val access: Int,
    )

    data class ClassItem(
        val info: ClassInfo,
        val methods: MutableMap<MethodIdentifier, MethodItem> = mutableMapOf(),
        val fields: MutableMap<String, FieldItem> = mutableMapOf(),
        val lock: Lock = ReentrantLock(),
        val innerClasses: MutableMap<String, InnerClassItem> = mutableMapOf(),
        var visitCount: Int = 1,
    ) {
        fun write(visitor: ClassVisitor, classVersion: Int, removedClasses: Set<String>) {
            visitor.visit(
                classVersion,
                info.access,
                info.name,
                info.signature,
                info.superClass,
                info.interfaces.toTypedArray()
            )
            for ((name, innerClass) in innerClasses) {
                if (removedClasses.contains(name)) {
                    continue
                }
                if (removedClasses.contains(innerClass.outerName)) {
                    continue
                }
                visitor.visitInnerClass(innerClass.name, innerClass.outerName, innerClass.innerName, innerClass.access)
            }
            for ((name, field) in fields) {
                visitor.visitField(field.access, name, field.type, field.signature, field.staticValue)
            }
            for ((identifier, method) in methods) {
                visitor.visitMethod(method.access, identifier.name, identifier.descriptor, method.signature, arrayOf())
                    ?.apply {
                        if (!method.isAbstract && !method.isNative) {
                            var arguments = Type.getMethodType(identifier.descriptor).argumentCount
                            visitCode()
                            visitTypeInsn(Opcodes.NEW, "java/lang/UnsupportedOperationException")
                            visitInsn(Opcodes.DUP)
                            visitLdcInsn("STUB!")
                            visitMethodInsn(
                                Opcodes.INVOKESPECIAL,
                                "java/lang/UnsupportedOperationException",
                                "<init>",
                                "(Ljava/lang/String;)V",
                                false
                            )
                            visitInsn(Opcodes.ATHROW)
                            visitMaxs(3, 1 + arguments)
                            visitEnd()
                        }
                    }
            }
            visitor.visitEnd()
        }
    }

    fun cleanUnvisitedItems() {
        this.cleanLock.withLock lockLabel@{
            val visitCount = this.visitCount.get()
            classes.values.removeIf { clazz ->
                clazz.lock.withLock {
                    if (clazz.visitCount < visitCount) {
                        removedClasses.add(clazz.info.name)
                        return@withLock true
                    }
                    clazz.methods.values.removeAll { method -> method.visitCount < visitCount }
                    clazz.fields.values.removeAll { field -> field.visitCount < visitCount }
                    false
                }
            }
        }
    }
}
