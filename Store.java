import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/** In-memory question bank and results store. */
public class Store {
    public static final List<Question> questions = new CopyOnWriteArrayList<>();
    public static final List<Result> results = new CopyOnWriteArrayList<>();
    private static final AtomicInteger ids = new AtomicInteger(1);

    public static Question add(String topic, String text, List<String> opts, int ans) {
        Question q = new Question(ids.getAndIncrement(), topic, text, opts, ans);
        questions.add(q);
        return q;
    }

    static {
        add("OOP", "Which keyword is used to inherit a class in Java?", List.of("implements", "extends", "inherits", "super"), 1);
        add("OOP", "Which concept hides internal details of a class?", List.of("Polymorphism", "Inheritance", "Encapsulation", "Abstraction only"), 2);
        add("OOP", "Which can an interface NOT contain?", List.of("Abstract methods", "Constructors", "Default methods", "Static methods"), 1);
        add("Collections", "Which collection does not allow duplicates?", List.of("ArrayList", "LinkedList", "HashSet", "Vector"), 2);
        add("Collections", "Which class maintains insertion order of key-value pairs?", List.of("HashMap", "TreeMap", "LinkedHashMap", "Hashtable"), 2);
        add("Collections", "Average time complexity of HashMap.get()?", List.of("O(n)", "O(log n)", "O(1)", "O(n log n)"), 2);
        add("Exceptions", "Which is a checked exception?", List.of("NullPointerException", "IOException", "ArithmeticException", "ArrayIndexOutOfBoundsException"), 1);
        add("Exceptions", "Which block always executes?", List.of("try", "catch", "finally", "throw"), 2);
        add("Multithreading", "Which method starts a new thread?", List.of("run()", "start()", "execute()", "begin()"), 1);
        add("Multithreading", "Which keyword provides mutual exclusion?", List.of("volatile", "transient", "synchronized", "static"), 2);
    }
}
