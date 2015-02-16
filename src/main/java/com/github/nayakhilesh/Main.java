package com.github.nayakhilesh;

import com.google.common.base.Charsets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

public class Main {

    public static void main(String[] args) throws Exception {

        //usage: corpus.en, corpus.es

        long start = System.currentTimeMillis();

        final Properties properties = new Properties();
        properties.load(Main.class.getResourceAsStream("mt.properties"));

        MachineTranslator translator = new MachineTranslator(properties, args[0], args[1]);

        long end = System.currentTimeMillis();
        System.out.println("Total time=" + (end - start) / 1000.0 + "s");

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in, Charsets.UTF_8));
        while (true) {
            try {
                System.out.println(translator.translate(br.readLine()));
            } catch (IOException ioe) {
                System.out.println("IO error trying to read your name!");
                System.exit(1);
            }
        }

    }

}
