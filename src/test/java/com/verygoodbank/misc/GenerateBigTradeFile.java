package com.verygoodbank.misc;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class GenerateBigTradeFile {
    public static void main(String[] args) throws IOException {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate locaDate = LocalDate.of(2016, 1, 1);
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("medium-trade-file.csv"))) {
            for (int i = 1; i < 100000; i++) {
                locaDate = i % 1000 == 0 ? locaDate.plusDays(1) : locaDate;
                bufferedWriter.write(locaDate.format(dateFormatter) + ",1,EUR," + i + ".0");
                bufferedWriter.newLine();
            }
        }
    }
}
