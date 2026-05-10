package io.github.sandydunlop.markista.orchestration;

import io.github.sandydunlop.markista.core.Context;

import javax.tools.Diagnostic.Kind;

import io.github.qishr.cascara.lang.java.model.SemanticModel;
import io.github.qishr.cascara.lang.java.model.MethodNode;
import io.github.qishr.cascara.lang.java.model.TypeNode;
import io.github.qishr.cascara.lang.java.model.VariableTypeNode;
import io.github.qishr.cascara.lang.java.modeler.StandardModeler;
import io.github.qishr.cascara.lang.java.util.JreUtil;

/// Utilities for rxtrating information from the API model
public class ModelUtils {
    static SemanticModel api;
    static Context ctx;

    private ModelUtils() {
        // Nothing to see here
    }

    public static void init(SemanticModel a, Context c) {
        api = a;
        ctx = c;
    }

    public static String baseTypeName(MethodNode methodNode){
        StandardModeler modeller = new StandardModeler();
        TypeNode type = api.getTypeNode(methodNode.getOwnerName());
        if (type == null) {
            return null;
        }
        for (int i = type.getSupertypes().size() - 1; i >= 0; i--) {
            VariableTypeNode typeRef = type.getSupertypes().get(i);
            String typeName = typeRef.getRawTypeName().toString();
            TypeNode supertypeNode = api.getTypeNode(typeName);
            if (supertypeNode == null) {
                // It's not in the model, try to find in JRE
                Class<?> jreClass = JreUtil.loadClass(typeName);
                if (jreClass != null) {
                    supertypeNode = modeller.modelClass(jreClass);
                }
            }
            if (supertypeNode != null && typeHasMethod(supertypeNode, methodNode)) {
                return supertypeNode.getName().fullyQualifiedName();
            }
        }
        // MFLP-89 If we reach here, it should be an interface
        return null;
    }

    public static boolean typeHasMethod(TypeNode typeNode, MethodNode methodNode) {
        for (MethodNode inheritedMethod : typeNode.getMethods()) {
            if (canOverride(methodNode, inheritedMethod)) {
                return true;
            }
        }
        return false;
    }

    public static boolean canOverride(MethodNode methodA, MethodNode methodB) {
        if (!methodA.getName().simpleName().equals(methodB.getName().simpleName())) {
            return false;
        }
        VariableTypeNode[] paramTypesA = methodA.getParamTypes();
        VariableTypeNode[] paramTypesB = methodB.getParamTypes();

        // Check if parameter types are compatible
        if (paramTypesA.length == paramTypesB.length) {
            for (int i = 0; i < paramTypesA.length; i++) {
                if (!isSubtype(paramTypesA[i], paramTypesB[i])) {
                    return false; // If any parameter type is not a subtype, return false
                }
            }
            return true; // All parameter types are compatible
        }
        return false;
    }

    /// Check if typeA is a subtype of typeB
    public static boolean isSubtype(VariableTypeNode typeA, VariableTypeNode typeB) {
        if (typeA instanceof VariableTypeNode.Generic paramTypeA && typeB instanceof VariableTypeNode.Generic paramTypeB) {
            return parameterizedTypesAreCompatible(paramTypeA, paramTypeB);
        } else {
            return isAssignableFrom(typeA, typeB);
        }
    }

    /// Check if typeA is subtype of typeB
    public static boolean isAssignableFrom(VariableTypeNode typeA, VariableTypeNode typeB) {
        if (typeA.getRawTypeName().equals(typeB.getRawTypeName())) {
            return true;
        }
        if (typeA.getRawTypeName().equals("?")) {
            System.out.println("TODO: Need to verify wildcards: " + typeA.getRawTypeName());
            return false;
        }
        Class<?> classA = JreUtil.loadClass(typeA.getRawTypeName());
        Class<?> classB = JreUtil.loadClass(typeB.getRawTypeName());
        if (classA == null) {
            ctx.getReporter().print(Kind.WARNING, "Failed to load class:" + typeA.getRawTypeName());
            return false;
        }
        if (classB == null) {
            ctx.getReporter().print(Kind.WARNING, "Failed to load class:" + typeB.getRawTypeName());
            return false;
        }
        return classB.isAssignableFrom(classA);
    }

    private static boolean parameterizedTypesAreCompatible(VariableTypeNode.Generic paramTypeA, VariableTypeNode.Generic paramTypeB) {
        // Check raw types
        if (!paramTypeA.getRawTypeName().equals(paramTypeB.getRawTypeName())) {
            return false;
        }
        // Check the actual type arguments
        VariableTypeNode typeArgsA = paramTypeA.getParams();
        VariableTypeNode typeArgsB = paramTypeB.getParams();

        if (typeArgsA instanceof VariableTypeNode.Sequence sequenceA && typeArgsB instanceof VariableTypeNode.Sequence sequenceB) {
            for (int i=0; i<sequenceA.size(); i++) {
                VariableTypeNode typeArgsAtype = sequenceA.get(i);
                VariableTypeNode typeArgsBtype = sequenceB.get(i);
                if (!isSubtype(typeArgsAtype, typeArgsBtype)) {
                    return false;
                }
            }
            return true;
        } else if (!isSubtype(typeArgsA, typeArgsB)) {
            return false;
        }
        return true;
    }
}
