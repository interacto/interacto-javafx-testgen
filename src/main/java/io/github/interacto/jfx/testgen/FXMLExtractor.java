package io.github.interacto.jfx.testgen;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FXMLExtractor {
	final Pattern fxcontroller;
	final Map<String, String> fxmlData;
	final String path;

	public FXMLExtractor(final String path) {
		super();
		this.path = path;
		fxmlData = new HashMap<>();
		fxcontroller = Pattern.compile("fx:controller=\"(.*)\"");
	}

	public void extract() throws IOException {
		fxmlData.putAll(
			Files.walk(Paths.get(path))
				.filter(path -> path.toFile().getName().endsWith(".fxml") && path.toFile().isFile())
				.map(path -> {
					try {
						return Map.entry(path.toString().replaceFirst(".*/main/resources", ""),
							String.join("\n", Files.readAllLines(path)));
					}catch(final IOException ex) {
						ex.printStackTrace();
						return null;
					}
				})
				.filter(Objects::nonNull)
				.map(entry -> {
					final Matcher matcher = fxcontroller.matcher(entry.getValue());
					if(matcher.find() && matcher.groupCount() == 1) {
						return Map.entry(matcher.group(1), entry.getKey());
					}
					return null;
				})
				.filter(Objects::nonNull)
				.collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()))
		);
	}
}
