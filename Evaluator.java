import java.util.*;

/** Auto-grading and analytics engine. */
public class Evaluator {

    public static Result evaluate(String student, List<Question> qs, Map<Integer, Integer> given) {
        int score = 0;
        Map<String, int[]> topics = new TreeMap<>(); // topic -> [correct, total]
        for (Question q : qs) {
            int[] t = topics.computeIfAbsent(q.topic(), k -> new int[2]);
            t[1]++;
            if (given.getOrDefault(q.id(), -1) == q.answer()) { score++; t[0]++; }
        }
        return new Result(student, score, qs.size(), topics, System.currentTimeMillis());
    }

    public static String grade(double p) {
        if (p >= 90) return "A+";
        if (p >= 80) return "A";
        if (p >= 70) return "B";
        if (p >= 60) return "C";
        if (p >= 50) return "D";
        return "F";
    }

    /** Smart feedback: finds strongest and weakest topics. */
    public static String feedback(Result r) {
        String best = null, worst = null;
        double bp = -1, wp = 101;
        for (var e : r.topicStats().entrySet()) {
            double p = e.getValue()[0] * 100.0 / e.getValue()[1];
            if (p > bp) { bp = p; best = e.getKey(); }
            if (p < wp) { wp = p; worst = e.getKey(); }
        }
        if (best == null) return "No data.";
        if (bp == wp) return "Consistent performance across all topics.";
        return "Strongest topic: " + best + " (" + Math.round(bp) + "%). Needs revision: "
                + worst + " (" + Math.round(wp) + "%).";
    }
}
