package io.github.sandydunlop.markista.model;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.Modifier;

import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.SeeTree;


public class Node {
    public Node owner = null;
    public Set<Modifier> modifiers = new HashSet<>();
    public Deprecation deprecation = Deprecation.NONE;
    public Text deprecationText = Text.empty();
    public String simpleName = "";
    public String qualifiedName = "";
    public String packageName = "";
    public Text since = Text.empty();

    private List<? extends DocTree> firstSentence = new ArrayList<>();
    private List<? extends DocTree> body = new ArrayList<>();
    private List<? extends DocTree> fullBody = new ArrayList<>();
    private List<Reference> references = new ArrayList<>();

    public void setFirstSentence(List<? extends DocTree> doc) {
        firstSentence = doc;
    }

    public List<? extends DocTree> getFirstSentence() {
        return firstSentence;
    }

    public void setBody(List<? extends DocTree> doc) {
        body = doc;
    }

    public List<? extends DocTree> getBody() {
        return body;
    }

    public void setFullBody(List<? extends DocTree> doc) {
        fullBody = doc;
    }

    public List<? extends DocTree> getFullBody() {
        return fullBody;
    }

    public void setReferences(List<Reference> refs) {
        references = refs;
    }

    public List<Reference> getReferences() {
        return references;
    }

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
