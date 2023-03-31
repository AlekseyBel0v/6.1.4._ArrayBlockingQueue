import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class Main {
    static final String LETTER_SET = "abc";
    static final int MAX_LENGTH_OF_TEXT = 100_000;
    static final int QUANTITY_OF_TEXTS = 10_000;
    static final int QUEUE_SIZE = 100;  //макс. размер очередей, где будут хранится тексты для букв. анализа
    //тут будут создаваться очереди по одной на букву
    static CopyOnWriteArrayList<ArrayBlockingQueue<String>> queuesForTextAnalyse = new CopyOnWriteArrayList<>();
    //сюда добавятся анализеры по одному на букву
    static List<Callable<String>> analyzers = new ArrayList<>();

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        String[] letterSet = LETTER_SET.split("");
        for (int i = 0; i < letterSet.length; i++) {
            queuesForTextAnalyse.add(new ArrayBlockingQueue<>(QUEUE_SIZE));
            System.out.println("добавлена очередь " + i);

            final Integer finalI = i;
            analyzers.add(() -> {
                String letter = letterSet[finalI];
                BlockingQueue<String> queueForOutput = null;
                if (finalI < letterSet.length) {
                    queueForOutput = queuesForTextAnalyse.get(finalI + 1);
                }
                System.out.println("final i = " + finalI);
                Map.Entry<String, Integer> result = analyzeByQuantityOfLetter(letter, queuesForTextAnalyse.get(finalI), queueForOutput, QUANTITY_OF_TEXTS);
                String text = result.getKey();
                int maxRepeat = result.getValue();
                System.out.println("analyzer is over");
                return "Анализ текстов на макс. количество символов " + letter + ":\n" +
                       "Чаще всего символ " + letter + " встречается в строке - " + text + "\n" +
                       "Символ " + letter + " встретился " + maxRepeat + " раз.";
            });
        }
        ExecutorService threadPull = Executors.newFixedThreadPool(queuesForTextAnalyse.size());
        List<Future<String>> results = threadPull.invokeAll(analyzers);

        new Thread(() -> {
            for (int j = 0; j < QUANTITY_OF_TEXTS; j++) {
                try {
                    queuesForTextAnalyse.get(1).put(TextGenerator.generateText(LETTER_SET, MAX_LENGTH_OF_TEXT));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        for (Future<String> result : results) {
            System.out.println(result.get());
        }
        threadPull.shutdown();
    }

    static Map.Entry<String, Integer> analyzeByQuantityOfLetter(String letter, BlockingQueue<String> queueForInput, BlockingQueue<String> queueForOutput, int quantityOfTexts) throws InterruptedException {
        System.out.println("заработал анализер для " + letter);
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
        System.out.println("analyze for " + letter + " over");
        return Map.entry(textWithMax, actualMax);
    }
}
