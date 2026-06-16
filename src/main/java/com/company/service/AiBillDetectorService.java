@Service
public class AiBillDetectorService {

    private final String API_KEY = "YOUR_OPENAI_KEY";

    public boolean isAiGenerated(String text) {

        try {
            String prompt = """
            You are an expert forensic invoice analyzer.

            Task:
            Check if this bill is AI generated or real scanned/photographed invoice.

            Return ONLY:
            YES = AI generated
            NO = Real document

            Bill Text:
            """ + text;

            // call OpenAI API (pseudo code)
            String response = callOpenAI(prompt);

            return response.contains("YES");

        } catch (Exception e) {
            return false;
        }
    }

    private String callOpenAI(String prompt) {
        // You will implement REST call here using RestTemplate or WebClient
        return "NO";
    }
}