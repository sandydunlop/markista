package io.github.sandydunlop.markista;

import io.github.sandydunlop.markista.core.Context;
import io.github.sandydunlop.cascara.model.AnnotationNode;
import io.github.sandydunlop.cascara.model.Api;
import io.github.sandydunlop.cascara.model.ClassNode;
import io.github.sandydunlop.cascara.model.EnumNode;
import io.github.sandydunlop.cascara.model.FieldNode;
import io.github.sandydunlop.cascara.model.InterfaceNode;
import io.github.sandydunlop.cascara.model.Link;
import io.github.sandydunlop.cascara.model.ModuleNode;
import io.github.sandydunlop.cascara.model.Name;
import io.github.sandydunlop.cascara.model.PackageNode;
import io.github.sandydunlop.cascara.model.PackageReference;
import io.github.sandydunlop.cascara.model.RecordNode;
import io.github.sandydunlop.cascara.model.Reference;
import io.github.sandydunlop.cascara.model.TypeNode;
import io.github.sandydunlop.cascara.model.VariableType;
import io.github.sandydunlop.markista.orchestration.LinkResolver;

import java.io.StringWriter;
import java.net.URI;

import jdk.javadoc.doclet.Reporter;

import com.sun.source.util.DocTreePath;

import javax.lang.model.element.Element;

public class ModelTestEnvironment {
    protected Context ctx;
    public TestReporter reporter;
	protected Api api;
    protected LinkResolver resolver;

    protected ModuleNode module;
    protected PackageNode model;
    protected PackageNode doclet;
	protected PackageNode markista;
    protected ClassNode node;

    protected class TestReporter implements Reporter {
        public StringWriter output = new StringWriter();

        @Override
        public void print(javax.tools.Diagnostic.Kind kind, String message) {
            output.write(message);
        }

        @Override
        public void print(javax.tools.Diagnostic.Kind kind, DocTreePath path, String message) {
            output.write(message);
        }

        @Override
        public void print(javax.tools.Diagnostic.Kind kind, Element element, String message) {
            output.write(message);
        }
    }

    protected void configureType(TypeNode typeNode, PackageNode pkg) {
        typeNode.setOwnerName(pkg.getName());
        pkg.addType(typeNode);
        api.addType(typeNode);
    }

    protected ClassNode newClass(String typeName, PackageNode pkg) {
        ClassNode classNode = new ClassNode(new Name(pkg.getName()+"."+typeName, pkg.getName()));
        configureType(classNode, pkg);
        return classNode;
    }

    protected EnumNode newEnum(String typeName, PackageNode pkg) {
        EnumNode classNode = new EnumNode(new Name(pkg.getName()+"."+typeName, pkg.getName()));
        configureType(classNode, pkg);
        return classNode;
    }

    protected InterfaceNode newInterface(String typeName, PackageNode pkg) {
        InterfaceNode classNode = new InterfaceNode(new Name(pkg.getName()+"."+typeName, pkg.getName()));
        configureType(classNode, pkg);
        return classNode;
    }

    protected AnnotationNode newAnnotation(String typeName, PackageNode pkg) {
        AnnotationNode classNode = new AnnotationNode(new Name(pkg.getName()+"."+typeName, pkg.getName()));
        configureType(classNode, pkg);
        return classNode;
    }

    protected RecordNode newRecord(String typeName, PackageNode pkg) {
        RecordNode classNode = new RecordNode(new Name(pkg.getName()+"."+typeName, pkg.getName()));
        configureType(classNode, pkg);
        return classNode;
    }

    protected FieldNode newField(String typeName, String name, TypeNode type) {
        FieldNode fieldNode = new FieldNode(typeName, name);
        type.addField(fieldNode);
        return fieldNode;
    }

    protected VariableType newVariableType(String typeName) {
        return VariableType.parse(typeName);
    }

    protected Link newMethodReference(String typeName, String methodName) {
        Link methodLink = Link.to(new Reference(typeName + "#" + methodName))
                .withKind(Link.Kind.METHOD)
                .withMethodName(methodName);
        methodLink.setAnchor(methodName.toLowerCase());
        methodLink.setUri(URI.create(typeName));
        return methodLink;
    }

    protected void setupModel() {
        Context.reset();
		ctx = Context.getInstance();
        reporter = new TestReporter();
		ctx.setReporter(reporter);

		api = new Api("Test API");
        api.addPackage(new PackageNode("io.github.sandydunlop"));
        markista = new PackageNode("io.github.sandydunlop.markista");
		doclet = new PackageNode("io.github.sandydunlop.markista.doclet");
		model = new PackageNode("io.github.sandydunlop.markista.model");
		api.addPackage(markista);
		api.addPackage(doclet);
		api.addPackage(model);

        module = new ModuleNode("markista");
		api.addModule(module);
		module.addPackage(new PackageReference(markista.getName()));
		module.addPackage(new PackageReference(doclet.getName()));
		module.addPackage(new PackageReference(model.getName()));
		markista.setModuleName(module.getName());
		doclet.setModuleName(module.getName());
		model.setModuleName(module.getName());

        node = newClass("Node", model);
        node.setOwnerName(model.getName());
        model.addType(node);
        api.addType(node);

        ctx.setApi(api);
    }
}
