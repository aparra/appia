package net.sf.appia.protocols.total.symmetric;


/**
 * Utility functions for the protocol.
 */
public class Utils {

    public static long min(long a, long b) {
        if (a < b)
            return a;
        else
            return b;
    }

    public static long max(long a, long b) {
        if (a > b)
            return a;
        else
            return b;
    }

    //	public static boolean DEBUG = true;
    public static void show(String line) {
        System.out.println(line);
    }
}
