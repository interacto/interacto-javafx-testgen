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

import com.sun.javafx.application.PlatformImpl;
import java.util.concurrent.CountDownLatch;
import spoon.MavenLauncher;
import spoon.support.compiler.FileSystemFile;

public final class Main {
	private Main() {
		super();
	}

	public static void main(final String[] args) throws InterruptedException {
		try {
			final MavenLauncher launcher = new MavenLauncher("../example-jfx-drawingeditor/pom.xml", MavenLauncher.SOURCE_TYPE.APP_SOURCE);

			final CountDownLatch startupLatch = new CountDownLatch(1);
			PlatformImpl.startup(() -> startupLatch.countDown());
			startupLatch.await();

			launcher.addTemplateResource(new FileSystemFile("src/main/java/io/github/interacto/jfx/testgen/CmdTestClassTemplate.java"));
			launcher.getEnvironment().setAutoImports(true);
			launcher.getEnvironment().setCopyResources(false);
//			launcher.addProcessor(new CmdProcessor());
			launcher.addProcessor(new BindingProcessor());
			launcher.run();
		}finally {
			PlatformImpl.exit();
		}
	}
}
