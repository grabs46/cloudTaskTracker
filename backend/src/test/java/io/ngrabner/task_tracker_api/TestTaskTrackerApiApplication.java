package io.ngrabner.task_tracker_api;

import org.springframework.boot.SpringApplication;

public class TestTaskTrackerApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(TaskTrackerApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
