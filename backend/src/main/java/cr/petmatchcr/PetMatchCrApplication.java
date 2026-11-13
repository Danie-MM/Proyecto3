package cr.petmatchcr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(excludeName = {"org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration"})
public class PetMatchCrApplication {

	public static void main(String[] args) {
		SpringApplication.run(PetMatchCrApplication.class, args);
	}

}