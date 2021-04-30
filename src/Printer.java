public class Printer {

    public static boolean ENABLE_OUTPUT = true;
    public static boolean ENABLE_METHOD = false;
    public static boolean ENABLE_DEBUG = false;
    public static boolean ENABLE_ERROR = true;

    public static void debug (String TAG, String message) {
        if (ENABLE_DEBUG)
            System.out.printf("%-10s |%-20s : %s\n", "Debug", TAG, message);
    }

    public static void error (String TAG, String message) {
        if (ENABLE_ERROR)
            System.out.printf("%-10s |%-20s : %s\n", "Error", TAG, message);
    }

    public static void output (String TAG, String message) {
        if (ENABLE_OUTPUT)
            System.out.printf("%-10s |%-20s : %s\n", "Output", TAG, message);
    }

    public static void insideMethod(String TAG, String methodName) {
        String msg = String.format("Inside %s()", methodName);
        if (ENABLE_METHOD)
            System.out.printf("%-10s |%-20s : %s\n", "Method", TAG, methodName);
    }
}
