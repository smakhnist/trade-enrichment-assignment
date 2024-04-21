package com.verygoodbank.misc;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class GenerateBigTradeFile {
    public static void main(String[] args) throws IOException {
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("medium-trade-file.csv"))) {
            for (int i = 1; i < 100000; i++) {
                bufferedWriter.write("20160101,1,EUR," + i + ".0");
                bufferedWriter.newLine();
            }
        }
    }
}
