package com.aware.utils;

import android.util.Log;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

public class Converters {

    /**
     * Converts temperature from Fahrenheit to Celsius
     *
     * @param fahrenheit
     * @return
     */
    public static float Fahrenheit2Celsius(float fahrenheit) {
        return (fahrenheit - 32) * 5 / 9;
    }

    /**
     * Converts elapsed time (in milliseconds) to human-readable hours:minutes (string)<br/>
     * If elapsed is 0, N/A is returned.
     *
     * @param milliseconds
     * @return
     */
    public static String readable_elapsed(long milliseconds) {
        if (milliseconds == 0) return "N/A";

        long h = (milliseconds / 1000) / 3600;
        long m = ((milliseconds / 1000) / 60) % 60;
        return h + "h" + ((m < 10) ? "0" + m : m) + "m";
    }

    /**
     * Checks if the string is a number
     *
     * @param str
     * @return boolean
     */
    public static boolean isNumeric(String str) {
        return str.matches("-?\\d+(\\.\\d+)?");  //match a number with optional '-' and decimal.
    }

    /**
     * Anonymises a string by substituting all alphanumeric characters with A, a, or 1.
     *
     * @param originalInput
     * @return string
     */
    public static String maskString(String originalInput){
        int length_input = originalInput.length();
        char[] input = originalInput.toCharArray();
        for(int i = 0; i < length_input; i++)
            if (Character.isUpperCase(input[i]))
                input[i] = 'A';
            else if (Character.isLowerCase(input[i]))
                input[i] = 'a';
            else if (Character.isDigit(input[i]))
                input[i] = '1';

        return String.valueOf(input);
    }

    /**
     * Formats a phone number according to the rules of the Google phone number library https://github.com/google/libphonenumber
     * If input is not a valid phone number and a NumberParseException is thrown, the original
     * address will be returned.
     *
     * @param address
     * @return string
     */
    public static String formatIfPhoneNumber(String address) {
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        String formattedPhoneNumber = address;
        try {
            Phonenumber.PhoneNumber numberProto = phoneUtil.parse(address, "US");
            formattedPhoneNumber = phoneUtil.format(
                    numberProto,
                    PhoneNumberUtil.PhoneNumberFormat.E164
            );
        } catch (NumberParseException e) {
            //TODO log this in Firebase so we can know when it happens remotely
            Log.e("NumberParseException was thrown for number: " + address + " ", e.getMessage(), e);
        }
        return formattedPhoneNumber;
    }
}
