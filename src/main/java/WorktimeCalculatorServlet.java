import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/calc-worktime")
public class WorktimeCalculatorServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    public WorktimeCalculatorServlet() {
        super();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.getRequestDispatcher("/index.html").forward(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	String s = request.getParameter("entradas"); 
    	ObjectMapper objectMapper = new ObjectMapper();
    	JsonNode jsonNode = objectMapper.readTree(s);
    	String property = jsonNode.get("property").asText();
    	String direction = jsonNode.get("direction").asText();
    }
}