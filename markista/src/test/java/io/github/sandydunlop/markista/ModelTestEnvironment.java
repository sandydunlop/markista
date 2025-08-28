package io.github.sandydunlop.markista;

import io.github.sandydunlop.markista.core.Context;
import io.github.sandydunlop.markista.model.Api;
import io.github.sandydunlop.markista.model.ClassNode;
import io.github.sandydunlop.markista.model.EnumNode;
import io.github.sandydunlop.markista.model.FieldNode;
import io.github.sandydunlop.markista.model.Link;
import io.github.sandydunlop.markista.model.ModuleNode;
import io.github.sandydunlop.markista.model.PackageNode;
import io.github.sandydunlop.markista.model.Text;
import io.github.sandydunlop.markista.model.TypeNode;
import io.github.sandydunlop.markista.model.TypeReference;

import java.io.StringWriter;
import jdk.javadoc.doclet.Reporter;

import com.sun.source.util.DocTreePath;

import javax.lang.model.element.Element;

public class ModelTestEnvironment {
    protected Context ctx;
    protected TestReporter reporter;
	protected Api api;
    protected ModuleNode module;
    protected PackageNode model;
    protected PackageNode doclet;
	protected PackageNode markista;
    protected ClassNode node;

    class TestReporter implements Reporter {
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
        ClassNode classNode = new ClassNode(typeName, pkg.getName());
        configureType(classNode, pkg);
        return classNode;
    }

    protected EnumNode newEnum(String typeName, PackageNode pkg) {
        EnumNode classNode = new EnumNode(typeName, pkg.getName());
        configureType(classNode, pkg);
        return classNode;
    }

    protected FieldNode newField(String typeName, String name, TypeNode type) {
        FieldNode fieldNode = new FieldNode(typeName, name);
        type.addField(fieldNode);
        return fieldNode;
    }

    protected TypeReference newTypeReference(String typeName) {
        TypeReference typeRef = TypeReference.to(typeName);
        typeRef.setText(Text.of(node.getQualifiedName()));
        return typeRef;
    }

    protected Link newMethodReference(String typeName, String methodName) {
        Link methodLink = Link.to(typeName)
                .withUri(typeName)
                .withKind(Link.Kind.METHOD)
                .withLabel(methodName)
                .withMethodName(methodName);
        methodLink.setHasAnchor(true);
        methodLink.setAnchor("#" + methodName);
        methodLink.setQualifiedClassName(typeName);
        return methodLink;
    }

    protected void setupModel() {
        Context.reset();
		ctx = Context.getInstance();
        reporter = new TestReporter();
		ctx.setReporter(reporter);

		api = new Api("Test API");
        ctx.setApi(api);
        api.addPackage(new PackageNode("io.github.sandydunlop"));
        markista = new PackageNode("io.github.sandydunlop.markista");
		doclet = new PackageNode("io.github.sandydunlop.markista.doclet");
		model = new PackageNode("io.github.sandydunlop.markista.model");
		api.addPackage(markista);
		api.addPackage(doclet);
		api.addPackage(model);

        module = new ModuleNode("markista");
		api.addModule(module);
		module.addPackage(markista);
		module.addPackage(doclet);
		module.addPackage(model);
		markista.setModuleName(module.getName());
		doclet.setModuleName(module.getName());
		model.setModuleName(module.getName());

        node = new ClassNode("Node", model.getName());
        node.setOwnerName(model.getName());
        model.addType(node);
        api.addType(node);
    }
}
