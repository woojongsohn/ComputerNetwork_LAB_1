import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;

public class QuizClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    // GUI 관련 필드
    private JFrame frame;
    private JTextArea questionArea;
    private JTextField answerField;
    private JButton submitButton;
    private JLabel feedbackLabel; // 왼쪽 아래
    private JLabel scoreLabel;    // 오른쪽 아래

    public QuizClient() {
        try {
            // 서버와 연결
            socket = new Socket("localhost", 1234);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // GUI 초기화
            initializeGUI();
            loadNextQuestion();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Unable to connect to the server: " + e.getMessage(),
                    "Connection Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private void initializeGUI() {
        frame = new JFrame("Quiz Game");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);
        frame.setLayout(new BorderLayout(10, 10));

        // 질문 영역
        questionArea = new JTextArea();
        questionArea.setEditable(false);
        questionArea.setFont(new Font("Arial", Font.BOLD, 18));
        questionArea.setLineWrap(true);
        questionArea.setWrapStyleWord(true);
        questionArea.setRows(5);
        JScrollPane questionScrollPane = new JScrollPane(questionArea);
        frame.add(questionScrollPane, BorderLayout.NORTH);

        // 답변 입력 필드와 제출 버튼
        JPanel answerPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        answerField = new JTextField(20);
        answerField.setFont(new Font("Arial", Font.PLAIN, 16));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        answerPanel.add(answerField, gbc);

        submitButton = new JButton("Submit");
        submitButton.setFont(new Font("Arial", Font.BOLD, 16));
        gbc.gridy = 1;
        answerPanel.add(submitButton, gbc);

        frame.add(answerPanel, BorderLayout.CENTER);

        // 피드백 메시지와 점수 표시
        feedbackLabel = new JLabel("Please enter your answer and click submit.");
        feedbackLabel.setFont(new Font("Arial", Font.ITALIC, 14));
        feedbackLabel.setHorizontalAlignment(SwingConstants.LEFT);

        scoreLabel = new JLabel("Score: 0");
        scoreLabel.setFont(new Font("Arial", Font.BOLD, 14));
        scoreLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        JPanel feedbackPanel = new JPanel(new BorderLayout());
        feedbackPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        feedbackPanel.add(feedbackLabel, BorderLayout.WEST);
        feedbackPanel.add(scoreLabel, BorderLayout.EAST);
        frame.add(feedbackPanel, BorderLayout.SOUTH);

        // 제출 버튼 클릭 이벤트 처리
        submitButton.addActionListener(e -> handleSubmit());

        // Enter 키로 제출 기능 추가
        answerField.addActionListener(e -> handleSubmit());

        frame.setVisible(true);
    }

    private void handleSubmit() {
        String answer = answerField.getText();
        submitAnswer(answer);
        answerField.setText("");
    }

    private void loadNextQuestion() {
        try {
            String question = in.readLine(); // 서버로부터 질문 수신
            if (question == null || question.contains("Quiz is over")) {
                feedbackLabel.setText(question != null ? question : "Connection lost. Quiz ended.");
                submitButton.setEnabled(false);
                socket.close();
            } else {
                questionArea.setText(question);
            }
        } catch (SocketException e) {
            feedbackLabel.setText("Connection lost. Please restart the quiz.");
            submitButton.setEnabled(false);
        } catch (IOException e) {
            feedbackLabel.setText("Error receiving question: " + e.getMessage());
        }
    }

    private void submitAnswer(String answer) {
        if (answer.trim().isEmpty()) {
            feedbackLabel.setText("Answer cannot be empty!");
            return;
        }

        try {
            out.println(answer); // 서버에 답변 전송
            String feedback = in.readLine(); // 서버로부터 피드백 수신

            // 피드백 메시지와 점수를 분리
            if (feedback.contains("|")) {
                String[] parts = feedback.split("\\|"); // "Correct!|Score:10" 형태로 분리
                feedbackLabel.setText(parts[0]); // 정답/오답 메시지
                if (parts[1].startsWith("Score:")) {
                    String score = parts[1].split(":")[1]; // 점수 추출
                    scoreLabel.setText("Score: " + score); // 점수 업데이트
                }
            } else {
                feedbackLabel.setText(feedback); // 기타 메시지 처리
            }

            loadNextQuestion();
        } catch (SocketException e) {
            feedbackLabel.setText("Connection lost. Please restart the quiz.");
            submitButton.setEnabled(false);
        } catch (IOException e) {
            feedbackLabel.setText("Error sending answer: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        new QuizClient();
    }
}
