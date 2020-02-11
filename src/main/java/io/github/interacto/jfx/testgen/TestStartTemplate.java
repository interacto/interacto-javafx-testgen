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
