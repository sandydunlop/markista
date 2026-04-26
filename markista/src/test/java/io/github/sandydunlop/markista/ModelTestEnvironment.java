package io.github.sandydunlop.markista;

import io.github.sandydunlop.markista.core.Context;
import io.github.qishr.cascara.lang.java.model.AnnotationNode;
import io.github.qishr.cascara.lang.java.model.SemanticModel;
import io.github.qishr.cascara.lang.java.model.ClassNode;
import io.github.qishr.cascara.lang.java.model.EnumNode;
import io.github.qishr.cascara.lang.java.model.FieldNode;
import io.github.qishr.cascara.lang.java.model.InterfaceNode;
import io.github.qishr.cascara.lang.java.model.Link;
import io.github.qishr.cascara.lang.java.model.ModuleNode;
import io.github.qishr.cascara.lang.java.model.NameUtil;
import io.github.qishr.cascara.lang.java.model.JlsName;
import io.github.qishr.cascara.lang.java.model.PackageNode;
import io.github.qishr.cascara.lang.java.model.PackageReference;
import io.github.qishr.cascara.lang.java.model.RecordNode;
import io.github.qishr.cascara.lang.java.model.Reference;
import io.github.qishr.cascara.lang.java.model.TypeNode;
import io.github.qishr.cascara.lang.java.model.VariableTypeNode;
import io.github.qishr.cascara.lang.java.model.ModelUtil;
import io.github.sandydunlop.markista.orchestration.LinkResolver;

import java.io.StringWriter;
import java.net.URI;

import jdk.javadoc.doclet.Reporter;

import com.sun.source.util.DocTreePath;

import javax.lang.model.element.Element;

public class ModelTestEnvironment {
    protected Context ctx;
    public TestReporter reporter;
	protected SemanticModel api;
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
        typeNode.setOwnerName(pkg.getName().fullyQualifiedName());
        pkg.addType(typeNode);
        api.addType(typeNode);
    }

    protected ClassNode newClass(String typeName, PackageNode pkg) {
        // ClassNode classNode = new ClassNode(NameUtil.createName(pkg.getName()+"."+typeName, pkg.getName()));
        ClassNode classNode = new ClassNode(NameUtil.createTypeName(pkg.getName(), typeName));
        configureType(classNode, pkg);
        return classNode;
    }

    protected EnumNode newEnum(String typeName, PackageNode pkg) {
        EnumNode classNode = new EnumNode(NameUtil.createTypeName(pkg.getName(), typeName));
        configureType(classNode, pkg);
        return classNode;
    }

    protected InterfaceNode newInterface(String typeName, PackageNode pkg) {
        InterfaceNode classNode = new InterfaceNode(NameUtil.createTypeName(pkg.getName(), typeName));
        configureType(classNode, pkg);
        return classNode;
    }

    protected AnnotationNode newAnnotation(String typeName, PackageNode pkg) {
        AnnotationNode classNode = new AnnotationNode(NameUtil.createTypeName(pkg.getName(), typeName));
        configureType(classNode, pkg);
        return classNode;
    }

    protected RecordNode newRecord(String typeName, PackageNode pkg) {
        RecordNode classNode = new RecordNode(NameUtil.createTypeName(pkg.getName(), typeName));
        configureType(classNode, pkg);
        return classNode;
    }

    protected FieldNode newField(String typeName, String name, TypeNode type) {
        VariableTypeNode vt = ModelUtil.parseVariableType(typeName);
        JlsName fieldName = NameUtil.createMemberName(name);
        FieldNode fieldNode = new FieldNode(vt, fieldName);
        type.addField(fieldNode);
        return fieldNode;
    }

    protected VariableTypeNode newVariableTypeNode(String typeName) {
        return ModelUtil.parseVariableType(typeName);
    }

    protected Link newMethodReference(String typeName, String methodName) {
        Link methodLink = Link.to(NameUtil.createReference(typeName + "#" + methodName))
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

		api = new SemanticModel("Test API");
        api.addPackage(new PackageNode("io.github.sandydunlop"));
        markista = new PackageNode("io.github.sandydunlop.markista");
		doclet = new PackageNode("io.github.sandydunlop.markista.doclet");
		model = new PackageNode("io.github.sandydunlop.markista.model");
		api.addPackage(markista);
		api.addPackage(doclet);
		api.addPackage(model);

        module = new ModuleNode("markista");
		api.addModule(module);
		module.addPackage(markista); //new PackageReference(markista.getName().fullyQualifiedName()));
		module.addPackage(doclet); //new PackageReference(doclet.getName().fullyQualifiedName()));
		module.addPackage(model); //new PackageReference(model.getName().fullyQualifiedName()));
		markista.setModuleName(module.getName().fullyQualifiedName());
		doclet.setModuleName(module.getName().fullyQualifiedName());
		model.setModuleName(module.getName().fullyQualifiedName());

        node = newClass("Node", model);
        node.setOwnerName(model.getName().fullyQualifiedName());
        model.addType(node);
        api.addType(node);

        ctx.setApi(api);
    }
}
