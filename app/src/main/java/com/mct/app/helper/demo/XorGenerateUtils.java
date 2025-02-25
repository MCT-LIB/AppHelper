//package com.mct.app.helper.demo;
//
//import android.util.Base64;
//import android.util.Log;
//
//import androidx.annotation.NonNull;
//
//import com.mct.app.helper.admob.utils.DVC;
//
//import java.util.Locale;
//
//public class XorGenerateUtils {
//
//    public static void generate(String input, int keyLength) {
//        StringBuilder keyBuilder = new StringBuilder();
//        for (int i = 0; i < keyLength; i++) {
//            keyBuilder.append((char) (Math.random() * 26 + 'a'));
//        }
//        String key = Base64.encodeToString(randomKey(keyLength).getBytes(), Base64.DEFAULT);
//        String encoded = DVC.XOR.encrypt(input, key);
//        String decoded = DVC.XOR.decrypt(encoded, key);
//
//        String format = "XOR.decrypt(\"%s\", \"%s\")";
//        Log.e("ddd", "generate: " + escape(decoded) + " -> " + String.format(Locale.ENGLISH, format, escape(encoded), escape(key)));
//    }
//
//    @NonNull
//    private static String escape(@NonNull String s) {
//        StringBuilder sb = new StringBuilder();
//        for (int i = 0; i < s.length(); i++) {
//            char c = s.charAt(i);
//            if (c == '\n') {
//                sb.append("\\n");
//            } else if (c == '\r') {
//                sb.append("\\r");
//            } else if (c == '\t') {
//                sb.append("\\t");
//            } else if (c == '\\') {
//                sb.append("\\\\");
//            } else {
//                sb.append(c);
//            }
//        }
//        return sb.toString();
//    }
//
//    @NonNull
//    private static String randomKey(int keyLength) {
//        StringBuilder keyBuilder = new StringBuilder();
//        for (int i = 0; i < keyLength; i++) {
//            keyBuilder.append((char) (Math.random() * 26 + 'a'));
//        }
//        return keyBuilder.toString();
//    }
//
//    private XorGenerateUtils() {
//        //no instance
//    }
//}
