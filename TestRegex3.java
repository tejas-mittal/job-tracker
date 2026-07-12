import java.util.regex.*;

public class TestRegex3 {
    public static void main(String[] args) {
        String text = "Hi *Tejas MITTAL*, We appreciate your participation in the selection process for the Myntra Data Science Internship Hiring. Congratulations on getting shortlisted for the Online Assessment Round scheduled for today, 18th April 2026. It's imperative that you have at least 8GB RAM.";
        
        Pattern[] patterns = {
            Pattern.compile("(?i)applying with\\s+([A-Z][A-Za-z0-9\\s]{2,29})"),
            Pattern.compile("(?i)Company\\s*:\\s*([^.,!<>]+)"),
            Pattern.compile("\\bat\\s+([A-Z][A-Za-z0-9\\s]{2,29})[.,!]"), 
            Pattern.compile("(?i)Team\\s+([A-Z][A-Za-z0-9\\s]{2,29})"),
            Pattern.compile("(?i)interest in(?: the)?\\s+([A-Z][A-Za-z0-9\\s]{2,29})[.,!]")
        };

        for (Pattern p : patterns) {
            Matcher m = p.matcher(text);
            if (m.find()) {
                System.out.println("Matched Pattern: " + p.pattern() + " => " + m.group(1));
            }
        }
    }
}
