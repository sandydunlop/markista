package io.github.sandydunlop.markista.model;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.Modifier;


public class Node {
    public Node owner = null;
    public Set<Modifier> modifiers = new HashSet<>();
    public Deprecation deprecation = Deprecation.NONE;
    public String simpleName = "";
    public String qualifiedName = "";
    public String packageName = "";
    public String firstSentence = "";
    public String description = "";

    public String getModifiers() {
        String mods = "";
        List<Modifier> modifierList = ModifierSorter.sortModifiers(modifiers);
        for (Modifier mod : modifierList) {
            if (mod != Modifier.PUBLIC) {
                mods += mod.toString() + " ";
            }
        }
        return mods;
    }

    public void sortModifiers() {

        ModifierSorter.sortModifiers(modifiers);
    }

    public class ModifierSorter {
        private static final List<Modifier> ORDER = Arrays.asList(
            Modifier.PUBLIC, Modifier.PROTECTED, Modifier.PRIVATE,
            Modifier.STATIC, Modifier.FINAL, Modifier.ABSTRACT,
            Modifier.SYNCHRONIZED, Modifier.TRANSIENT, Modifier.VOLATILE,
            Modifier.NATIVE, Modifier.STRICTFP, Modifier.DEFAULT
        );

        public static List<Modifier> sortModifiers(Set<Modifier> modifierSet) {
            List<Modifier> modifierList = new ArrayList<>(modifierSet);
            modifierList.sort(Comparator.comparingInt(o -> ORDER.indexOf(o)));
            return modifierList;
        }
    }
}
