package com.guichaguri.fastbean;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import static org.objectweb.asm.Opcodes.*;

/**
 * @author Guichaguri
 */
public class BeanCompiler {

    private static final Type OBJECT = Type.getType(Object.class);
    private static final Type BEAN = Type.getType(Bean.class);
    private static final Type MAPPER = Type.getType(IPropertyMapper.class);

    private ClassWriter cw;
    private MethodVisitor mv;
    private Label start;

    private Type type;
    private Type objectType;

    public BeanCompiler(Class object, String className) {
        cw = new ClassWriter(0);

        String internalName = className.replace('.', '/');
        type = Type.getObjectType(internalName);
        objectType = Type.getType(object);

        cw.visit(52, ACC_PUBLIC + ACC_SUPER,
                internalName, null,
                OBJECT.getInternalName(),
                new String[]{BEAN.getInternalName()});

        cw.visitSource(className, null);

        createConstructor();
        createSyntheticMethod();
        startMethod();
    }

    public byte[] compile() {
        endMethod();

        cw.visitEnd();
        return cw.toByteArray();
    }

    private void createConstructor() {
        Label start = new Label();
        Label end = new Label();

        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitLabel(start);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, OBJECT.getInternalName(), "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitLabel(end);

        mv.visitLocalVariable("this", type.getDescriptor(), null, start, end, 0);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }

    private void createSyntheticMethod() {
        Label start = new Label();
        Label end = new Label();

        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC + ACC_BRIDGE + ACC_SYNTHETIC, "create", Type.getMethodDescriptor(OBJECT, MAPPER), null, null);
        mv.visitCode();
        mv.visitLabel(start);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, type.getInternalName(), "create", Type.getMethodDescriptor(objectType, MAPPER), false);
        mv.visitInsn(ARETURN);
        mv.visitLabel(end);

        mv.visitLocalVariable("this", type.getDescriptor(), null, start, end, 0);
        mv.visitMaxs(2, 2);
        mv.visitEnd();
    }

    private void startMethod() {
        start = new Label();

        mv = cw.visitMethod(ACC_PUBLIC, "create", Type.getMethodDescriptor(objectType, MAPPER), null, null);
        mv.visitCode();
        mv.visitLabel(start);

        mv.visitTypeInsn(NEW, objectType.getInternalName());
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, objectType.getInternalName(), "<init>", "()V", false);
        mv.visitVarInsn(ASTORE, 2);
    }

    private void endMethod() {
        Label end = new Label();

        mv.visitVarInsn(ALOAD, 2);
        mv.visitInsn(ARETURN);
        mv.visitLabel(end);

        mv.visitLocalVariable("this", type.getDescriptor(), null, start, end, 0);
        mv.visitLocalVariable("mapper", MAPPER.getDescriptor(), null, start, end, 1);
        mv.visitLocalVariable("obj", objectType.getDescriptor(), null, start, end, 2);
        mv.visitMaxs(3, 3);
        mv.visitEnd();
    }

    public void addMethodCall(Method method, String name) {
        Class parameter = method.getParameterTypes()[0];

        mv.visitVarInsn(ALOAD, 2);

        addGetter(parameter, name);

        mv.visitMethodInsn(INVOKEVIRTUAL, objectType.getInternalName(), method.getName(), Type.getMethodDescriptor(method), false);

        if(method.getReturnType() != void.class) {
            mv.visitInsn(POP);
        }
    }

    public void addFieldPut(Field field, String name) {
        Class parameter = field.getType();

        mv.visitVarInsn(ALOAD, 2);

        addGetter(parameter, name);

        mv.visitFieldInsn(PUTFIELD, objectType.getInternalName(), field.getName(), Type.getDescriptor(parameter));
    }

    private void addGetter(Class type, String name) {
        String mapperName = MAPPER.getInternalName();

        mv.visitVarInsn(ALOAD, 1);
        mv.visitLdcInsn(name);

        if(type == int.class) {
            mv.visitMethodInsn(INVOKEINTERFACE, mapperName, "getInt", "(Ljava/lang/String;)I", true);
        } else if(type == short.class) {
            mv.visitMethodInsn(INVOKEINTERFACE, mapperName, "getShort", "(Ljava/lang/String;)S", true);
        } else if(type == long.class) {
            mv.visitMethodInsn(INVOKEINTERFACE, mapperName, "getLong", "(Ljava/lang/String;)J", true);
        } else if(type == double.class) {
            mv.visitMethodInsn(INVOKEINTERFACE, mapperName, "getDouble", "(Ljava/lang/String;)D", true);
        } else if(type == float.class) {
            mv.visitMethodInsn(INVOKEINTERFACE, mapperName, "getFloat", "(Ljava/lang/String;)F", true);
        } else if(type == byte.class) {
            mv.visitMethodInsn(INVOKEINTERFACE, mapperName, "getByte", "(Ljava/lang/String;)B", true);
        } else if(type == boolean.class) {
            mv.visitMethodInsn(INVOKEINTERFACE, mapperName, "getBoolean", "(Ljava/lang/String;)Z", true);
        } else if(type == char.class) {
            mv.visitMethodInsn(INVOKEINTERFACE, mapperName, "getChar", "(Ljava/lang/String;)C", true);
        } else if(type == String.class) {
            mv.visitMethodInsn(INVOKEINTERFACE, mapperName, "getString", "(Ljava/lang/String;)Ljava/lang/String;", true);
        } else {
            mv.visitMethodInsn(INVOKEINTERFACE, mapperName, "getProperty", "(Ljava/lang/String;)Ljava/lang/Object;", true);
            if(type != Object.class) mv.visitTypeInsn(CHECKCAST, Type.getInternalName(type));
        }
    }

}
