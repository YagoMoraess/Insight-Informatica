import java.io.BufferedReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

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

        List<Map<String, Object>> resultList = new ArrayList<>();

        int j = 0;
        boolean isOvernightShift = false;
        for (int i = 0; i < workScheduleStartList.size(); i++) {
            Map<String, Object> startSchedule = workScheduleStartList.get(i);
            Map<String, Object> endSchedule = workScheduleEndList.get(i);

            LocalDateTime scheduleStart = (LocalDateTime) startSchedule.get("dateTime");
            LocalDateTime scheduleEnd = (LocalDateTime) endSchedule.get("dateTime");

            if(scheduleStart.isBefore(scheduleEnd)) {
                isOvernightShift = true;
            }

            while (j < workDoneStartList.size()) {
                Map<String, Object> startDone = workDoneStartList.get(j);
                Map<String, Object> endDone = workDoneEndList.get(j);

                LocalDateTime doneStart = (LocalDateTime) startDone.get("dateTime");
                LocalDateTime doneEnd = (LocalDateTime) endDone.get("dateTime");

                if(isOvernightShift) {
                    boolean isSameDayStart = doneStart.minusHours(12).isBefore(scheduleStart) && doneStart.plusHours(12).isAfter(scheduleStart);
                    boolean isSameDayEnd = doneEnd.minusHours(12).isBefore(scheduleEnd) && doneEnd.plusHours(12).isAfter(scheduleEnd);
                    if(!isSameDayStart) {
                        doneStart = doneStart.plusDays(1);
                    }
                    if(!isSameDayEnd) {
                        doneEnd = doneEnd.plusDays(1);
                    }

                    if (doneStart.isBefore(scheduleStart)) {
                        addExtraTime(resultList, doneStart, scheduleStart);
                        doneStart = scheduleStart;
                    }

                    if (i < workScheduleStartList.size() - 1) {
                        Map<String, Object> nextStartSchedule = workScheduleStartList.get(i + 1);
                        LocalDateTime nextScheduleStart = (LocalDateTime) nextStartSchedule.get("dateTime");

                        if (scheduleEnd.isBefore(nextScheduleStart)) {
                            addExtraTime(resultList, scheduleEnd, nextScheduleStart);
                        }
                    } else {
                        if (doneEnd.isAfter(scheduleEnd)) {
                            addExtraTime(resultList, scheduleEnd, doneEnd);
                        }
                    }
                    j++;
                }

                if (doneStart.isBefore(scheduleStart)) {
                    addExtraTime(resultList, doneStart, scheduleStart);
                    doneStart = scheduleStart;
                }

                if (i < workScheduleStartList.size() - 1) {
                    Map<String, Object> nextStartSchedule = workScheduleStartList.get(i + 1);
                    LocalDateTime nextScheduleStart = (LocalDateTime) nextStartSchedule.get("dateTime");

                    if (scheduleEnd.isBefore(nextScheduleStart)) {
                        addExtraTime(resultList, scheduleEnd, nextScheduleStart);
                    }
                } else {
                    if (doneEnd.isAfter(scheduleEnd)) {
                        addExtraTime(resultList, scheduleEnd, doneEnd);
                    }
                }
                j++;
            }
        }

        j = 0;
        isOvernightShift = false;
        for (int i = 0; i < workScheduleStartList.size(); i++) {
            while (j < workDoneStartList.size()) {
                Map<String, Object> startSchedule = workScheduleStartList.get(i);
                Map<String, Object> endSchedule = workScheduleEndList.get(i);

                LocalDateTime scheduleStart = (LocalDateTime) startSchedule.get("dateTime");
                LocalDateTime scheduleEnd = (LocalDateTime) endSchedule.get("dateTime");
                Map<String, Object> startDone = workDoneStartList.get(j);
                Map<String, Object> endDone = workDoneEndList.get(j);

                LocalDateTime doneStart = (LocalDateTime) startDone.get("dateTime");
                LocalDateTime doneEnd = (LocalDateTime) endDone.get("dateTime");

                if(scheduleStart.isBefore(scheduleEnd)) {
                    isOvernightShift = true;
                }

                if(isOvernightShift) {
                    boolean isSameDayStart = doneStart.minusHours(12).isBefore(scheduleStart) && doneStart.plusHours(12).isAfter(scheduleStart);
                    boolean isSameDayEnd = doneEnd.minusHours(12).isBefore(scheduleEnd) && doneEnd.plusHours(12).isAfter(scheduleEnd);
                    if(!isSameDayStart) {
                        doneStart = doneStart.plusDays(1);
                    }
                    if(!isSameDayEnd) {
                        doneEnd = doneEnd.plusDays(1);
                    }

                    if (doneStart.isAfter(scheduleStart)) {
                        addDelay(resultList, scheduleStart, doneStart);
                        doneStart = scheduleStart;
                    }

                    if(doneEnd.isBefore(scheduleEnd)) {
                        addDelay(resultList, doneEnd, scheduleEnd);
                        doneEnd = scheduleEnd;
                    }

                    if (i < workScheduleStartList.size() - 1) {
                        Map<String, Object> nextStartSchedule = workScheduleStartList.get(i + 1);
                        Map<String, Object> nextEndSchedule = workScheduleEndList.get(i + 1);

                        LocalDateTime nextScheduleStart = (LocalDateTime) nextStartSchedule.get("dateTime");
                        LocalDateTime nextScheduleEnd = (LocalDateTime) nextEndSchedule.get("dateTime");

                        if(j == 0) {
                            j++;
                            i++;
                            continue;
                        }

                        if(doneStart.isAfter(nextScheduleStart)) {
                            addDelay(resultList, nextScheduleStart, doneStart);
                            doneStart = nextScheduleStart;
                        }

                        if(doneEnd.isBefore(nextScheduleEnd)) {
                            addDelay(resultList, doneEnd, nextScheduleEnd);
                            doneEnd = nextScheduleEnd;
                        }
                    }
                    j++;
                }

                if (doneStart.isAfter(scheduleStart)) {
                    addDelay(resultList, scheduleStart, doneStart);
                    doneStart = scheduleStart;
                }

                if(doneEnd.isBefore(scheduleEnd)) {
                    addDelay(resultList, doneEnd, scheduleEnd);
                    doneEnd = scheduleEnd;
                }

                if (i < workScheduleStartList.size() - 1) {
                    Map<String, Object> nextStartSchedule = workScheduleStartList.get(i + 1);
                    Map<String, Object> nextEndSchedule = workScheduleEndList.get(i + 1);

                    LocalDateTime nextScheduleStart = (LocalDateTime) nextStartSchedule.get("dateTime");
                    LocalDateTime nextScheduleEnd = (LocalDateTime) nextEndSchedule.get("dateTime");

                    if(j == 0) {
                        j++;
                        i++;
                        continue;
                    }

                    if(doneStart.isAfter(nextScheduleStart)) {
                        addDelay(resultList, nextScheduleStart, doneStart);
                        doneStart = nextScheduleStart;
                    }

                    if(doneEnd.isBefore(nextScheduleEnd)) {
                        addDelay(resultList, doneEnd, nextScheduleEnd);
                        doneEnd = nextScheduleEnd;
                    }
                }
                j++;
            }
        }

        if (!workScheduleEndList.isEmpty() && !workDoneEndList.isEmpty()) {
            Map<String, Object> lastWorkScheduleEnd = workScheduleEndList.get(workScheduleEndList.size() - 1);
            Map<String, Object> lastWorkDoneEnd = workDoneEndList.get(workDoneEndList.size() - 1);

            LocalDateTime lastScheduleEndTime = (LocalDateTime) lastWorkScheduleEnd.get("dateTime");
            LocalDateTime lastDoneEndTime = (LocalDateTime) lastWorkDoneEnd.get("dateTime");

            if (lastDoneEndTime.isAfter(lastScheduleEndTime)) {
                addExtraTime(resultList, lastScheduleEndTime, lastDoneEndTime);
            }
        }

        Set<Map<String, Object>> resultListWithoutDuplicates = new LinkedHashSet<>(resultList);
        String jsonResult = objectMapper.writeValueAsString(resultListWithoutDuplicates);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(jsonResult);
    }

    private void addExtraTime(List<Map<String, Object>> resultList, LocalDateTime start, LocalDateTime end) {
        Map<String, Object> extraTime = new HashMap<>();
        extraTime.put("type", "extra-time");
        extraTime.put("result", start.toString() + " - " + end.toString());
        resultList.add(extraTime);
    }

    private void addDelay(List<Map<String, Object>> resultList, LocalDateTime start, LocalDateTime end) {
        Map<String, Object> delay = new HashMap<>();
        delay.put("type", "delay");
        delay.put("result", start.toString() + " - " + end.toString());
        resultList.add(delay);
    }
}