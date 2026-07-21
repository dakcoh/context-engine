package com.commerce.context_engine;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ContextEngineApplicationTests {

	@Test
	void contextLoads() {
	}

	@Test
	void runtimeDoesNotContainWebServerOrActuator() {
		assertThat(classPresent("org.apache.catalina.startup.Tomcat")).isFalse();
		assertThat(classPresent("org.springframework.web.servlet.DispatcherServlet")).isFalse();
		assertThat(classPresent("org.springframework.boot.actuate.health.HealthEndpoint")).isFalse();
	}

	private static boolean classPresent(String name) {
		try {
			Class.forName(name);
			return true;
		}
		catch (ClassNotFoundException ignored) {
			return false;
		}
	}

}
