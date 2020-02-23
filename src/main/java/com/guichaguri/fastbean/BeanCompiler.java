package com.guichaguri.fastbean;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
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
    private static final Type GETTER = Type.getType(IPropertyGetter.class);
    private static final Type SETTER = Type.getType(IPropertySetter.class);

    private ClassWriter cw;

    private MethodVisitor fill;
    private MethodVisitor extract;

    private Label fillStart = new Label();
    private Label extractStart = new Label();

    private Type type;
    private Type objectType;

    public BeanCompiler(Class<?> object, String className) {
        cw = new ClassWriter(0);

        String internalName = className.replace('.', '/');
        type = Type.getObjectType(internalName);
        objectType = Type.getType(object);

        cw.visit(52, ACC_PUBLIC + ACC_SUPER, internalName,
                OBJECT.getDescriptor() + "L" + BEAN.getInternalName() + "<" + objectType.getDescriptor() + ">;",
                OBJECT.getInternalName(), new String[]{BEAN.getInternalName()});

        cw.visitSource(className, null);

        generateConstructor();

        generateCreateBridge();

        generateSyntheticMethod("fill", GETTER);
        generateSyntheticMethod("extract", SETTER);

        fill = startConversionMethod("fill", GETTER, fillStart);
        extract = startConversionMethod("extract", SETTER, extractStart);
    }

    public byte[] compile() {
        endConversionMethod(fill, GETTER, fillStart);
        endConversionMethod(extract, SETTER, extractStart);

        cw.visitEnd();
        return cw.toByteArray();
    }

    private void generateConstructor() {
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

    private void generateSyntheticMethod(String name, Type argument) {
        Label start = new Label();
        Label end = new Label();

        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC + ACC_BRIDGE + ACC_SYNTHETIC, name, Type.getMethodDescriptor(Type.VOID_TYPE, OBJECT, argument), null, null);
        mv.visitCode();

        mv.visitLabel(start);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitTypeInsn(CHECKCAST, objectType.getInternalName());
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(INVOKEVIRTUAL, type.getInternalName(), name, Type.getMethodDescriptor(Type.VOID_TYPE, objectType, argument), false);
        mv.visitInsn(RETURN);
        mv.visitLabel(end);

        mv.visitLocalVariable("this", type.getDescriptor(), null, start, end, 0);
        mv.visitMaxs(3, 3);
        mv.visitEnd();
    }

    private MethodVisitor startConversionMethod(String name, Type argument, Label start) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, name, Type.getMethodDescriptor(Type.VOID_TYPE, objectType, argument), null, null);
        mv.visitCode();
        mv.visitLabel(start);

        return mv;
    }

    private void endConversionMethod(MethodVisitor mv, Type argument, Label start) {
        Label end = new Label();

        mv.visitLabel(end);
        mv.visitInsn(RETURN);
        mv.visitLabel(end);

        mv.visitLocalVariable("this", type.getDescriptor(), null, start, end, 0);
        mv.visitLocalVariable("instance", objectType.getDescriptor(), null, start, end, 1);
        mv.visitLocalVariable("converter", argument.getDescriptor(), null, start, end, 2);
        mv.visitMaxs(3, 3);
        mv.visitEnd();
    }

    public void generateCreateMethod(Constructor<?> constructor) {
        int stackSize = 2;
        Label start = new Label();
        Label end = new Label();
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "create", Type.getMethodDescriptor(objectType), null, null);
        mv.visitCode();

        mv.visitLabel(start);

        if (constructor != null) {
            // return new T();
            mv.visitTypeInsn(NEW, objectType.getInternalName());
            mv.visitInsn(DUP);

            // Sets all parameters to the default value (0, false, null)
            for (Class<?> type : constructor.getParameterTypes()) {
                if (type == int.class || type == short.class || type == boolean.class || type == char.class || type == byte.class) {
                    mv.visitInsn(ICONST_0);
                } else if (type == float.class) {
                    mv.visitInsn(FCONST_0);
                } else if (type == double.class) {
                    mv.visitInsn(DCONST_0);
                } else if (type == long.class) {
                    mv.visitInsn(LCONST_0);
                } else {
                    mv.visitInsn(ACONST_NULL);
                }
                stackSize++;
            }

            mv.visitMethodInsn(INVOKESPECIAL, objectType.getInternalName(), "<init>", Type.getConstructorDescriptor(constructor), false);
        } else {
            // return null;
            mv.visitInsn(ACONST_NULL);
        }

        mv.visitInsn(ARETURN);
        mv.visitLabel(end);

        mv.visitLocalVariable("this", type.getDescriptor(), null, start, end, 0);
        mv.visitMaxs(stackSize, 1);
        mv.visitEnd();
    }

    private void generateCreateBridge() {
        Label start = new Label();
        Label end = new Label();
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC + ACC_BRIDGE + ACC_SYNTHETIC, "create", Type.getMethodDescriptor(OBJECT), null, null);
        mv.visitCode();

        // return this.create(obj)
        mv.visitLabel(start);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKEVIRTUAL, type.getInternalName(), "create", Type.getMethodDescriptor(objectType), false);
        mv.visitInsn(ARETURN);
        mv.visitLabel(end);

        mv.visitLocalVariable("this", type.getDescriptor(), null, start, end, 0);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }

    public void addGetter(Class<?> type, String name, Member member) {
        String mapperName = GETTER.getInternalName();

        fill.visitVarInsn(ALOAD, 1);
        fill.visitVarInsn(ALOAD, 2);
        fill.visitLdcInsn(name);

        if(type == int.class) {
            fill.visitMethodInsn(INVOKEINTERFACE, mapperName, "getInt", "(Ljava/lang/String;)I", true);
        } else if(type == short.class) {
            fill.visitMethodInsn(INVOKEINTERFACE, mapperName, "getShort", "(Ljava/lang/String;)S", true);
        } else if(type == long.class) {
            fill.visitMethodInsn(INVOKEINTERFACE, mapperName, "getLong", "(Ljava/lang/String;)J", true);
        } else if(type == double.class) {
            fill.visitMethodInsn(INVOKEINTERFACE, mapperName, "getDouble", "(Ljava/lang/String;)D", true);
        } else if(type == float.class) {
            fill.visitMethodInsn(INVOKEINTERFACE, mapperName, "getFloat", "(Ljava/lang/String;)F", true);
        } else if(type == byte.class) {
            fill.visitMethodInsn(INVOKEINTERFACE, mapperName, "getByte", "(Ljava/lang/String;)B", true);
        } else if(type == boolean.class) {
            fill.visitMethodInsn(INVOKEINTERFACE, mapperName, "getBoolean", "(Ljava/lang/String;)Z", true);
        } else if(type == char.class) {
            fill.visitMethodInsn(INVOKEINTERFACE, mapperName, "getChar", "(Ljava/lang/String;)C", true);
        } else if(type == String.class) {
            fill.visitMethodInsn(INVOKEINTERFACE, mapperName, "getString", "(Ljava/lang/String;)Ljava/lang/String;", true);
        } else {
            // Gets the object and then casts it to the expected type
            fill.visitMethodInsn(INVOKEINTERFACE, mapperName, "getObject", "(Ljava/lang/String;)Ljava/lang/Object;", true);
            if(type != Object.class) fill.visitTypeInsn(CHECKCAST, Type.getInternalName(type));
        }

        if (member instanceof Field) {
            Field f = (Field) member;

            fill.visitFieldInsn(PUTFIELD, objectType.getInternalName(), f.getName(), Type.getDescriptor(type));
        } else if (member instanceof Method) {
            Method m = (Method) member;

            fill.visitMethodInsn(INVOKEVIRTUAL, objectType.getInternalName(), m.getName(), Type.getMethodDescriptor(m), false);

            if(m.getReturnType() != void.class) {
                fill.visitInsn(POP);
            }
        } else {
            throw new RuntimeException("Unknown member type");
        }
    }

    public void addSetter(Class<?> type, String name, Member member) {
        String mapperName = SETTER.getInternalName();

        extract.visitVarInsn(ALOAD, 2);
        extract.visitLdcInsn(name);
        extract.visitVarInsn(ALOAD, 1);

        if (member instanceof Field) {
            Field f = (Field) member;

            extract.visitFieldInsn(GETFIELD, objectType.getInternalName(), f.getName(), Type.getDescriptor(type));
        } else if (member instanceof Method) {
            Method m = (Method) member;

            extract.visitMethodInsn(INVOKEVIRTUAL, objectType.getInternalName(), m.getName(), Type.getMethodDescriptor(m), false);
        } else {
            throw new RuntimeException("Unknown member type");
        }

        if(type == int.class) {
            extract.visitMethodInsn(INVOKEINTERFACE, mapperName, "setInt", "(Ljava/lang/String;I)V", true);
        } else if(type == short.class) {
            extract.visitMethodInsn(INVOKEINTERFACE, mapperName, "setShort", "(Ljava/lang/String;S)V", true);
        } else if(type == long.class) {
            extract.visitMethodInsn(INVOKEINTERFACE, mapperName, "setLong", "(Ljava/lang/String;J)V", true);
        } else if(type == double.class) {
            extract.visitMethodInsn(INVOKEINTERFACE, mapperName, "setDouble", "(Ljava/lang/String;D)V", true);
        } else if(type == float.class) {
            extract.visitMethodInsn(INVOKEINTERFACE, mapperName, "setFloat", "(Ljava/lang/String;F)V", true);
        } else if(type == byte.class) {
            extract.visitMethodInsn(INVOKEINTERFACE, mapperName, "setByte", "(Ljava/lang/String;B)V", true);
        } else if(type == boolean.class) {
            extract.visitMethodInsn(INVOKEINTERFACE, mapperName, "setBoolean", "(Ljava/lang/String;Z)V", true);
        } else if(type == char.class) {
            extract.visitMethodInsn(INVOKEINTERFACE, mapperName, "setChar", "(Ljava/lang/String;C)V", true);
        } else if(type == String.class) {
            extract.visitMethodInsn(INVOKEINTERFACE, mapperName, "setString", "(Ljava/lang/String;Ljava/lang/String;)V", true);
        } else {
            extract.visitMethodInsn(INVOKEINTERFACE, mapperName, "setObject", "(Ljava/lang/String;Ljava/lang/Object;)V", true);
        }
    }

}
