import java.util.Random;

public class TextGenerator {
    public static String generateText(String letters, int length) {
        Random random = new Random();
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < length; i++) {
            text.append(letters.charAt(random.nextInt(letters.length())));
        }
//        System.out.println(text);
        return text.toString();
    }
}
