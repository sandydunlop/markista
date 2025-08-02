package io.github.sandydunlop.markista.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import com.sun.source.util.DocTreePath;

import io.github.sandydunlop.markista.model.DirectiveNode;

import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;

import javax.lang.model.element.Element;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic.Kind;
import javax.lang.model.element.ModuleElement.DirectiveKind;
import javax.lang.model.element.ModuleElement.ExportsDirective;
import javax.lang.model.element.ModuleElement.OpensDirective;
import javax.lang.model.element.ModuleElement.ProvidesDirective;
import javax.lang.model.element.ModuleElement.RequiresDirective;
import javax.lang.model.element.ModuleElement.UsesDirective;
import javax.lang.model.element.Name;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class ModuleDirectivesTests {
    private Elements elementUtils;
    private ModuleElement aModuleElement;
    private DocletEnvironment docletEnv;
    private ModuleElement moduleElement;
    private Name name;
    private PackageElement packageElement;
    private TypeElement typeElement;

    @Mock static Reporter reporter = new Reporter() {
        @Override
        public void print(Kind kind, String message) {
            System.out.println(kind + ": " + message);
        }

        @Override
        public void print(Kind kind, DocTreePath path, String message) {
            // Do nothing
        }

        @Override
        public void print(Kind kind, Element element, String message) {
            // Do nothing
        }
    };
    
    @BeforeAll
    static void initAll() {
		Context.setReporter(reporter);
    }

    @BeforeEach
    void setUp() {
        docletEnv = mock(DocletEnvironment.class);
        elementUtils = mock(Elements.class);
        aModuleElement = mock(ModuleElement.class);
        moduleElement = mock(ModuleElement.class);
        name = mock(Name.class);
        packageElement = mock(PackageElement.class);
        typeElement = mock(TypeElement.class);
        when(elementUtils.getModuleOf(aModuleElement)).thenReturn(moduleElement);
        when(docletEnv.getElementUtils()).thenReturn(elementUtils);
        when(moduleElement.getQualifiedName()).thenReturn(name);
        when(packageElement.getQualifiedName()).thenReturn(name);
        when(aModuleElement.getQualifiedName()).thenReturn(name);
        when(typeElement.getQualifiedName()).thenReturn(name);
        when(name.toString()).thenReturn("sandy");
        ModuleDirectives.setEnvironment(docletEnv);
    }

    @SuppressWarnings("unused")
    @Test 
    void createFrom_REQUIRES() {
        RequiresDirective directive = mock(RequiresDirective.class);
        when(directive.getDependency()).thenReturn(aModuleElement);
        when(directive.getKind()).thenAnswer(unused -> DirectiveKind.REQUIRES); //NOSONAR
        DirectiveNode directiveNode = ModuleDirectives.createFrom(directive);
        assertNotNull(directiveNode);
        DirectiveNode.Kind kind = directiveNode.getKind();
        assertEquals(DirectiveNode.Kind.REQUIRES, kind);
    }

    @SuppressWarnings("unused")
    @Test 
    void createFrom_EXPORTS() {
        ExportsDirective directive = mock(ExportsDirective.class);
        List<? extends ModuleElement> targetModules = List.of(aModuleElement);
        when(directive.getPackage()).thenReturn(packageElement);
        when(directive.getTargetModules()).thenAnswer(unused -> targetModules);
        when(directive.getKind()).thenAnswer(unused -> DirectiveKind.EXPORTS); //NOSONAR
        DirectiveNode directiveNode = ModuleDirectives.createFrom(directive);
        assertNotNull(directiveNode);
        DirectiveNode.Kind kind = directiveNode.getKind();
        assertEquals(DirectiveNode.Kind.EXPORTS, kind);
    }

    @SuppressWarnings("unused")
    @Test 
    void createFrom_OPENS() {
        OpensDirective directive = mock(OpensDirective.class);
        List<? extends ModuleElement> targetModules = List.of(aModuleElement);
        when(directive.getPackage()).thenReturn(packageElement);
        when(directive.getTargetModules()).thenAnswer(unused -> targetModules); //NOSONAR
        when(directive.getKind()).thenAnswer(unused -> DirectiveKind.OPENS); //NOSONAR
        DirectiveNode directiveNode = ModuleDirectives.createFrom(directive);
        assertNotNull(directiveNode);
        DirectiveNode.Kind kind = directiveNode.getKind();
        assertEquals(DirectiveNode.Kind.OPENS, kind);
    }

    @SuppressWarnings("unused")
    @Test 
    void createFrom_USES() {
        UsesDirective directive = mock(UsesDirective.class);
        when(directive.getService()).thenReturn(typeElement);
        when(directive.getKind()).thenAnswer(unused -> DirectiveKind.USES); //NOSONAR
        DirectiveNode directiveNode = ModuleDirectives.createFrom(directive);
        assertNotNull(directiveNode);
        DirectiveNode.Kind kind = directiveNode.getKind();
        assertEquals(DirectiveNode.Kind.USES, kind);
    }

    @SuppressWarnings("unused")
    @Test 
    void createFrom_PROVIDES() {
        ProvidesDirective directive = mock(ProvidesDirective.class);
        List<? extends TypeElement> implementations = List.of(typeElement);
        when(directive.getService()).thenReturn(typeElement);
        when(directive.getImplementations()).thenAnswer(unused -> implementations); //NOSONAR
        when(directive.getKind()).thenAnswer(unused -> DirectiveKind.PROVIDES); //NOSONAR
        DirectiveNode directiveNode = ModuleDirectives.createFrom(directive);
        assertNotNull(directiveNode);
        DirectiveNode.Kind kind = directiveNode.getKind();
        assertEquals(DirectiveNode.Kind.PROVIDES, kind);
    }
}
