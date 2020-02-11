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
import io.github.interacto.jfx.binding.api.BaseBinderBuilder;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.reference.CtTypeReference;

public class BindingTestsGenerator {
	private final CtClass<?> bindingsClass;
	private final CtInvocation<?> binder;
	private final CtClass<?> genBaseCl;
	private final CtClass<?> genImplCl;
	private final Factory factory;

	private List<CtInvocation<?>> binderRoutines;

	/* widgets */
	final Set<CtFieldReference<?>> widgetFields = new HashSet<>();
	final Set<CtExpression<?>> collectionWidgets = new HashSet<>();
	/* User interaction */
	CtTypeReference<?> interactionType;
	CtTypeReference<?> interactionDataType;
	/* command */
	CtTypeReference<?> cmdType;


	public BindingTestsGenerator(final CtClass<?> bindingsClass, final CtInvocation<?> binder,
								final CtClass<?> genBaseCl, final CtClass<?> genImplCl) {
		super();
		this.bindingsClass = bindingsClass;
		this.binder = binder;
		this.genBaseCl = genBaseCl;
		this.genImplCl = genImplCl;
		factory = bindingsClass.getFactory();
	}

	public void generate() {
		final var binderType = factory.createCtTypeReference(BaseBinderBuilder.class);

		binderRoutines = binder
			.filterChildren(elt -> elt instanceof CtInvocation)
			.list(CtInvocation.class)
			.stream()
			.filter(invok -> invok.getExecutable().getDeclaringType().isSubtypeOf(binderType) ||
				invok.getExecutable().getType().isSubtypeOf(binderType))
			.map(invok -> (CtInvocation<?>) invok)
			.collect(Collectors.toList());

		extractInteraction();
		extractCommand();

		binderRoutines
			.stream()
			.filter(invok -> "on".equals(invok.getExecutable().getSimpleName()))
			.forEach(invok -> extractWidgets(invok, invok.getExecutable().getParameters()));
	}

	private void extractCommand() {
		cmdType = binderRoutines
			.stream()
			.filter(r -> "toProduce".equals(r.getExecutable().getSimpleName()))
			.findFirst()
			.map(r ->
				r.getArguments().get(0).getType().getActualTypeArguments().get(
					r.getArguments().get(0).getType().getActualTypeArguments().size() - 1))
			.orElseGet(() -> (CtTypeReference) binderRoutines.get(binderRoutines.size() - 1)
				.getExecutable().getType().getActualTypeArguments().get(1));
	}

	private void extractInteraction() {
		// Looking for the usingInteraction routine to get the interaction
		interactionType = binderRoutines
			.stream()
			.filter(r -> "usingInteraction".equals(r.getExecutable().getSimpleName()))
			.findFirst()
			.map(r ->
				r.getArguments().get(0).getType().getActualTypeArguments().get(0))
			// If no usingInteraction, looking for the root routine that should implies an interaction
			// eg buttonBinder()
			.orElseGet(() -> (CtTypeReference) binderRoutines.get(binderRoutines.size() - 1)
				.getExecutable().getType().getActualTypeArguments().get(1));

		// Extracting the interaction data type
		final var dataType = factory.createCtTypeReference(InteractionData.class);
		interactionDataType = interactionType.getTypeDeclaration().getReferencedTypes()
			.stream()
			.filter(i -> i.isInterface() && i.isSubtypeOf(dataType))
			.findFirst()
			.orElse(interactionType.getTypeDeclaration().getReference());
	}

	private void extractWidgets(final CtInvocation<?> invok, final List<CtTypeReference<?>> parameters) {
		if(parameters.size() == 1) {
			final CtTypeReference<?> param = parameters.get(0);

			if(param.getTypeDeclaration().isSubtypeOf(factory.createCtTypeReference(List.class))) {
				final CtExpression<?> arg = invok.getArguments().get(0);
				final Set<CtFieldAccess<?>> fs = arg.filterChildren(CtFieldRead.class::isInstance)
					.list()
					.stream()
					.map(f -> (CtFieldRead<?>) f)
					.collect(Collectors.toSet());

				collectionWidgets.add(arg);

				if(fs.isEmpty()) {
					System.err.println("on collections: cannot find a root fieldread: " + arg);
					return;
				}

				if(fs.size() > 1) {
					System.err.println("on collections: more than on fieldRead: " + fs);
					return;
				}

				widgetFields.add(fs.iterator().next().getVariable());

				return;
			}

			if(param.getTypeDeclaration().isArray()) {
				widgetFields.addAll(invok.getArguments().stream()
					.filter(CtFieldAccess.class::isInstance)
					.map(arg -> (CtFieldAccess<?>) arg)
					.map(f -> f.getVariable())
					.collect(Collectors.toSet()));
			}
		}
	}
}
