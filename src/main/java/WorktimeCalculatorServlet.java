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
            while (j < workDoneStartList.size()) {
                Map<String, Object> startSchedule = workScheduleStartList.get(i);
                Map<String, Object> endSchedule = workScheduleEndList.get(i);

                LocalDateTime scheduleStart = (LocalDateTime) startSchedule.get("dateTime");
                LocalDateTime scheduleEnd = (LocalDateTime) endSchedule.get("dateTime");

                if(scheduleStart.getDayOfMonth() != scheduleEnd.getDayOfMonth()) {
                    isOvernightShift = true;
                }

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

                    if(i == 0 && j == 0 && doneStart.isBefore(scheduleStart) && doneEnd.isBefore(scheduleEnd)) {
                        addExtraTime(resultList, doneStart, doneEnd);
                    }

                    if (doneStart.isBefore(scheduleStart) && (doneEnd.isBefore(scheduleStart) || doneEnd.equals(scheduleStart))) {
                        addExtraTime(resultList, doneStart, doneEnd);
                    }

                    if (doneStart.isBefore(scheduleStart) && doneEnd.isAfter(scheduleStart)) {
                        addExtraTime(resultList, doneStart, scheduleStart);
                    }

                    if(!(i < workScheduleStartList.size() - 1) && doneEnd.isAfter(scheduleEnd)) {
                        if(doneStart.isBefore(scheduleEnd)) {
                            addExtraTime(resultList, scheduleEnd, doneEnd);
                        } else {
                            addExtraTime(resultList, doneStart, doneEnd);
                        }
                    }

                    if (i < workScheduleStartList.size() - 1) {
                        Map<String, Object> nextStartSchedule = workScheduleStartList.get(i + 1);
                        LocalDateTime nextScheduleStart = (LocalDateTime) nextStartSchedule.get("dateTime");

                        Map<String, Object> nextEndSchedule = workScheduleEndList.get(i + 1);
                        LocalDateTime nextScheduleEnd = (LocalDateTime) nextEndSchedule.get("dateTime");

                        if (nextScheduleStart.isAfter(doneStart) && nextScheduleStart.isBefore(doneEnd)) {
                            addExtraTime(resultList, scheduleEnd, nextScheduleStart);
                        }

                        if ((j + 1 < workDoneStartList.size())) {
                            Map<String, Object> nextStartDone = workDoneStartList.get(j + 1);
                            LocalDateTime nextDoneStart = (LocalDateTime) nextStartDone.get("dateTime");

                            Map<String, Object> nextEndDone = workDoneEndList.get(j + 1);
                            LocalDateTime nextDoneEnd = (LocalDateTime) nextEndDone.get("dateTime");

                            if(nextScheduleEnd.isBefore(nextDoneStart) && workScheduleStartList.size() == workDoneStartList.size()) {
                                addExtraTime(resultList, nextDoneStart, nextDoneEnd);
                            }

                            if(nextDoneEnd.isAfter(nextScheduleEnd)) {
                                addExtraTime(resultList, nextScheduleEnd, nextDoneEnd);
                            }

                            if ((i + 2 < workScheduleStartList.size())) {
                                Map<String, Object> nextNextStartSchedule = workScheduleStartList.get(i + 2);
                                LocalDateTime nextNextScheduleStart = (LocalDateTime) nextNextStartSchedule.get("dateTime");
                                if((nextScheduleEnd.equals(nextDoneStart) || nextDoneStart.isAfter(nextScheduleEnd)) && (nextNextScheduleStart.equals(nextDoneEnd) || nextDoneEnd.isBefore(nextNextScheduleStart))) {
                                    addExtraTime(resultList, nextDoneStart, nextDoneEnd);
                                }
                            }

                        }

                        if(nextScheduleEnd.isBefore(doneEnd)) {
                            if ((i + 2 < workScheduleStartList.size())) {
                                Map<String, Object> nextNextStartSchedule = workScheduleStartList.get(i + 2);
                                LocalDateTime nextNextScheduleStart = (LocalDateTime) nextNextStartSchedule.get("dateTime");
                                addExtraTime(resultList, nextScheduleEnd, nextNextScheduleStart);
                            }
                        }
                    }

                    j++;
                    if (i < workScheduleStartList.size() - 1 && doneStart.isAfter(scheduleStart)) {
                        i++;
                    }
                    continue;
                }
                
                if(i == 0 && j == 0 && doneStart.isBefore(scheduleStart) && doneEnd.isBefore(scheduleEnd)) {
                	addExtraTime(resultList, doneStart, doneEnd);
                }

                if (doneStart.isBefore(scheduleStart) && (doneEnd.isBefore(scheduleStart) || doneEnd.equals(scheduleStart))) {
                    addExtraTime(resultList, doneStart, doneEnd);
                }
                
                if (doneStart.isBefore(scheduleStart) && doneEnd.isAfter(scheduleStart)) {
                    addExtraTime(resultList, doneStart, scheduleStart);
                }
                
                if(!(i < workScheduleStartList.size() - 1) && doneEnd.isAfter(scheduleEnd)) {
                	if(doneStart.isBefore(scheduleEnd)) {
                		addExtraTime(resultList, scheduleEnd, doneEnd);
                	} else {
                		addExtraTime(resultList, doneStart, doneEnd);
                	}
                }

                if (i < workScheduleStartList.size() - 1) {
                    Map<String, Object> nextStartSchedule = workScheduleStartList.get(i + 1);
                    LocalDateTime nextScheduleStart = (LocalDateTime) nextStartSchedule.get("dateTime");

                    Map<String, Object> nextEndSchedule = workScheduleEndList.get(i + 1);
                    LocalDateTime nextScheduleEnd = (LocalDateTime) nextEndSchedule.get("dateTime");

                    if (nextScheduleStart.isAfter(doneStart) && nextScheduleStart.isBefore(doneEnd)) {
                        addExtraTime(resultList, scheduleEnd, nextScheduleStart);
                    }

                    if ((j + 1 < workDoneStartList.size())) {
                        Map<String, Object> nextStartDone = workDoneStartList.get(j + 1);
                        LocalDateTime nextDoneStart = (LocalDateTime) nextStartDone.get("dateTime");

                        Map<String, Object> nextEndDone = workDoneEndList.get(j + 1);
                        LocalDateTime nextDoneEnd = (LocalDateTime) nextEndDone.get("dateTime");

                        if(nextScheduleEnd.isBefore(nextDoneStart) && workScheduleStartList.size() == workDoneStartList.size()) {
                            addExtraTime(resultList, nextDoneStart, nextDoneEnd);
                        }
                        
                        if(nextDoneEnd.isAfter(nextScheduleEnd)) {
                        	addExtraTime(resultList, nextScheduleEnd, nextDoneEnd);
                        }

                        if ((i + 2 < workScheduleStartList.size())) {
                            Map<String, Object> nextNextStartSchedule = workScheduleStartList.get(i + 2);
                            LocalDateTime nextNextScheduleStart = (LocalDateTime) nextNextStartSchedule.get("dateTime");
                            if((nextScheduleEnd.equals(nextDoneStart) || nextDoneStart.isAfter(nextScheduleEnd)) && (nextNextScheduleStart.equals(nextDoneEnd) || nextDoneEnd.isBefore(nextNextScheduleStart))) {
                                addExtraTime(resultList, nextDoneStart, nextDoneEnd);
                            }
                        }

                    }

                    if(nextScheduleEnd.isBefore(doneEnd)) {
                        if ((i + 2 < workScheduleStartList.size())) {
                            Map<String, Object> nextNextStartSchedule = workScheduleStartList.get(i + 2);
                            LocalDateTime nextNextScheduleStart = (LocalDateTime) nextNextStartSchedule.get("dateTime");
                            addExtraTime(resultList, nextScheduleEnd, nextNextScheduleStart);
                        }
                    }
                }

                j++;
                if (i < workScheduleStartList.size() - 1 && doneStart.isAfter(scheduleStart)) {
                    i++;
                }
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

                if(scheduleStart.getDayOfMonth() != scheduleEnd.getDayOfMonth()) {
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
                        if(j == 0) {
                            addDelay(resultList, scheduleStart, doneStart);
                        }
                    }

                    if(doneEnd.isBefore(scheduleEnd) && doneEnd.isAfter(scheduleStart) && !(j + 1 < workDoneStartList.size())) {
                        addDelay(resultList, doneEnd, scheduleEnd);
                    }

                    if(doneEnd.isBefore(scheduleEnd) && doneEnd.isAfter(scheduleStart) && workDoneStartList.size() == workScheduleStartList.size()) {
                        addDelay(resultList, doneEnd, scheduleEnd);
                    }

                    if (i < workScheduleStartList.size() - 1) {
                        Map<String, Object> nextStartSchedule = workScheduleStartList.get(i + 1);
                        LocalDateTime nextScheduleStart = (LocalDateTime) nextStartSchedule.get("dateTime");

                        Map<String, Object> nextEndSchedule = workScheduleEndList.get(i + 1);
                        LocalDateTime nextScheduleEnd = (LocalDateTime) nextEndSchedule.get("dateTime");

                        if(doneEnd.isBefore(nextScheduleStart) && !(j + 1 < workDoneStartList.size())) {
                            addDelay(resultList, nextScheduleStart, nextScheduleEnd);
                        }
                    }

                    if ((j + 1 < workDoneStartList.size())) {
                        Map<String, Object> nextStartDone = workDoneStartList.get(j + 1);
                        LocalDateTime nextDoneStart = (LocalDateTime) nextStartDone.get("dateTime");

                        Map<String, Object> nextEndDone = workDoneEndList.get(j + 1);
                        LocalDateTime nextDoneEnd = (LocalDateTime) nextEndDone.get("dateTime");

                        if(doneEnd.isBefore(scheduleEnd) && nextDoneStart.isBefore(scheduleEnd) && doneEnd.isAfter(scheduleStart)) {
                            addDelay(resultList, doneEnd, nextDoneStart);
                        }

                        if(doneEnd.isBefore(scheduleStart) && nextDoneStart.isBefore(scheduleStart) && !(j + 1 < workDoneStartList.size())) {
                            addDelay(resultList, scheduleStart, scheduleEnd);
                        }

                        if(doneEnd.isBefore(scheduleStart) && nextDoneStart.isAfter(scheduleStart) && workScheduleStartList.size() == 1) {
                            addDelay(resultList, scheduleStart, nextDoneStart);
                        }

                        if(doneEnd.isBefore(scheduleStart) && nextDoneStart.isAfter(scheduleEnd)) {
                            addDelay(resultList, scheduleStart, scheduleEnd);
                        }

                        if(doneEnd.isBefore(scheduleEnd) && (doneEnd.isAfter(scheduleStart) || doneEnd.equals(scheduleStart)) && nextDoneStart.equals(scheduleEnd)) {
                            addDelay(resultList, doneEnd, scheduleEnd);
                        }

                        if (i < workScheduleStartList.size() - 1) {
                            Map<String, Object> nextStartSchedule = workScheduleStartList.get(i + 1);
                            LocalDateTime nextScheduleStart = (LocalDateTime) nextStartSchedule.get("dateTime");

                            Map<String, Object> nextEndSchedule = workScheduleEndList.get(i + 1);
                            LocalDateTime nextScheduleEnd = (LocalDateTime) nextEndSchedule.get("dateTime");

                            if((doneEnd.equals(nextScheduleStart) || doneEnd.isAfter(nextScheduleStart)) && (nextDoneStart.equals(nextScheduleEnd)) || nextDoneStart.isAfter(nextScheduleEnd)) {
                                if(nextDoneStart.isAfter(nextScheduleEnd)) {
                                    addDelay(resultList, nextScheduleStart, nextScheduleEnd);
                                } else {
                                    addDelay(resultList, nextScheduleStart, nextDoneStart);
                                }
                            }

                            if(nextScheduleStart.isBefore(nextDoneStart) && nextScheduleStart.isAfter(doneEnd)) {
                                addDelay(resultList, nextScheduleStart, nextDoneStart);
                            }

                            if(doneEnd.isBefore(nextScheduleEnd) && nextDoneStart.isBefore(nextScheduleEnd) && doneEnd.isAfter(nextScheduleStart)) {
                                addDelay(resultList, doneEnd, nextDoneStart);
                            }

                            if ((i + 2 < workScheduleStartList.size())) {
                                Map<String, Object> nextNextStartSchedule = workScheduleStartList.get(i + 2);
                                LocalDateTime nextNextScheduleStart = (LocalDateTime) nextNextStartSchedule.get("dateTime");

                                Map<String, Object> nextNextEndSchedule = workScheduleEndList.get(i + 2);
                                LocalDateTime nextNextScheduleEnd = (LocalDateTime) nextNextEndSchedule.get("dateTime");

                                if(nextNextScheduleStart.equals(nextDoneEnd)) {
                                    addDelay(resultList, nextDoneEnd, nextNextScheduleEnd);
                                }

                                if(nextDoneStart.isAfter(nextNextScheduleStart)) {
                                    addDelay(resultList, nextNextScheduleStart, nextDoneStart);
                                }
                            }
                        }

                        if(nextDoneEnd.isBefore(scheduleEnd)) {
                            if ((j + 2 < workDoneStartList.size())) {
                                Map<String, Object> nextNextStartDone = workDoneStartList.get(j + 2);
                                LocalDateTime nextNextDoneStart = (LocalDateTime) nextNextStartDone.get("dateTime");
                                if(nextNextDoneStart.isBefore(scheduleEnd) && nextNextDoneStart.isAfter(scheduleStart)) {
                                    addDelay(resultList, nextDoneEnd, nextNextDoneStart);
                                }
                            } else {
                                addDelay(resultList, nextDoneEnd, nextDoneStart);
                            }
                        }
                    }

                    j++;
                    if (i < workScheduleStartList.size() - 1 && (doneEnd.isAfter(scheduleEnd) || doneEnd.equals(scheduleEnd))) {
                        i++;
                    }
                    continue;
                }

                if (doneStart.isAfter(scheduleStart)) {
                	if(j == 0) {
                		addDelay(resultList, scheduleStart, doneStart);	
                	}
                }

                if(doneEnd.isBefore(scheduleEnd) && doneEnd.isAfter(scheduleStart) && !(j + 1 < workDoneStartList.size())) {
                    addDelay(resultList, doneEnd, scheduleEnd);
                }
                
                if(doneEnd.isBefore(scheduleEnd) && doneEnd.isAfter(scheduleStart) && workDoneStartList.size() == workScheduleStartList.size()) {
                	addDelay(resultList, doneEnd, scheduleEnd);
                }
                
                if (i < workScheduleStartList.size() - 1) {
                    Map<String, Object> nextStartSchedule = workScheduleStartList.get(i + 1);
                    LocalDateTime nextScheduleStart = (LocalDateTime) nextStartSchedule.get("dateTime");
                    
                    Map<String, Object> nextEndSchedule = workScheduleEndList.get(i + 1);
                    LocalDateTime nextScheduleEnd = (LocalDateTime) nextEndSchedule.get("dateTime");
                    
                    if(doneEnd.isBefore(nextScheduleStart) && !(j + 1 < workDoneStartList.size())) {
                    	addDelay(resultList, nextScheduleStart, nextScheduleEnd);
                    }
                }

                if ((j + 1 < workDoneStartList.size())) {
                    Map<String, Object> nextStartDone = workDoneStartList.get(j + 1);
                    LocalDateTime nextDoneStart = (LocalDateTime) nextStartDone.get("dateTime");

                    Map<String, Object> nextEndDone = workDoneEndList.get(j + 1);
                    LocalDateTime nextDoneEnd = (LocalDateTime) nextEndDone.get("dateTime");
                    
                    if(doneEnd.isBefore(scheduleEnd) && nextDoneStart.isBefore(scheduleEnd) && doneEnd.isAfter(scheduleStart)) {
                    	addDelay(resultList, doneEnd, nextDoneStart);
                    }
                    
                    if(doneEnd.isBefore(scheduleStart) && nextDoneStart.isBefore(scheduleStart) && !(j + 1 < workDoneStartList.size())) {
                    	addDelay(resultList, scheduleStart, scheduleEnd);
                    }
                    
                    if(doneEnd.isBefore(scheduleStart) && nextDoneStart.isAfter(scheduleStart) && workScheduleStartList.size() == 1) {
                    	addDelay(resultList, scheduleStart, nextDoneStart);
                    }
                    
                    if(doneEnd.isBefore(scheduleStart) && nextDoneStart.isAfter(scheduleEnd)) {
                    	addDelay(resultList, scheduleStart, scheduleEnd);
                    }
                    
                    if(doneEnd.isBefore(scheduleEnd) && (doneEnd.isAfter(scheduleStart) || doneEnd.equals(scheduleStart)) && nextDoneStart.equals(scheduleEnd)) {
                    	addDelay(resultList, doneEnd, scheduleEnd);
                    }

                    if (i < workScheduleStartList.size() - 1) {
                        Map<String, Object> nextStartSchedule = workScheduleStartList.get(i + 1);
                        LocalDateTime nextScheduleStart = (LocalDateTime) nextStartSchedule.get("dateTime");

                        Map<String, Object> nextEndSchedule = workScheduleEndList.get(i + 1);
                        LocalDateTime nextScheduleEnd = (LocalDateTime) nextEndSchedule.get("dateTime");

                        if((doneEnd.equals(nextScheduleStart) || doneEnd.isAfter(nextScheduleStart)) && (nextDoneStart.equals(nextScheduleEnd)) || nextDoneStart.isAfter(nextScheduleEnd)) {
                            if(nextDoneStart.isAfter(nextScheduleEnd)) {
                                addDelay(resultList, nextScheduleStart, nextScheduleEnd);
                            } else {
                                addDelay(resultList, nextScheduleStart, nextDoneStart);
                            }
                        }
                        
                        if(nextScheduleStart.isBefore(nextDoneStart) && nextScheduleStart.isAfter(doneEnd)) {
                        	addDelay(resultList, nextScheduleStart, nextDoneStart);
                        }
                        
                        if(doneEnd.isBefore(nextScheduleEnd) && nextDoneStart.isBefore(nextScheduleEnd) && doneEnd.isAfter(nextScheduleStart)) {
                        	addDelay(resultList, doneEnd, nextDoneStart);
                        }

                        if ((i + 2 < workScheduleStartList.size())) {
                            Map<String, Object> nextNextStartSchedule = workScheduleStartList.get(i + 2);
                            LocalDateTime nextNextScheduleStart = (LocalDateTime) nextNextStartSchedule.get("dateTime");

                            Map<String, Object> nextNextEndSchedule = workScheduleEndList.get(i + 2);
                            LocalDateTime nextNextScheduleEnd = (LocalDateTime) nextNextEndSchedule.get("dateTime");

                            if(nextNextScheduleStart.equals(nextDoneEnd)) {
                                addDelay(resultList, nextDoneEnd, nextNextScheduleEnd);
                            }
                            
                            if(nextDoneStart.isAfter(nextNextScheduleStart)) {
                            	addDelay(resultList, nextNextScheduleStart, nextDoneStart);
                            }
                        }
                    }

                    if(nextDoneEnd.isBefore(scheduleEnd)) {
                        if ((j + 2 < workDoneStartList.size())) {
                            Map<String, Object> nextNextStartDone = workDoneStartList.get(j + 2);
                            LocalDateTime nextNextDoneStart = (LocalDateTime) nextNextStartDone.get("dateTime");
                            if(nextNextDoneStart.isBefore(scheduleEnd) && nextNextDoneStart.isAfter(scheduleStart)) {
                            	addDelay(resultList, nextDoneEnd, nextNextDoneStart);
                            }
                        } else {
                            addDelay(resultList, nextDoneEnd, nextDoneStart);
                        }
                    }
                }

                j++;
                if (i < workScheduleStartList.size() - 1 && (doneEnd.isAfter(scheduleEnd) || doneEnd.equals(scheduleEnd))) {
                    i++;
                }
            }
        }

        if(workDoneStartList.size() == 1) {
            for (int i = 0; i < workScheduleStartList.size(); i++) {
                Map<String, Object> startSchedule = workScheduleStartList.get(i);
                Map<String, Object> endSchedule = workScheduleEndList.get(i);

                LocalDateTime scheduleStart = (LocalDateTime) startSchedule.get("dateTime");
                LocalDateTime scheduleEnd = (LocalDateTime) endSchedule.get("dateTime");

                Map<String, Object> startDone = workDoneStartList.get(0);
                Map<String, Object> endDone = workDoneEndList.get(0);

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
                }
                
                if(doneStart.isAfter(scheduleStart)) {
                	if(scheduleEnd.isBefore(doneStart)) {
                		addDelay(resultList, scheduleStart, scheduleEnd);
                	} else {
                		addDelay(resultList, scheduleStart, doneStart);
                	}
                }

                if(doneEnd.isBefore(scheduleStart)) {
                    addDelay(resultList, scheduleStart, scheduleEnd);
                }

                if(doneEnd.isAfter(scheduleStart) && doneEnd.isBefore(scheduleEnd)) {
                    addDelay(resultList, doneEnd, scheduleEnd);
                }
            }
        }

        if (!workScheduleEndList.isEmpty() && !workDoneEndList.isEmpty()) {
            Map<String, Object> lastWorkScheduleEnd = workScheduleEndList.get(workScheduleEndList.size() - 1);
            Map<String, Object> lastWorkDoneEnd = workDoneEndList.get(workDoneEndList.size() - 1);

            LocalDateTime lastScheduleEndTime = (LocalDateTime) lastWorkScheduleEnd.get("dateTime");
            LocalDateTime lastDoneEndTime = (LocalDateTime) lastWorkDoneEnd.get("dateTime");

            if (lastDoneEndTime.isAfter(lastScheduleEndTime) && workDoneEndList.size() == 1) {
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