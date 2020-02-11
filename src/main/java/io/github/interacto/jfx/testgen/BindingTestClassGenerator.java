/*
 * Interacto
 * Copyright (C) 2020 Arnaud Blouin
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.github.interacto.jfx.testgen;

import io.github.interacto.interaction.InteractionData;
import io.github.interacto.jfx.test.BindingsContext;
import io.github.interacto.jfx.test.CmdAssert;
import io.github.interacto.jfx.test.WidgetBindingExtension;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.service.query.NodeQuery;
import org.testfx.util.WaitForAsyncUtils;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.reference.CtVariableReference;
import spoon.template.Substitution;

public class BindingTestClassGenerator {
	private final CtClass<?> bindingsClass;
	private final Set<CtInvocation<?>> binders;
	private final Factory factory;
	private final String fxmlPath;

	private CtClass<?> genBaseCl;
	private CtClass<?> genImplCl;
	List<BindingTestsGenerator> bindings;
	private CtField<?> controller;

	public BindingTestClassGenerator(final CtClass<?> bindingsClass, final Set<CtInvocation<?>> binders, final String fxmlPath) {
		super();
		this.bindingsClass = bindingsClass;
		this.binders = binders;
		factory = bindingsClass.getFactory();
		this.fxmlPath = fxmlPath;
	}

	public void generate() {
		genBaseCl = factory.createClass(bindingsClass.getPackage(), bindingsClass.getSimpleName() + "BaseTest");
		genImplCl = factory.createClass(bindingsClass.getPackage(), bindingsClass.getSimpleName() + "Test");

		bindings = binders
				.stream()
				.map(binder -> new BindingTestsGenerator(bindingsClass, binder, genBaseCl, genImplCl))
				.collect(Collectors.toList());
		bindings.forEach(gen -> gen.generate());

		configBaseTestClass();
		configImplTestClass();
	}


	/* Impl test class methods */

	private void configImplTestClass() {
		// public class
		genImplCl.setModifiers(Set.of(ModifierKind.PUBLIC));
		// super class
		genImplCl.setSuperclass(genBaseCl.getReference());

		// Adding the TestFX annotation
		final var annot = factory.createAnnotation(factory.createCtTypeReference(ExtendWith.class));
		annot.addValue("value", ApplicationExtension.class);
		genImplCl.addAnnotation(annot);

		genImplDataMethods();
		genImplActivateCheckMethod();
		completeImplDataMethods();
		genImplStartMethod();
	}

	private void genImplDataMethods() {
		genBaseCl.getAllExecutables()
			.stream()
			.map(e -> e.getDeclaration())
			.filter(CtMethod.class::isInstance)
			.map(CtMethod.class::cast)
			.filter(CtMethod::isAbstract)
			.forEach(m -> {
				final CtMethod<?> m2 = m.clone();
				m2.setModifiers(Set.of());
				m2.setBody(factory.createBlock());
				genImplCl.addMethod(m2);
				annotate(m2, Override.class);
			});
	}

	private void genImplActivateCheckMethod() {
		genBaseCl.getAllExecutables()
			.stream()
			.map(e -> e.getDeclaration())
			.filter(CtMethod.class::isInstance)
			.map(CtMethod.class::cast)
			.filter(m -> m.getSimpleName().startsWith("activate") || m.getSimpleName().startsWith("check"))
			.forEach(m -> {
				final CtMethod<?> m2 = m.clone();
				m2.setModifiers(Set.of());
				m2.setBody(factory.createBlock());
				genImplCl.addMethod(m2);
				annotate(m2, Override.class);
			});
	}


	private void completeImplDataMethods() {
		final var returnType = factory.createCtTypeReference(InteractionData.class);
		final var mockito = factory.createCtTypeReference(Mockito.class);

		genImplCl.getAllExecutables()
			.stream()
			.map(e -> e.getDeclaration())
			.filter(CtMethod.class::isInstance)
			.map(CtMethod.class::cast)
			.filter(m -> m.getType().isSubtypeOf(returnType))
			.forEach(m -> {
				final var data = factory.createLocalVariable(m.getType(), "data",
					factory.createInvocation(
						factory.createTypeAccess(mockito),
						mockito.getAllExecutables().stream()
							.filter(e -> "mock".equals(e.getSimpleName()) && e.getParameters().size() == 1).findFirst().orElseThrow(),
						factory.createClassAccess(m.getType())));
				data.setModifiers(Set.of(ModifierKind.FINAL));

				m.getBody().addStatement(data);

				final var ret = factory.createReturn();
				ret.setReturnedExpression(factory.createVariableRead(data.getReference(), false));
				m.getBody().addStatement(ret);
			});
	}


	private void genImplStartMethod() {
		final var template = new TestStartTemplate();
		template.Object = controller.getType().getSimpleName();
		template.path = "\"" + fxmlPath + "\"";
		template.controller = controller.getSimpleName();
		template.cons = factory.createConstructorCall(controller.getType());
		Substitution.insertAllMethods(genImplCl, template);
	}


	/* Base test class methods */

	private void configBaseTestClass() {
		final var annotExtends = annotate(genBaseCl, ExtendWith.class);
		annotExtends.addValue("value", WidgetBindingExtension.class);
		genBaseCl.setModifiers(Set.of(ModifierKind.PUBLIC, ModifierKind.ABSTRACT));

		controller = factory.createField(genBaseCl, Set.of(), bindingsClass.getReference(), bindingsClass.getSimpleName().toLowerCase());

		final List<CtField<?>> baseWidgetFields = bindings.stream()
			.map(b -> b.widgetFields)
			.flatMap(w -> w.stream())
			.distinct()
			.map(w -> (CtField<?>) factory.createField(genBaseCl, Set.of(), w.getType(), w.getSimpleName()))
			.collect(Collectors.toList());

		genBaseSetUp(baseWidgetFields);

		createBaseNbBidingsTest();

		// Generating base tests and methods for each binding
		bindings.forEach(genBinding -> generateBaseTestAndMethodForBinder(genBinding));
	}

	private void generateBaseTestAndMethodForBinder(final BindingTestsGenerator genBinding) {
		// Creating a description of the binding
		final String bindingName = genBinding.interactionType.getSimpleName() + "To" + genBinding.cmdType.getSimpleName();

		// Generating the method to be used to activate the binding
		final var activateMethod = factory.createMethod(genBaseCl, Set.of(),
			factory.Type().voidPrimitiveType(), "activate" + bindingName, List.of(), Set.of());
		activateMethod.setBody(factory.createBlock());
		createFxRobotParam(activateMethod);

		// Generating the method to check the executing of the binding
		final var checkMethod = factory.createMethod(genBaseCl, Set.of(),
			factory.Type().voidPrimitiveType(), "check" + bindingName, List.of(), Set.of());
		checkMethod.setBody(factory.createBlock());
		createParam(checkMethod, "cmd", genBinding.cmdType.getTypeDeclaration().getReference());
		createParam(checkMethod, "data", genBinding.interactionDataType);

		// Generating the method that provides interaction data to the test
		final var dataMethod = factory.createMethod(genBaseCl, Set.of(ModifierKind.ABSTRACT), genBinding.interactionDataType,
			"data" + bindingName, List.of(), Set.of());
		createFxRobotParam(dataMethod);

		// Generating the test methods
		genBinding.widgetFields.forEach(w -> {
			final var testMethod = factory.createMethod(genBaseCl, Set.of(),
				factory.Type().voidPrimitiveType(), "test" + bindingName + "Using" + w.getSimpleName(), List.of(), Set.of());
			testMethod.setBody(factory.createBlock());
			createFxRobotParam(testMethod);
			createBindingCtxParam(testMethod);
			annotate(testMethod, Test.class);
			completeBaseTestMethod(testMethod, checkMethod, dataMethod, activateMethod, genBinding, w);
		});
	}


	private void completeBaseTestMethod(final CtMethod<?> test, final CtMethod<?> check, final CtMethod<?> data, final CtMethod<?> activate,
				final BindingTestsGenerator genBinding, final CtFieldReference<?> w) {
		// Adding the invocation to the activation method
		test.getBody().addStatement(
			factory.createInvocation(null, activate.getReference(),
				factory.createVariableRead(test.getParameters().get(0).getReference(), false))
		);

		// Adding the invocation and assignment to the data method
		final var dataVar = factory.createLocalVariable((CtTypeReference) data.getType(), "data",
			factory.createInvocation(null, data.getReference(),
				factory.createVariableRead(test.getParameters().get(0).getReference(), false))
		);
		dataVar.setModifiers(Set.of(ModifierKind.FINAL));
		test.getBody().addStatement(dataVar);

		// Adding the interaction execution
		InteractionSequencesGen.genSequences(genBinding.interactionType, test, test.getParameters().get(0).getReference(), w);
		final var wait = factory.createCtTypeReference(WaitForAsyncUtils.class);
		test.getBody().addStatement(factory.createInvocation(factory.createTypeAccess(wait),
			wait.getAllExecutables()
				.stream()
				.filter(e -> "waitForFxEvents".equals(e.getSimpleName()) && e.getParameters().isEmpty())
				.findFirst().orElseThrow())
		);

		// Adding the command execution assertion
		final var bindingCtx = test.getParameters().get(1);
		final var cmdVar = factory.createLocalVariable((CtTypeReference) genBinding.cmdType, "cmd",
			factory.createInvocation(
				factory.createInvocation(
					factory.createVariableRead(bindingCtx.getReference(), false),
					bindingCtx.getType().getAllExecutables().stream()
						.filter(e -> e.getParameters().size() == 1 && "oneCmdProducedAmong".equals(e.getSimpleName()))
						.findFirst().orElseThrow(),
					factory.createClassAccess(genBinding.cmdType)),
				factory.createCtTypeReference(CmdAssert.class).getAllExecutables().stream()
					.filter(e -> e.getParameters().isEmpty() && "getCommand".equals(e.getSimpleName()))
					.findFirst().orElseThrow()
		));
		cmdVar.setModifiers(Set.of(ModifierKind.FINAL));
		test.getBody().addStatement(cmdVar);

		// Adding the invocation to the checking method
		test.getBody().addStatement(
			factory.createInvocation(null, check.getReference(),
				factory.createVariableRead(cmdVar.getReference(), false),
				factory.createVariableRead(dataVar.getReference(), false))
		);
	}


	private void genBaseSetUp(final List<CtField<?>> baseWidgetFields) {
		final var setUp = factory.createMethod(genBaseCl, Set.of(),
			factory.Type().voidPrimitiveType(),"setUp" + genBaseCl.getSimpleName(), List.of(), Set.of());
		final CtBlock<?> body = factory.createBlock();

		annotate(setUp, BeforeEach.class);
		setUp.setBody(body);
		final var robotParam = createFxRobotParam(setUp);

		baseWidgetFields.forEach(w -> {
			final var query = factory.createCtTypeReference(NodeQuery.class).getAllExecutables()
				.stream()
				.filter(exec -> exec.getSimpleName().equals("query"))
				.findFirst().orElseThrow();
			final var lookup = factory.createCtTypeReference(FxRobot.class).getAllExecutables()
				.stream()
				.filter(exec -> "lookup".equals(exec.getSimpleName()) && exec.getParameters().size() == 1 &&
					exec.getParameters().get(0).getTypeDeclaration().getSimpleName().equals("String"))
				.findFirst().orElseThrow();

			final var robotvar = factory.createVariableRead(robotParam.getReference(), false);
			body.addStatement(
				factory.createVariableAssignment((CtVariableReference) w.getReference(), false,
					factory.createInvocation(
						factory.createInvocation(robotvar, lookup, factory.createLiteral("#" + w.getSimpleName())), query)
					));
		});
	}


	private void createBaseNbBidingsTest() {
		final var test = factory.createMethod(genBaseCl, Set.of(), factory.Type().voidPrimitiveType(), "testNumberOfBindings", List.of(), Set.of());
		final var param = createBindingCtxParam(test);
		annotate(test, Test.class);
		final var body = factory.createBlock();
		test.setBody(body);

		body.addStatement(
			factory.createInvocation(factory.createVariableRead(param.getReference(), false),
			getMethod(BindingsContext.class, "hasBindings"),
			factory.createLiteral(binders.size())));
	}



	/* Utility methods */


	private CtParameter<?> createFxRobotParam(final CtMethod<?> method) {
		return createParam(method, "robot", factory.createCtTypeReference(FxRobot.class));
	}

	private CtParameter<?> createBindingCtxParam(final CtMethod<?> method) {
		return createParam(method, "ctx", factory.createCtTypeReference(BindingsContext.class));
	}

	private CtParameter<?> createParam(final CtMethod<?> method, final String argName, final CtTypeReference<?> argClass) {
		final var param = factory.createParameter(method, argClass, argName);
		param.setModifiers(Set.of(ModifierKind.FINAL));
		return param;
	}

	private CtAnnotation<Annotation> annotate(final CtElement elt, final Class<?> annotation) {
		final CtAnnotation<Annotation> annot = factory.createAnnotation(factory.createCtTypeReference(annotation));
		elt.addAnnotation(annot);
		return annot;
	}

	private CtExecutableReference<?> getMethod(final Class<?> cl, final String name) {
		return factory.createCtTypeReference(cl)
			.getAllExecutables()
			.stream()
			.filter(exec -> name.equals(exec.getSimpleName()))
			.findFirst()
			.orElseThrow();
	}
}
