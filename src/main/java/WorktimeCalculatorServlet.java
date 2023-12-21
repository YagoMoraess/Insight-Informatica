import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String entrada = request.getParameter("entrada");
        String saida = request.getParameter("saida");

        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
        LocalDateTime entradaLocalDateTime = LocalDateTime.parse(entrada, formatter);
        LocalDateTime saidaLocalDateTime = LocalDateTime.parse(saida, formatter);

        // Resto do código para processar os dados

        response.setContentType("text/plain");
        response.getWriter().write("Dados do calendário recebidos com sucesso!");
    }
}