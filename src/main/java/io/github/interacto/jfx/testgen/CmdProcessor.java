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

import io.github.interacto.command.Command;
import spoon.processing.AbstractProcessor;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.reference.CtTypeReference;

public class CmdProcessor extends AbstractProcessor<CtClass<?>> {
	private CtTypeReference<?> cmdRef;

	public CmdProcessor() {
		super();
	}

	@Override
	public boolean isToBeProcessed(final CtClass<?> candidate) {
		if(cmdRef == null) {
			cmdRef = getFactory().createCtTypeReference(Command.class);
		}
		return !candidate.isAbstract() && candidate.isSubtypeOf(cmdRef);
	}

	@Override
	public void process(final CtClass ctClass) {
		new CmdTestGenerator(ctClass).generate();
	}
}
