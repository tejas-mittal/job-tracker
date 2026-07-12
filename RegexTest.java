import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class RegexTest {
    public static void main(String[] args) {
        String text = "Congratulations, you star!\n" +
                      "By applying for Myntra Ramp Up - Data Science Internship Hiring (Batch 2027) at Myntra, you’ve taken the first step towards an exciting career opportunity.\n" +
                      "Wishing you all the best for the next steps in the hiring process. We’re rooting for you to ace this opportunity and achieve your career goals!\n" +
                      "Regards,\n" +
                      "Team Myntra";

        Pattern p = Pattern.compile("(?i)(thank you for applying|application received|application for|applied for|resume received|application has been received|thank you for your interest|congratulations.*applying)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher m = p.matcher(text);
        if (m.find()) {
            System.out.println("Match found: " + m.group());
        } else {
            System.out.println("NO MATCH FOUND");
        }
        
        String subject = "Campus Interview - 2024";
        String body = "Please join the meeting.";
        String text2 = subject + " " + body;
        Pattern p2 = Pattern.compile("(?i)(schedule an interview|invite you to interview|next steps in the interview|technical interview|phone screen|availability|schedule a call|moving forward with your application|moving to the next round|campus interview|interview invitation|interview -|discussion with)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher m2 = p2.matcher(text2);
        if (m2.find()) {
            System.out.println("Match 2 found: " + m2.group());
        } else {
            System.out.println("NO MATCH 2 FOUND");
        }
    }
}
