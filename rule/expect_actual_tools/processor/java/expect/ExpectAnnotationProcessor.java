package top.fifthlight.mergetools.processor.expect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.palantir.javapoet.*;
import top.fifthlight.mergetools.api.ExpectFactory;
import top.fifthlight.mergetools.processor.ExpectData;
import top.fifthlight.mergetools.processor.Util;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.util.ArrayList;
import java.util.ServiceLoader;
import java.util.Set;

@SupportedSourceVersion(SourceVersion.RELEASE_21)
@SupportedAnnotationTypes("top.fifthlight.mergetools.api.ExpectFactory")
public class ExpectAnnotationProcessor extends AbstractProcessor {
    private Elements elementUtils;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.elementUtils = processingEnv.getElementUtils();
    }

    private boolean generateFactoryClass(String packageName, String interfaceName, String className, ExpectData expectData) {
        var expectInterfaceName = ClassName.get(packageName, interfaceName);
        var factoryInterface = expectInterfaceName.nestedClass("Factory");

        var factoryImplField = FieldSpec.builder(factoryInterface, "factoryImpl", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("$T.load($T.class).findFirst().orElseThrow(() -> new $T($S))",
                        ServiceLoader.class,
                        factoryInterface,
                        RuntimeException.class,
                        "No factory of " + expectInterfaceName.canonicalName() + " found")
                .build();

        var typeSpecBuilder = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC)
                .addField(factoryImplField);

        for (var constructorData : expectData.constructors()) {
            var methodSpecBuilder = MethodSpec.methodBuilder(constructorData.name())
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .returns(expectInterfaceName);

            var parameters = new ArrayList<String>();
            for (var parameterData : constructorData.parameters()) {
                var paramType = Util.getJavaTypeName(parameterData.type());
                var paramName = parameterData.name();
                methodSpecBuilder.addParameter(paramType, paramName);
                parameters.add(paramName);
            }

            var statement = parameters.stream().map(p -> CodeBlock.of("$L", p)).collect(CodeBlock.joining(", "));
            methodSpecBuilder.addStatement("return factoryImpl.$L($L)", constructorData.name(), statement);

            typeSpecBuilder.addMethod(methodSpecBuilder.build());
        }

        var javaFile = JavaFile.builder(packageName, typeSpecBuilder.build()).build();
        try {
            javaFile.writeTo(processingEnv.getFiler());
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Failed to generate class: " + e.getMessage());
            return false;
        }
        return true;
    }

    private boolean generateExpectManifest(String interfaceFullQualifiedName, ExpectData expectData) {
        try {
            var mapper = new ObjectMapper();
            var filePath = "META-INF/expects/" + interfaceFullQualifiedName + ".json";
            var resource = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", filePath);

            try (var writer = resource.openWriter()) {
                mapper.writeValue(writer, expectData);
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Failed to generate JSON file: " + e.getMessage());
            return false;
        }
        return true;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (var element : roundEnv.getElementsAnnotatedWith(ExpectFactory.class)) {
            if (!(element instanceof TypeElement type)) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "ExpectFactory can only be applied to type");
                continue;
            }

            if (type.getKind() != ElementKind.INTERFACE) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "ExpectFactory can only be applied to interface");
                continue;
            }

            if (type.getNestingKind() != NestingKind.MEMBER) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "ExpectFactory can only be applied to member class");
                continue;
            }

            var interfaceSimple = type.getSimpleName().toString();
            if (!"Factory".equals(interfaceSimple)) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "ExpectFactory can only be applied to interface named Factory");
                continue;
            }

            var expectElement = type.getEnclosingElement();
            if (!(expectElement instanceof TypeElement expectType)) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "ExpectFactory can only be applied to member class");
                continue;
            }

            var expectIsPublic = expectType.getModifiers().contains(Modifier.PUBLIC);
            if (!expectIsPublic) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "ExpectFactory's parent class " + expectType + " must be public");
                continue;
            }

            if (expectType.getKind() != ElementKind.INTERFACE) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "ExpectFactory's parent class " + expectType + " can only be interface");
                continue;
            }

            var parentElement = expectType.getEnclosingElement();
            if (parentElement == null || parentElement.getKind() != ElementKind.PACKAGE) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "ExpectFactory's parent class " + expectType + " can only be to top level class, but get " + expectType.getEnclosingElement());
                continue;
            }

            var constructors = new ArrayList<ExpectData.Constructor>();
            for (var enclosedElement : type.getEnclosedElements()) {
                if (enclosedElement.getKind() != ElementKind.METHOD) {
                    continue;
                }
                var method = (ExecutableElement) enclosedElement;
                var name = method.getSimpleName().toString();
                var parameters = method.getParameters()
                        .stream()
                        .map(parameter -> new ExpectData.Constructor.Parameter(
                                Util.getInternalTypeName(elementUtils, parameter.asType()),
                                parameter.getSimpleName().toString()
                        ))
                        .toArray(ExpectData.Constructor.Parameter[]::new);

                constructors.add(new ExpectData.Constructor(name, parameters));
            }

            var expectSimpleName = expectType.getSimpleName().toString();
            var expectBinaryName = Util.getInternalTypeName(elementUtils, expectType.asType());
            var expectData = new ExpectData(expectBinaryName, constructors.toArray(ExpectData.Constructor[]::new));

            var expectPackage = elementUtils.getPackageOf(expectElement);
            if (expectPackage.isUnnamed()) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "ExpectFactory can only be applied to member class in named package");
                continue;
            }
            var expectPackageName = expectPackage.getQualifiedName().toString();
            var expectFullQualifiedName = expectType.getQualifiedName().toString();

            if (!generateFactoryClass(expectPackageName, expectSimpleName, expectSimpleName + "Factory", expectData)) {
                continue;
            }
            if (!generateExpectManifest(expectFullQualifiedName, expectData)) {
                continue;
            }
        }
        return true;
    }
}
