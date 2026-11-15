import java.util.Map;

public record Result(String student, int score, int total, Map<String, int[]> topicStats, long time) {
    public double percent() { return total == 0 ? 0 : score * 100.0 / total; }
}
