package com.github.mashlol;

import java.util.Random;

public class RandomUtils {
	// Seed this only once per lifetime of the plugin
	private static final Random randomGenerator = new Random();

	/**
	 * Gets a random number between 0 (inclusive) and max (exclusive)
	 * @param max upper (exclusive) range of the requested random number. 
	 * @return a random integer within the range requested
	 */
	public static int getRandomNumber( Integer max ){
		return randomGenerator.nextInt(max);
	}

	/**
	 * Gets a random number between 0 (inclusive) and 1 (exclusive)
	 * @param max upper (exclusive) range of the requested random number. 
	 * @return a random integer within the range requested
	 */
	public static double getRandomDouble(){
		return randomGenerator.nextDouble();
	}
}
