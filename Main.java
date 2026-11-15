import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Smart Assessment & Evaluation System - pure Java (JDK built-in HTTP server). */
public class Main {

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer s = HttpServer.create(new InetSocketAddress(port), 0);
        s.createContext("/", Main::home);
        s.createContext("/quiz", Main::quiz);
        s.createContext("/submit", Main::submit);
        s.createContext("/leaderboard", Main::leaderboard);
        s.createContext("/admin", Main::admin);
        s.createContext("/admin/add", Main::adminAdd);
        s.start();
        System.out.println("Smart Assessment & Evaluation System running on port " + port);
    }

    // ---------- Handlers ----------
    static void home(HttpExchange x) throws IOException {
        if (!x.getRequestURI().getPath().equals("/")) { send(x, 404, page("Not found", "<p>Page not found.</p>")); return; }
        send(x, 200, page("Home",
            "<div class='card'><h2>Start an Assessment</h2><form action='/quiz' method='get'>"
            + "<label>Your name</label><input name='name' required placeholder='Enter your name'>"
            + "<button>Begin Test (" + Store.questions.size() + " questions)</button></form></div>"
            + "<div class='card'><h3>Quick stats</h3><p>Attempts so far: <b>" + Store.results.size()
            + "</b> &middot; Class average: <b>" + String.format("%.1f", avg()) + "%</b></p></div>"));
    }

    static void quiz(HttpExchange x) throws IOException {
        String name = param(query(x.getRequestURI().getRawQuery()), "name");
        if (name.isBlank()) { redirect(x, "/"); return; }
        StringBuilder b = new StringBuilder("<form action='/submit' method='post'><input type='hidden' name='name' value='" + esc(name) + "'>");
        b.append("<p>Candidate: <b>").append(esc(name)).append("</b></p>");
        int n = 1;
        for (Question q : Store.questions) {
            b.append("<div class='card'><p><b>Q").append(n++).append(".</b> <span class='tag'>").append(esc(q.topic()))
             .append("</span><br>").append(esc(q.text())).append("</p>");
            for (int i = 0; i < q.options().size(); i++)
                b.append("<label class='opt'><input type='radio' name='q").append(q.id()).append("' value='").append(i)
                 .append("'> ").append(esc(q.options().get(i))).append("</label>");
            b.append("</div>");
        }
        b.append("<button>Submit Answers</button></form>");
        send(x, 200, page("Assessment", b.toString()));
    }

    static void submit(HttpExchange x) throws IOException {
        Map<String, String> p = query(new String(x.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        String name = param(p, "name");
        Map<Integer, Integer> given = new HashMap<>();
        for (Question q : Store.questions) {
            String v = p.get("q" + q.id());
            if (v != null) given.put(q.id(), Integer.parseInt(v));
        }
        Result r = Evaluator.evaluate(name.isBlank() ? "Anonymous" : name, Store.questions, given);
        Store.results.add(r);

        StringBuilder b = new StringBuilder();
        b.append("<div class='card center'><h2>").append(esc(r.student())).append("</h2><div class='big'>")
         .append(r.score()).append(" / ").append(r.total()).append("</div><p>")
         .append(String.format("%.1f", r.percent())).append("% &middot; Grade <b>")
         .append(Evaluator.grade(r.percent())).append("</b></p><p>").append(esc(Evaluator.feedback(r))).append("</p></div>");
        b.append("<div class='card'><h3>Topic-wise performance</h3>");
        for (var e : r.topicStats().entrySet()) {
            int pct = e.getValue()[0] * 100 / e.getValue()[1];
            b.append("<p>").append(esc(e.getKey())).append(" (").append(e.getValue()[0]).append("/").append(e.getValue()[1])
             .append(")</p><div class='bar'><div style='width:").append(pct).append("%'></div></div>");
        }
        b.append("</div><div class='card'><h3>Answer review</h3>");
        int n = 1;
        for (Question q : Store.questions) {
            int g = given.getOrDefault(q.id(), -1);
            boolean ok = g == q.answer();
            b.append("<p class='").append(ok ? "ok" : "bad").append("'><b>Q").append(n++).append(".</b> ").append(esc(q.text()))
             .append("<br>Your answer: ").append(g < 0 ? "<i>not answered</i>" : esc(q.options().get(g)))
             .append(ok ? " &#10003;" : " &#10007; &mdash; Correct: " + esc(q.options().get(q.answer()))).append("</p>");
        }
        b.append("</div><a class='btn' href='/leaderboard'>View Leaderboard</a>");
        send(x, 200, page("Result", b.toString()));
    }

    static void leaderboard(HttpExchange x) throws IOException {
        List<Result> sorted = new ArrayList<>(Store.results);
        sorted.sort(Comparator.comparingDouble(Result::percent).reversed().thenComparingLong(Result::time));
        StringBuilder b = new StringBuilder("<div class='card'><h2>Leaderboard</h2><table><tr><th>Rank</th><th>Name</th><th>Score</th><th>%</th><th>Grade</th></tr>");
        int rank = 1;
        for (Result r : sorted)
            b.append("<tr><td>").append(rank++).append("</td><td>").append(esc(r.student())).append("</td><td>")
             .append(r.score()).append("/").append(r.total()).append("</td><td>").append(String.format("%.1f", r.percent()))
             .append("</td><td>").append(Evaluator.grade(r.percent())).append("</td></tr>");
        b.append("</table>");
        if (sorted.isEmpty()) b.append("<p>No attempts yet.</p>");
        b.append("</div>");
        send(x, 200, page("Leaderboard", b.toString()));
    }

    static void admin(HttpExchange x) throws IOException {
        String msg = param(query(x.getRequestURI().getRawQuery()), "msg");
        send(x, 200, page("Admin",
            "<div class='card'><h2>Add Question</h2>" + (msg.isBlank() ? "" : "<p class='ok'>" + esc(msg) + "</p>")
            + "<form action='/admin/add' method='post'><label>Topic</label><input name='topic' required>"
            + "<label>Question</label><input name='text' required>"
            + "<label>Option 1</label><input name='o1' required><label>Option 2</label><input name='o2' required>"
            + "<label>Option 3</label><input name='o3' required><label>Option 4</label><input name='o4' required>"
            + "<label>Correct option (1-4)</label><input name='ans' type='number' min='1' max='4' required>"
            + "<button>Add Question</button></form></div>"
            + "<div class='card'><p>Total questions in bank: <b>" + Store.questions.size() + "</b></p></div>"));
    }

    static void adminAdd(HttpExchange x) throws IOException {
        Map<String, String> p = query(new String(x.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        try {
            int ans = Integer.parseInt(param(p, "ans")) - 1;
            if (ans < 0 || ans > 3) throw new NumberFormatException();
            Store.add(param(p, "topic"), param(p, "text"),
                List.of(param(p, "o1"), param(p, "o2"), param(p, "o3"), param(p, "o4")), ans);
            redirect(x, "/admin?msg=Question+added");
        } catch (NumberFormatException e) {
            redirect(x, "/admin?msg=Invalid+correct+option");
        }
    }

    // ---------- Helpers ----------
    static double avg() {
        return Store.results.stream().mapToDouble(Result::percent).average().orElse(0);
    }

    static Map<String, String> query(String s) {
        Map<String, String> m = new HashMap<>();
        if (s == null || s.isEmpty()) return m;
        for (String kv : s.split("&")) {
            int i = kv.indexOf('=');
            if (i < 0) continue;
            m.put(URLDecoder.decode(kv.substring(0, i), StandardCharsets.UTF_8),
                  URLDecoder.decode(kv.substring(i + 1), StandardCharsets.UTF_8));
        }
        return m;
    }

    static String param(Map<String, String> m, String k) { return m.getOrDefault(k, "").trim(); }

    static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    static void redirect(HttpExchange x, String to) throws IOException {
        x.getResponseHeaders().add("Location", to);
        x.sendResponseHeaders(303, -1);
        x.close();
    }

    static void send(HttpExchange x, int code, String html) throws IOException {
        byte[] d = html.getBytes(StandardCharsets.UTF_8);
        x.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
        x.sendResponseHeaders(code, d.length);
        try (OutputStream o = x.getResponseBody()) { o.write(d); }
    }

    static String page(String title, String body) {
        return "<!DOCTYPE html><html><head><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'>"
            + "<title>" + title + " | Smart Assessment System</title><style>"
            + "body{font-family:system-ui,sans-serif;background:#f3f5f9;color:#1b2430;margin:0}"
            + "header{background:#1f3a8a;color:#fff;padding:14px 20px;display:flex;gap:18px;align-items:center;flex-wrap:wrap}"
            + "header a{color:#cfe0ff;text-decoration:none}header h1{font-size:18px;margin:0;flex:1}"
            + "main{max-width:760px;margin:20px auto;padding:0 14px}"
            + ".card{background:#fff;border-radius:10px;padding:16px;margin-bottom:14px;box-shadow:0 1px 3px #0001}"
            + "input{display:block;width:100%;box-sizing:border-box;padding:9px;margin:6px 0 12px;border:1px solid #c6cdd9;border-radius:6px}"
            + "input[type=radio]{display:inline;width:auto;margin:0 8px 0 0}.opt{display:block;padding:6px 0}"
            + "button,.btn{background:#1f3a8a;color:#fff;border:0;padding:10px 18px;border-radius:6px;cursor:pointer;text-decoration:none;display:inline-block}"
            + ".tag{background:#e3eaff;color:#1f3a8a;font-size:12px;padding:2px 8px;border-radius:10px}"
            + ".center{text-align:center}.big{font-size:44px;font-weight:700;color:#1f3a8a}"
            + ".bar{background:#e5e9f2;border-radius:6px;height:10px}.bar div{background:#2e9e5b;height:10px;border-radius:6px}"
            + ".ok{color:#1d7a46}.bad{color:#b3261e}table{width:100%;border-collapse:collapse}"
            + "td,th{padding:8px;border-bottom:1px solid #e5e9f2;text-align:left}</style></head><body>"
            + "<header><h1>Smart Assessment &amp; Evaluation System</h1><a href='/'>Home</a><a href='/leaderboard'>Leaderboard</a><a href='/admin'>Admin</a></header>"
            + "<main>" + body + "</main></body></html>";
    }
}
