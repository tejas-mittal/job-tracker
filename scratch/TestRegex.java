public class TestRegex {
    public static void main(String[] args) {
        String text = "formerly Training and Placement DepartmentYour application for Internship interview for the Company : Legalzoom on 30-03-2026 09:00 has been added.";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("(?i)Company\\s*:\\s*([A-Za-z0-9\\s&.-]+?)(?=\\s+(?:on|for|at|has)\\b|[.,!<>\\n]|$)");
        java.util.regex.Matcher m = p.matcher(text);
        if (m.find()) {
            System.out.println("Matched: '" + m.group(1) + "'");
        } else {
            System.out.println("No match");
        }
    }
}
