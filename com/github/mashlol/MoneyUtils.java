package com.github.mashlol;

import java.text.DecimalFormat;

public class MoneyUtils {
	private static DecimalFormat moneyFormat = new DecimalFormat("#.##");
	private static DecimalFormat moneyFormatMill = new DecimalFormat("#.####");
	
	/**
     * Round a number to 2 (two) decimal places 
     * @param val number to rounded
     * @return the number was in val rounded to the nearest 2 (two) decimal places
     */
	public static double round ( double val ){
    	return (double) (Math.round( val * 100.0) / 100.0);
    }
	
	/**
     * Return a string representation of a money value with 2 (two) decimal places
     * @param val number to formated
     * @return string showing val with two decimal places
     */
	public static String format( double val ) {
		return moneyFormat.format(val);
	}

	/**
     * Return a string representation of a money value with 4 (four) decimal places
     * @param val number to formated
     * @return string showing val with two decimal places
     */
	public static String formatMill( double val ) {
		return moneyFormatMill.format(val);
	}
}
