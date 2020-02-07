package io.github.interacto.jfx.testgen;

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
		binderRoutines = binder
			.filterChildren(elt -> elt instanceof CtInvocation)
			.list(CtInvocation.class)
			.stream()
			.filter(invok -> invok.getExecutable().getDeclaringType().isSubtypeOf(factory.createCtTypeReference(BaseBinderBuilder.class)))
			.map(invok -> (CtInvocation<?>) invok)
			.collect(Collectors.toList());

		binderRoutines
			.stream()
			.filter(invok -> "on".equals(invok.getExecutable().getSimpleName()))
			.forEach(invok -> extractWidgets(invok, invok.getExecutable().getParameters()));
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
