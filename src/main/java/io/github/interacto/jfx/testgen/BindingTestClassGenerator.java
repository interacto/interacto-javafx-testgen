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

import java.util.Set;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtClass;

public class BindingTestClassGenerator {
	private final CtClass<?> bindingsClass;
	private final Set<CtInvocation<?>> binders;

	public BindingTestClassGenerator(final CtClass<?> bindingsClass, final Set<CtInvocation<?>> binders) {
		super();
		this.bindingsClass = bindingsClass;
		this.binders = binders;
	}

	public void generate() {

	}
}
