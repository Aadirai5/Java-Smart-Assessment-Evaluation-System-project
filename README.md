# Smart Assessment & Evaluation System (Pure Java)

Online MCQ assessment platform: auto-grading, grades, topic-wise analytics, smart feedback, leaderboard, and an admin page to add questions. Uses only the JDK (no frameworks).

## Run locally (JDK 17+)
    mkdir out && javac -d out src/*.java
    java -cp out Main
Open http://localhost:8080

## Deploy
Push this folder to GitHub, then create a "Web Service" on Render / Railway / Fly.io and choose the included Dockerfile. The app reads the PORT environment variable.

## Structure
- Main.java: HTTP server, routes, HTML pages
- Evaluator.java: grading, grade scale, feedback logic
- Store.java: in-memory question bank and results
- Question.java / Result.java: data models
