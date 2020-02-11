package io.github.interacto.jfx.testgen;

import io.github.interacto.jfx.interaction.library.ButtonPressed;
import java.util.Map;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.reference.CtVariableReference;

/**
 * Proof of concept.
 */
public class InteractionSequencesGen {
	interface SeqGen {
		void genSequences(final CtMethod<?> test, final CtVariableReference<?> robot, CtFieldReference<?> widget);
	}

	private static final Map<String, SeqGen> generatorCalls = Map.of(
		ButtonPressed.class.getName(), (t, r, w) -> genButtonPressedSequences(t, r, w)
	);

	public static void genSequences(final CtTypeReference<?> interactionType, final CtMethod<?> test, final CtVariableReference<?> robot,
			final CtFieldReference<?> widget) {
		System.out.println(interactionType.getQualifiedName() + " " + ButtonPressed.class.getName());
		generatorCalls.getOrDefault(interactionType.getQualifiedName(), (t, r, w) -> {}).genSequences(test, robot, widget);
	}

	public static void genButtonPressedSequences(final CtMethod<?> test, final CtVariableReference<?> robot, CtFieldReference<?> w) {
		final Factory factory = robot.getFactory();
		test.getBody().addStatement(
			factory.createInvocation(factory.createVariableRead(robot, false), findMethod("clickOn", robot.getType(), 1),
				factory.createVariableRead(w, false))
		);
	}

	private static CtExecutableReference<?> findMethod(final String name, final CtTypeReference<?> type, final int nbArgs) {
		return type.getAllExecutables()
			.stream()
			.filter(e -> name.equals(e.getSimpleName()) && e.getParameters().size() == nbArgs)
			.findFirst()
			.orElseThrow();
	}
}
