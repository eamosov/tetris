package ru.efreet.trading.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Created by fluder on 26/02/2018.
 */
public class IntFunction3 {

    static final int FUNCTION_SIZE = 12;
    static final int ROW_SIZE = FUNCTION_SIZE * 1 + 4;

    static final int TABLE_SIZE = (int) Math.pow(3.0, (double) FUNCTION_SIZE);

    private static byte[] table;
    private static int tableIndex = 0;

    static {
        try {
            table = Files.readAllBytes(Paths.get("IntFunction3.table"));
        } catch (IOException e) {
            table = new byte[TABLE_SIZE * ROW_SIZE];
            gen(new byte[ROW_SIZE], 0);
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    printCounters();
                    Files.write(Paths.get("IntFunction3.table"), table, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    private static void gen(byte[] src, int index) {

        if (index >= FUNCTION_SIZE) {
            System.arraycopy(src, 0, table, tableIndex * ROW_SIZE, ROW_SIZE);
            tableIndex++;
        } else {
            src[index] = 0;
            gen(src, index + 1);
            src[index] = 1;
            gen(src, index + 1);
            src[index] = 2;
            gen(src, index + 1);
        }
    }


    public static byte get(int function, int sd, int m1, int m2) {
        if (!(sd >= 0 && sd <= 2 && m1 >= 0 && m1 <= 1 && m2 >= 0 && m2 <= 1)) {
            throw new IllegalArgumentException(String.format("Illegal arguments %d %d %d %d", function, sd, m1, m2));
        }

        return table[ROW_SIZE * function + 4 * sd + 2 * m2 + m2];
    }

    public synchronized static void setCounter(int function, int data) {
        table[ROW_SIZE * function + FUNCTION_SIZE] = (byte) ((data & 0xFF000000) >> 24);
        table[ROW_SIZE * function + FUNCTION_SIZE + 1] = (byte) ((data & 0x00FF0000) >> 16);
        table[ROW_SIZE * function + FUNCTION_SIZE + 2] = (byte) ((data & 0x0000FF00) >> 8);
        table[ROW_SIZE * function + FUNCTION_SIZE + 3] = (byte) (data & 0xFF);
    }

    public synchronized static int getCounter(int function) {

        return
            (((int) table[ROW_SIZE * function + FUNCTION_SIZE] << 24) & 0xFF000000)
                | (((int) table[ROW_SIZE * function + FUNCTION_SIZE + 1] << 16) & 0xFF0000)
                | (((int) table[ROW_SIZE * function + FUNCTION_SIZE + 2] << 8) & 0xFF00)
                | ((int) table[ROW_SIZE * function + FUNCTION_SIZE + 3] & 0xFF);
    }

    public synchronized static void incCounter(int function) {
        setCounter(function, getCounter(function) + 1);
    }

    public static int size() {
        return TABLE_SIZE;
    }

    public static void printCounters() {
        int sum = 0;
        int count = 0;
        for (int i = 0; i < TABLE_SIZE; i++) {
            int v = getCounter(i);
            sum += v;
            if (v > 0) {
                count ++;
                //System.out.println(String.format("%d:%d", i, getCounter(i)));
            }
        }

        System.out.println(String.format("IntFunction3: registered %d profits for %d functions", sum, count));
    }

    public static void main(String args[]) throws IOException {

        printCounters();
        //System.out.println(getData(0));
        //setData(0, 12345);
        //System.out.println(getData(0) == 0xFFFFFFFF);

        //        for (int i = 0; i < 1000; i++) {
        //            byte[] tmp = new byte[ROW_SIZE];
        //            System.arraycopy(table, i * ROW_SIZE, tmp, 0, ROW_SIZE);
        //            System.out.println(i + ":" + Arrays.toString(tmp));
        //
        //        }
    }
}
