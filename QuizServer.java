import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QuizServer {
    private static final int PORT = 1234;
    private static final List<Question> questionPool = new ArrayList<>();

    public static void main(String[] args) {
        // 퀴즈 풀 초기화
        initializeQuestions();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Quiz server has started.");

            while (true) {
                // 클라이언트 연결 대기
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());

                // 클라이언트 처리용 스레드 생성 및 시작
                new ClientHandler(clientSocket, getRandomQuestions(5)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 퀴즈 저장
    private static void initializeQuestions() {
        questionPool.add(new Question("What is the capital of KOREA?", "Seoul"));
        questionPool.add(new Question("What is ?", "12"));
        questionPool.add(new Question("Who wrote 'Hamlet'?", "Shakespeare"));
        questionPool.add(new Question("What is the chemical symbol for water?", "H2O"));
        questionPool.add(new Question("What is 5 * 6?", "30"));
        questionPool.add(new Question("What is the smallest prime number?", "2"));
        questionPool.add(new Question("What is the capital of Japan?", "Tokyo"));
        questionPool.add(new Question("Who painted the Mona Lisa?", "da Vinci"));
    }

    // 랜덤으로 n개의 질문 선택
    private static List<Question> getRandomQuestions(int n) {
        List<Question> randomQuestions = new ArrayList<>(questionPool);
        Collections.shuffle(randomQuestions); // 질문을 랜덤으로 섞음
        return randomQuestions.subList(0, n); // 상위 n개 질문 반환
    }
}

// 클라이언트 처리용 스레드 클래스
class ClientHandler extends Thread {
    private final Socket clientSocket;
    private final List<Question> questions;

    public ClientHandler(Socket clientSocket, List<Question> questions) {
        this.clientSocket = clientSocket;
        this.questions = questions;
    }

    @Override
    public void run() {
        try (PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {

            System.out.println("Handling client: " + clientSocket.getInetAddress());
            int score = 0;

            for (Question question : questions) {
                try {
                    out.println(question.getQuestionText()); // 질문 전송
                    String answer = in.readLine(); // 클라이언트 답변 수신

                    if (answer == null) { // 클라이언트가 연결을 끊었을 경우
                        System.out.println("Client disconnected unexpectedly.");
                        break;
                    }

                    if (question.isCorrectAnswer(answer)) {
                        score += 10; // 점수는 10점씩 증가
                        out.println("Correct!|Score:" + score); // 정답 메시지 + 점수 전송
                    } else {
                        out.println("Incorrect!|Score:" + score); // 오답 메시지 + 점수 전송
                    }
                } catch (SocketException e) {
                    System.err.println("Client connection lost: " + clientSocket.getInetAddress());
                    break;
                } catch (IOException e) {
                    System.err.println("Error reading client input: " + e.getMessage());
                }
            }

            out.println("Quiz is over. Final score: " + score); // 최종 점수 전송
        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Failed to close client socket: " + e.getMessage());
            }
            System.out.println("Thread for client [" + clientSocket.getInetAddress() + "] has ended.");
        }
    }
}

// 질문 클래스
class Question {
    private final String questionText;
    private final String answer;

    public Question(String questionText, String answer) {
        this.questionText = questionText;
        this.answer = answer;
    }

    public String getQuestionText() {
        return questionText;
    }

    public String getAnswer() {
        return answer;
    }

    public boolean isCorrectAnswer(String inputAnswer) {
        return answer.equalsIgnoreCase(inputAnswer);
    }
}
