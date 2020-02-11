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

import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.mockito.Mockito;
import org.testfx.framework.junit5.Start;
import spoon.reflect.code.CtConstructorCall;
import spoon.template.ExtensionTemplate;
import spoon.template.Parameter;

public class TestStartTemplate extends ExtensionTemplate {
	@Parameter String Object;
	@Parameter
	String path;
	@Parameter CtConstructorCall<?> cons;
	@Parameter
	String controller;

	@Start
	void start(final Stage stage) throws IOException {
		final FXMLLoader loader = new FXMLLoader(getClass().getResource(path),
			null, new JavaFXBuilderFactory(), cl -> {
			if(cl == Object.class) {
				return cons.S();
			}
			return Mockito.mock(cl);
		});
		stage.setScene(new Scene(loader.load()));
		stage.show();
		controller = loader.getController();
	}
}
