package com.spulido.agent;

import java.util.concurrent.CountDownLatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AgentApplication {

	private static final CountDownLatch latch = new CountDownLatch(1);
	private static ConfigurableApplicationContext context;

	public static void main(String[] args) throws InterruptedException {
		SpringApplication app = new SpringApplication(AgentApplication.class);
		app.setWebApplicationType(WebApplicationType.NONE);
		context = app.run(args);
		Runtime.getRuntime().addShutdownHook(new Thread(AgentApplication::shutdown));
		latch.await();
		context.close();
	}

	public static void shutdown() {
		latch.countDown();
	}
}
