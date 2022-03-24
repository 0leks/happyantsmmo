package ok;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import ok.games.coingame.CoinGame;

@SpringBootApplication
public class Application {

	public static CoinGame coingame;
	public static void main(String[] args) {
		coingame = new CoinGame();
		coingame.start();
		
		SpringApplication.run(Application.class, args);
	}

//	@Bean
//	public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
//		return args -> {
//			System.out.println("running server 5");
//		};
//	}

}
