package godtier;

import org.springframework.boot.SpringApplication;

public class TestGodtierApplication {

	public static void main(String[] args) {
		SpringApplication.from(GodtierApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
