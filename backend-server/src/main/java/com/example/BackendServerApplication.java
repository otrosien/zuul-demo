package com.example;

import java.util.concurrent.ThreadLocalRandom;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class BackendServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendServerApplication.class, args);
	}

	@RequestMapping(method=RequestMethod.GET, path="/api/ok")
	public String ok() {
		return "ok";
	}

	@RequestMapping(method=RequestMethod.GET, path="/api/1s")
	public String second() {
		try {
			Thread.sleep(1000L);
		} catch (InterruptedException e) {
			//
			Thread.interrupted();
		}
		return "ok";
	}

	@RequestMapping(method=RequestMethod.GET, path="/api/random")
	public String random() {
		try {
			Thread.sleep(ThreadLocalRandom.current().nextLong(1_000L));
		} catch (InterruptedException e) {
			//
		}
		return "ok";
	}

	@RequestMapping(method=RequestMethod.GET, path="/api/sleep/{millis}")
	public String seconds(@PathVariable("millis") Long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			//
		}
		return "ok";
	}

	@RequestMapping(method=RequestMethod.POST, path="/api/post/{millis}")
	public String postSleep(@PathVariable("millis") Long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			//
		}
		return "ok";
	}
}