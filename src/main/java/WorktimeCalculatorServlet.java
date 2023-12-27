import java.io.BufferedReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
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
        StringBuilder builder = new StringBuilder();
        BufferedReader reader = request.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }
        String data = builder.toString();

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(data);

        JsonNode workScheduleStartArray = jsonNode.get("workScheduleStartArray");
        JsonNode workScheduleEndArray = jsonNode.get("workScheduleEndArray");
        JsonNode workDoneStartArray = jsonNode.get("workDoneStartArray");
        JsonNode workDoneEndArray = jsonNode.get("workDoneEndArray");

        TypeReference<List<Map<String, Object>>> typeRef = new TypeReference<List<Map<String, Object>>>() {};
        List<Map<String, Object>> workScheduleStartList = objectMapper.readValue(workScheduleStartArray.toString(), typeRef);
        List<Map<String, Object>> workScheduleEndList = objectMapper.readValue(workScheduleEndArray.toString(), typeRef);
        List<Map<String, Object>> workDoneStartList = objectMapper.readValue(workDoneStartArray.toString(), typeRef);
        List<Map<String, Object>> workDoneEndList = objectMapper.readValue(workDoneEndArray.toString(), typeRef);

        LocalDate currentDay = LocalDate.of(2023, 3, 10);
        LocalDateTime lastEndTime = null;

        for (Map<String, Object> startItem : workScheduleStartList) {
            int startHours = Integer.parseInt((String) startItem.get("hours"));
            int startMinutes = Integer.parseInt((String) startItem.get("minutes"));
            LocalDateTime startDateTime = LocalDateTime.of(currentDay, LocalTime.of(startHours, startMinutes));

            if (lastEndTime != null && startDateTime.isBefore(lastEndTime)) {
                startDateTime = startDateTime.plusDays(1);
                currentDay = startDateTime.toLocalDate();
            }

            startItem.put("dateTime", startDateTime);

            for (Map<String, Object> endItem : workScheduleEndList) {
                if (startItem.get("id").equals(endItem.get("id"))) {
                    int endHours = Integer.parseInt((String) endItem.get("hours"));
                    int endMinutes = Integer.parseInt((String) endItem.get("minutes"));
                    LocalDateTime endDateTime = LocalDateTime.of(currentDay, LocalTime.of(endHours, endMinutes));

                    if (endDateTime.isBefore(startDateTime)) {
                        endDateTime = endDateTime.plusDays(1);
                    }

                    endItem.put("dateTime", endDateTime);
                    lastEndTime = endDateTime;
                }
            }
        }

        currentDay = LocalDate.of(2023, 3, 10);
        lastEndTime = null;

        for (Map<String, Object> startItem : workDoneStartList) {
            int startHours = Integer.parseInt((String) startItem.get("hours"));
            int startMinutes = Integer.parseInt((String) startItem.get("minutes"));
            LocalDateTime startDateTime = LocalDateTime.of(currentDay, LocalTime.of(startHours, startMinutes));

            if (lastEndTime != null && startDateTime.isBefore(lastEndTime)) {
                startDateTime = startDateTime.plusDays(1);
                currentDay = startDateTime.toLocalDate();
            }

            startItem.put("dateTime", startDateTime);

            for (Map<String, Object> endItem : workDoneEndList) {
                if (startItem.get("id").equals(endItem.get("id"))) {
                    int endHours = Integer.parseInt((String) endItem.get("hours"));
                    int endMinutes = Integer.parseInt((String) endItem.get("minutes"));
                    LocalDateTime endDateTime = LocalDateTime.of(currentDay, LocalTime.of(endHours, endMinutes));

                    if (endDateTime.isBefore(startDateTime)) {
                        endDateTime = endDateTime.plusDays(1);
                    }

                    endItem.put("dateTime", endDateTime);
                    lastEndTime = endDateTime;
                }
            }
        }
        
        System.out.println("teste");

    }
}