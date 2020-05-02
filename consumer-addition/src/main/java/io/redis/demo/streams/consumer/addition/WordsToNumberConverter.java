package io.redis.demo.streams.consumer.addition;

import java.util.Arrays;
import java.util.List;


public class WordsToNumberConverter {

    final static List<String> numbers = Arrays.asList("zero", "one", "two", "three", "four", "five", "six", "seven", "eight","nine");

    public static long getNumberFromWords(String words) throws  Exception{
        List<String> items = Arrays.asList(words.trim().toLowerCase().split("\\s+"));
        StringBuilder sb = new StringBuilder();
        items.forEach((n) -> {
            if (n.equals("zero")) {
                sb.append(0);
            } else if (n.equals("one")) {
                sb.append(1);
            } else if (n.equals("two")) {
                sb.append(2);
            } else if (n.equals("three")) {
                sb.append(3);
            } else if (n.equals("four")) {
                sb.append(4);
            } else if (n.equals("five")) {
                sb.append(5);
            } else if (n.equals("six")) {
                sb.append(6);
            } else if (n.equals("seven")) {
                sb.append(7);
            } else if (n.equals("eight")) {
                sb.append(8);
            } else if (n.equals("nine")) {
                sb.append(9);
            } else {
                throw new RuntimeException("Cannot convert this text");
            }
        });
        return Long.parseLong(sb.toString());
    }


}
