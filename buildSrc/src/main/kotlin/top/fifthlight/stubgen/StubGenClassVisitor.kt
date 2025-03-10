package top.fifthlight.stubgen

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import kotlin.concurrent.withLock

class StubGenClassVisitor(
    private val classMap: ClassMap,
) : ClassVisitor(Opcodes.ASM9, null) {
    private var classItem: ClassMap.ClassItem? = null

    override fun visit(
        version: Int,
        access: Int,
        name: String,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        if (name == "module-info") {
            return
        }
        if (name == "package-info") {
            return
        }
        if (access and Opcodes.ACC_PRIVATE != 0) {
            return
        }
        val classInfo = ClassMap.ClassInfo(
            name = name,
            access = access,
            superClass = superName,
            interfaces = interfaces?.toMutableSet() ?: mutableSetOf(),
            signature = signature,
            version = version,
        )
        classItem = classMap.classes.compute(name) { _, existingItem ->
            if (existingItem == null) {
                ClassMap.ClassItem(classInfo)
            } else {
                existingItem.lock.withLock {
                    if (existingItem.info.isCompatible(classInfo)) {
                        existingItem.info.mergeFrom(classInfo)
                        existingItem
                    } else {
                        null
                    }
                }
            }
        }
    }

    override fun visitInnerClass(name: String, outerName: String?, innerName: String?, access: Int) {
        if (outerName == null || innerName == null) {
            return
        }
        if (access and Opcodes.ACC_PRIVATE != 0) {
            return
        }
        classItem?.let { classItem ->
            val innerClassItem = ClassMap.InnerClassItem(
                name = name,
                outerName = outerName,
                innerName = innerName,
                access = access,
            )
            classItem.lock.withLock {
                classItem.innerClasses[name] = innerClassItem
            }
        }
    }

    override fun visitOuterClass(owner: String, name: String?, descriptor: String?) {
        classItem = null
    }

    override fun visitField(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        value: Any?,
    ): FieldVisitor? {
        if (access and Opcodes.ACC_PRIVATE != 0) {
            return null
        }
        classItem?.let { classItem ->
            val fieldItem = ClassMap.FieldItem(
                type = descriptor,
                access = access,
                staticValue = value,
                signature = signature,
            )
            classItem.lock.withLock {
                val existingItem = classItem.fields[name]
                if (existingItem != null) {
                    if (existingItem.isCompatible(fieldItem)) {
                        existingItem.visitCount++
                        existingItem.mergeFrom(fieldItem)
                    } else {
                        classItem.fields.remove(name)
                    }
                } else {
                    classItem.fields[name] = fieldItem
                }
            }
        }
        return null
    }

    override fun visitMethod(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<out String>?,
    ): MethodVisitor? {
        if (access and Opcodes.ACC_PRIVATE != 0) {
            return null
        }
        classItem?.let { classItem ->
            val methodIdentifier = ClassMap.MethodIdentifier(
                name = name,
                descriptor = descriptor,
            )
            val methodItem = ClassMap.MethodItem(
                access = access,
                signature = signature,
            )
            classItem.lock.withLock {
                val existingItem = classItem.methods[methodIdentifier]
                if (existingItem != null) {
                    if (existingItem.isCompatible(methodItem)) {
                        existingItem.visitCount++
                        existingItem.mergeFrom(methodItem)
                    } else {
                        classItem.methods.remove(methodIdentifier)
                    }
                } else {
                    classItem.methods[methodIdentifier] = methodItem
                }
            }
        }
        return null
    }

    override fun visitEnd() {
        classItem?.let { classItem ->
            classItem.lock.withLock {
                classItem.visitCount++
            }
        }
    }
}
