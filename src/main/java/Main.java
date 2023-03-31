import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

public class Main {
    static final String LETTER_SET = "abc";
    static final int MAX_LENGTH_OF_TEXT = 100_000;
    static final int QUANTITY_OF_TEXTS = 10_000;
    static final int QUEUE_SIZE = 100;  //макс. размер очередей, где будут хранится тексты для букв. анализа
    static final ArrayBlockingQueue<String> queueA = new ArrayBlockingQueue<>(QUEUE_SIZE);
    static final ArrayBlockingQueue<String> queueB = new ArrayBlockingQueue<>(QUEUE_SIZE);
    static final ArrayBlockingQueue<String> queueC = new ArrayBlockingQueue<>(QUEUE_SIZE);
    static List<Callable<String>> analyzers = new CopyOnWriteArrayList<>();

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        new Thread(() -> {
            for (int j = 0; j < QUANTITY_OF_TEXTS; j++) {
                try {
                    queueA.put(TextGenerator.generateText(LETTER_SET, MAX_LENGTH_OF_TEXT));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        Callable<String> analyserA = analyzeByQuantityOfLetter("a", queueA, queueB, QUANTITY_OF_TEXTS);
        Callable<String> analyserB = analyzeByQuantityOfLetter("b", queueB, queueC, QUANTITY_OF_TEXTS);
        Callable<String> analyserC = analyzeByQuantityOfLetter("c", queueC, null, QUANTITY_OF_TEXTS);
        analyzers.add(analyserA);
        analyzers.add(analyserB);
        analyzers.add(analyserC);
        ExecutorService threadPull = Executors.newFixedThreadPool(LETTER_SET.length());
        List<Future<String>> results = threadPull.invokeAll(analyzers);

        for (Future<String> result : results) {
            System.out.println(result.get());
        }
        threadPull.shutdown();
    }

    static Callable<String> analyzeByQuantityOfLetter(String letter, BlockingQueue<String> queueForInput, BlockingQueue<String> queueForOutput, int quantityOfTexts) throws InterruptedException {
        return () -> {
            System.out.println("start analyser-" + letter);
            int actualMax = 0;
            String textWithMax = "";
            for (int i = 0; i < quantityOfTexts; i++) {
                String text = queueForInput.take();
                int max = (int) Arrays.stream(text.split(""))
                        .filter(x -> x.equals(letter))
                        .count();
                if (actualMax < max) {
                    actualMax = max;
                    textWithMax = text;
                }
                if (queueForOutput != null) {
                    queueForOutput.put(text);
                }
            }
            System.out.println("analyze-" + letter + " is over");
            return "Анализ текстов на макс. количество символов " + letter + ":\n" +
                   "Чаще всего символ " + letter + " встречается в строке - " + textWithMax + "\n" +
                   "Символ " + letter + " встретился " + actualMax + " раз.";
        };
    }
}