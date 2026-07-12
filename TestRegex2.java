public class TestRegex2 {
    public static void main(String[] args) {
        String text = "Assessment Details Position SDE Intern Type Coding Exam (Timed) Duration 90 minutes Deadline 8th April 2026, 10:00 PM IST";
        java.util.regex.Matcher assessMatcher = java.util.regex.Pattern.compile("(?i)(?:assessment|test) on\\s+(\\d{2}-\\d{2}-\\d{4})|Deadline\\s*[:\\-]?\\s*(\\d{1,2}(?:st|nd|rd|th)?\\s+[A-Za-z]+\\s+\\d{4})|Due(?: Date)?\\s*[:\\-]?\\s*([^<>\\n]{5,20})")
                        .matcher(text);
        if (assessMatcher.find()) {
            System.out.println("Matched! 1:" + assessMatcher.group(1) + " 2:" + assessMatcher.group(2) + " 3:" + assessMatcher.group(3));
        } else {
            System.out.println("No match!");
        }
    }
}
