package io.github.sandydunlop.markista.orchestration;

import io.github.sandydunlop.markista.core.Context;
import io.github.sandydunlop.markista.model.Api;
import io.github.sandydunlop.markista.model.MethodNode;
import io.github.sandydunlop.markista.model.TypeNode;
import io.github.sandydunlop.markista.model.TypeReference;
import io.github.sandydunlop.markista.modelling.StandardModeller;

/// Utilities for rxtrating information from the API model
public class ModelUtils {
    static Api api;
    static Context ctx;

    private ModelUtils() {
        // Nothing to see here
    }

    public static void init(Api a, Context c) {
        api = a;
        ctx = c;
    }

    public static String baseTypeName(MethodNode methodNode){
        StandardModeller modeller = new StandardModeller();
        TypeNode type = api.getTypeNode(methodNode.getOwnerName());
        if (type == null) {
            return null;
        }
        for (int i = type.getSupertypes().size() - 1; i >= 0; i--) {
            TypeReference typeRef = type.getSupertypes().get(i);
            String typeName = typeRef.getRawTypeName();
            TypeNode supertypeNode = api.getTypeNode(typeName);
            if (supertypeNode == null) {
                // It's not in the model, try to find in JRE
                Class<?> jreClass = JreUtils.loadClass(typeName);
                if (jreClass != null) {
                    supertypeNode = modeller.modelClass(jreClass);
                }
            }
            if (typeHasMethod(supertypeNode, methodNode)) {
                return supertypeNode.getQualifiedName();
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
        if (!methodA.getSimpleName().equals(methodB.getSimpleName())) {
            return false;
        }
        TypeReference[] paramTypesA = methodA.getParamTypes();
        TypeReference[] paramTypesB = methodB.getParamTypes();

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
    public static boolean isSubtype(TypeReference typeA, TypeReference typeB) {
        if (typeA instanceof TypeReference.Generic paramTypeA && typeB instanceof TypeReference.Generic paramTypeB) {
            return parameterizedTypesAreCompatible(paramTypeA, paramTypeB);
        } else {
            return isAssignableFrom(typeA, typeB);
        }
    }

    /// Check if typeA is subtype of typeB
    public static boolean isAssignableFrom(TypeReference typeA, TypeReference typeB) {
        Class<?> classA = JreUtils.loadClass(typeA.getRawTypeName());
        Class<?> classB = JreUtils.loadClass(typeB.getRawTypeName());
        return classB.isAssignableFrom(classA);
    }

    private static boolean parameterizedTypesAreCompatible(TypeReference.Generic paramTypeA, TypeReference.Generic paramTypeB) {
        // Check raw types
        if (!paramTypeA.getRawTypeName().equals(paramTypeB.getRawTypeName())) {
            return false;
        }
        // Check the actual type arguments
        TypeReference typeArgsA = paramTypeA.getParams();
        TypeReference typeArgsB = paramTypeB.getParams();

        if (typeArgsA instanceof TypeReference.Sequence sequenceA && typeArgsB instanceof TypeReference.Sequence sequenceB) {
            for (int i=0; i<sequenceA.size(); i++) {
                TypeReference typeArgsAtype = sequenceA.get(i);
                TypeReference typeArgsBtype = sequenceB.get(i);
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
