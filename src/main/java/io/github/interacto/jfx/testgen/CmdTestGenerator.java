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

import io.github.interacto.command.CommandImpl;
import io.github.interacto.jfx.test.CommandTest;
import io.github.interacto.jfx.test.UndoableCmdTest;
import io.github.interacto.undo.Undoable;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import spoon.reflect.code.CtBlock;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtTypeReference;
import spoon.template.Substitution;

public class CmdTestGenerator {
	private final CtClass<?> cmd;

	private CtTypeReference<?> cmdTypeRef;
	private Factory factory;
	private CtClass<?> testClass;
	boolean isUndoable;
	private List<CtField<?>> fields;

	public CmdTestGenerator(final CtClass<?> cmd) {
		super();
		this.cmd = cmd;
	}


	public void generate() {
		factory = cmd.getFactory();
		cmdTypeRef = factory.createCtTypeReference(CommandImpl.class);
		System.out.println(cmd.getSimpleName() + "Test in " + cmd.getPackage());
		testClass = factory.createClass(cmd.getPackage(), cmd.getSimpleName() + "Test");
		isUndoable = cmd.isSubtypeOf(factory.createCtTypeReference(Undoable.class));

		addSuperTestClass();

		Substitution.insertAllMethods(testClass, new CmdTestClassTemplate());
		final CtAnnotation<Annotation> overrideAnnot = factory.createAnnotation(factory.createCtTypeReference(Override.class));
		testClass.getMethods().forEach(m -> m.addAnnotation(overrideAnnot));

		addAttributes();

		addTearDown();
	}

	private void addAttributes() {
		fields = cmd.getAllFields()
			.stream()
			.filter(f -> !f.getDeclaringType().equals(cmdTypeRef))
			.map(f -> (CtField<?>) factory.createField(testClass, Set.of(), f.getType(), f.getSimpleName()))
			.collect(Collectors.toList());
	}

	private void addSuperTestClass() {
		final CtTypeReference<?> ref;

		if(isUndoable) {
			ref = factory.createCtTypeReference(UndoableCmdTest.class);
		}else {
			ref = factory.createCtTypeReference(CommandTest.class);
		}

		ref.addActualTypeArgument(cmd.getReference());
		testClass.setSuperclass(ref);
	}

	private void addTearDown() {
		final CtMethod<?> tearDown = factory.createMethod(testClass,
			Set.of(),
			factory.Type().voidPrimitiveType(),
			"tearDown" + testClass.getSimpleName(),
			List.of(), Set.of());
		final CtBlock<?> body = factory.createBlock();

		tearDown.addAnnotation(factory.createAnnotation(factory.createCtTypeReference(AfterEach.class)));
		tearDown.setBody(body);

		body.getStatements().addAll(
			fields.stream()
				.map(f -> factory.createVariableAssignment(f.getReference(), false, factory.createLiteral(null)))
				.collect(Collectors.toList())
		);

	}
}
