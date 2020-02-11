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

import io.github.interacto.jfx.binding.api.BaseBinderBuilder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.reference.CtExecutableReference;

public class BindingProcessor extends AbstractProcessor<CtInvocation<?>> {
	final Map<CtClass<?>, Set<CtInvocation<?>>> binders;
	final FXMLExtractor fxmls;

	public BindingProcessor(final FXMLExtractor fxmls) {
		super();
		this.fxmls = fxmls;
		binders = new HashMap<>();
	}

	@Override
	public boolean isToBeProcessed(final CtInvocation<?> candidate) {
		final CtExecutableReference<?> exec = candidate.getExecutable();

		return "bind".equals(exec.getSimpleName()) &&
			exec.getDeclaringType().isSubtypeOf(candidate.getFactory().createCtTypeReference(BaseBinderBuilder.class));
	}

	@Override
	public void process(final CtInvocation<?> element) {
		final CtClass<?> parentClass = element.getParent(CtClass.class);
		final Set<CtInvocation<?>> invoks = binders.computeIfAbsent(parentClass, k -> new HashSet<>());
		invoks.add(element);
	}

	@Override
	public void processingDone() {
		binders.forEach((key, value) -> new BindingTestClassGenerator(key, value, fxmls.fxmlData.get(key.getQualifiedName())).generate());
	}
}
