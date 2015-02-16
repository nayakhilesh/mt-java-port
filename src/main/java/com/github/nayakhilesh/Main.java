package com.github.nayakhilesh;

import com.google.common.base.Charsets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

public class Main {

    public static void main(String[] args) throws IOException {

        //usage: corpus.en.txt corpus.es.txt

        long start = System.currentTimeMillis();

        final Properties properties = new Properties();
        properties.load(Main.class.getClassLoader().getResourceAsStream("mt.properties"));

        MachineTranslator translator = new MachineTranslator(properties, args[0], args[1]);

        long end = System.currentTimeMillis();
        System.out.println("Total time=" + (end - start) / 1000.0 + "s");

        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in, Charsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(translator.translate(line));
            }
        }

    }

}
