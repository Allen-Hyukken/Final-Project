package com.cerebro.finalproject.model;

import jakarta.persistence.*;

// Answer Entity
@Entity
@Table(name = "answer")
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "attempt_id", nullable = false)
    private Attempt attempt;

    @ManyToOne
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @ManyToOne
    @JoinColumn(name = "choice_id")
    private Choice choice;

    @Column(name = "given_text", columnDefinition = "TEXT")
    private String givenText;

    @Column(nullable = false)
    private Boolean correct = false;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Attempt getAttempt() { return attempt; }
    public void setAttempt(Attempt attempt) { this.attempt = attempt; }

    public Question getQuestion() { return question; }
    public void setQuestion(Question question) { this.question = question; }

    public Choice getChoice() { return choice; }
    public void setChoice(Choice choice) { this.choice = choice; }

    public String getGivenText() { return givenText; }
    public void setGivenText(String givenText) { this.givenText = givenText; }

    public Boolean getCorrect() { return correct; }
    public void setCorrect(Boolean correct) { this.correct = correct; }

    // Helper method to extract essay score from givenText
    // Format: "ESSAY_SCORE:5.0|||<actual essay text>"
    public Double getEssayScore() {
        if (givenText != null && givenText.startsWith("ESSAY_SCORE:")) {
            try {
                int endIndex = givenText.indexOf("|||");
                if (endIndex > 0) {
                    String scoreStr = givenText.substring(12, endIndex);
                    return Double.parseDouble(scoreStr);
                }
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    // Helper method to get actual essay text without score prefix
    public String getActualEssayText() {
        if (givenText != null && givenText.startsWith("ESSAY_SCORE:")) {
            int startIndex = givenText.indexOf("|||");
            if (startIndex > 0 && startIndex + 3 < givenText.length()) {
                return givenText.substring(startIndex + 3);
            }
        }
        return givenText;
    }

    // Helper method to set essay score (stores in givenText with special format)
    public void setEssayScore(Double score, String essayText) {
        if (score != null) {
            this.givenText = "ESSAY_SCORE:" + score + "|||" + (essayText != null ? essayText : "");
            this.correct = true; // Mark as graded
        }
    }
}