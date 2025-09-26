package io.github.sandydunlop.markista.orchestration;

import io.github.sandydunlop.markista.core.Context;

import io.github.sandydunlop.cascara.model.Api;
import io.github.sandydunlop.cascara.model.MethodNode;
import io.github.sandydunlop.cascara.model.TypeNode;
import io.github.sandydunlop.cascara.model.VariableType;
import io.github.sandydunlop.cascara.modelling.StandardModeller;
import io.github.sandydunlop.cascara.jreutil.JreUtil;

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
            VariableType typeRef = type.getSupertypes().get(i);
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
        VariableType[] paramTypesA = methodA.getParamTypes();
        VariableType[] paramTypesB = methodB.getParamTypes();

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
    public static boolean isSubtype(VariableType typeA, VariableType typeB) {
        if (typeA instanceof VariableType.Generic paramTypeA && typeB instanceof VariableType.Generic paramTypeB) {
            return parameterizedTypesAreCompatible(paramTypeA, paramTypeB);
        } else {
            return isAssignableFrom(typeA, typeB);
        }
    }

    /// Check if typeA is subtype of typeB
    public static boolean isAssignableFrom(VariableType typeA, VariableType typeB) {
        Class<?> classA = JreUtil.loadClass(typeA.getRawTypeName().toString());
        Class<?> classB = JreUtil.loadClass(typeB.getRawTypeName().toString());
        return classB.isAssignableFrom(classA);
    }

    private static boolean parameterizedTypesAreCompatible(VariableType.Generic paramTypeA, VariableType.Generic paramTypeB) {
        // Check raw types
        if (!paramTypeA.getRawTypeName().equals(paramTypeB.getRawTypeName())) {
            return false;
        }
        // Check the actual type arguments
        VariableType typeArgsA = paramTypeA.getParams();
        VariableType typeArgsB = paramTypeB.getParams();

        if (typeArgsA instanceof VariableType.Sequence sequenceA && typeArgsB instanceof VariableType.Sequence sequenceB) {
            for (int i=0; i<sequenceA.size(); i++) {
                VariableType typeArgsAtype = sequenceA.get(i);
                VariableType typeArgsBtype = sequenceB.get(i);
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
