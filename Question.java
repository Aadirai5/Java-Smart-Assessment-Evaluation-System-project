import java.util.List;

public record Question(int id, String topic, String text, List<String> options, int answer) {}
