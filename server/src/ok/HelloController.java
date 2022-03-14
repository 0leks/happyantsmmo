package ok;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

	@GetMapping("/")
	public String index() {
		System.err.println("received request");
		System.out.println("received request");
		return "Greetings from Spring Boot! asdf";
	}

}
