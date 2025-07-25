package io.github.sandydunlop.markista.model;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Node {
    protected String simpleName = "";
    protected String qualifiedName = "";
    protected PackageNode packageNode = null;
    private Set<Modifier> modifiers = new HashSet<>();
    private Deprecation deprecation = Deprecation.NONE;
    private Text deprecationText = Text.empty();
    private Text since = Text.empty();
    protected Text firstSentence = Text.empty();
    private Text body = Text.empty();
    private Text fullBody = Text.empty();
    private List<Reference> references = new ArrayList<>();

    public void addModifier(Modifier mod) {
        modifiers.add(mod);    
    }

    public Set<Modifier> getModifiers() {
        return modifiers;
    }

    public void setDeprecation(Deprecation deprecation) {
        this.deprecation = deprecation;
    }

    public Deprecation getDeprecation() {
        return deprecation;
    }

    public void setDeprecationText(Text text) {
        deprecationText = text;
    }

    public Text getDeprecationText() {
        return deprecationText;
    }

    public void setSince(Text text) {
        since = text;
    }

    public Text getSince() {
        return since;
    }

    public void setFirstSentence(Text text) {
        firstSentence.set(text);
    }

    public Text getFirstSentence() {
        return firstSentence;
    }

    public void setBody(Text text) {
        body.set(text);
    }

    public Text getBody() {
        return body;
    }

    public void setFullBody(Text text) {
        fullBody.set(text);
    }

    public Text getFullBody() {
        return fullBody;
    }

    public void setReferences(List<Reference> refs) {
        references = refs;
    }

    public List<Reference> getReferences() {
        return references;
    }

    public String getModifiersString() {
        StringBuilder mods = new StringBuilder();
        List<Modifier> modifierList = ModifierSorter.sortModifiers(modifiers);
        for (Modifier mod : modifierList) {
            if (mod != Modifier.PUBLIC) {
                mods.append(mod.toString()).append(" ");
            }
        }
        return mods.toString();
    }

    public class ModifierSorter {
        private ModifierSorter() {
            // Hiding the public constructor
        }

        private static final List<Modifier> ORDER = Arrays.asList(
            Modifier.PUBLIC, Modifier.PROTECTED, Modifier.PRIVATE,
            Modifier.STATIC, Modifier.FINAL, Modifier.ABSTRACT,
            Modifier.SYNCHRONIZED, Modifier.TRANSIENT, Modifier.VOLATILE,
            Modifier.NATIVE, Modifier.STRICTFP, Modifier.DEFAULT
        );

        public static List<Modifier> sortModifiers(Set<Modifier> modifierSet) {
            List<Modifier> modifierList = new ArrayList<>(modifierSet);
            modifierList.sort(Comparator.comparingInt(ORDER::indexOf));
            return modifierList;
        }
    }
}
